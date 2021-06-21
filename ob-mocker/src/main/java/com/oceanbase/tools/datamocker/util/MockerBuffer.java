package com.oceanbase.tools.datamocker.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.oceanbase.tools.datamocker.core.task.AbstractDataPipe;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.Validate;

/**
 * Buffer of mock data, used to buffer temporary data generated
 *
 * @author yh263208
 * @date 2021-01-14 20:48
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class MockerBuffer {
    private Boolean isClose = Boolean.FALSE;
    /**
     * Thread synchronizer
     */
    private CyclicBarrier synchronizer;
    /**
     * Indicates whether the thread synchronizer has been set
     */
    private Boolean hasSet = Boolean.FALSE;
    /**
     * Data buffer, buffer a batch of data
     */
    private List<Map<String, Pair<AbstractDataType, Object>>> rows;
    /**
     * Collection of column names of mock table
     */
    private Set<String> columnSet;
    /**
     * The current line in the buffer
     */
    private Map<String, Pair<AbstractDataType, Object>> currentRow;
    /**
     * Data pipeline collection through which data is sent out
     */
    private final List<AbstractDataPipe> dataPipes;
    /**
     * The data flushing threshold, the data in the buffer reaches this value and the value pipeline
     * will be forced to refresh
     */
    private final Long flushThreshold;
    /**
     * Lock object, used to protect the currentRow object
     */
    private Lock lock = new ReentrantLock();

    public MockerBuffer(Map<String, AbstractDataType> tableSchema, Long batchSize, int concurrent) {
        if (batchSize < 0 || concurrent < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Batch size or concurrent size can not be null");
        }
        if (tableSchema == null || tableSchema.size() == 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Table schame can not be null or empty");
        }
        this.columnSet = tableSchema.keySet();
        this.rows = new ArrayList<>(batchSize.intValue() * 2);
        this.dataPipes = new ArrayList<>();
        this.flushThreshold = batchSize;
        this.synchronizer = new CyclicBarrier(concurrent, null);
    }

    public MockerBuffer(Map<String, AbstractDataType> tableSchema, Long batchSize) {
        if (batchSize < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Batch size can not be null");
        }
        if (tableSchema == null || tableSchema.size() == 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Table schame can not be null or empty");
        }
        this.columnSet = tableSchema.keySet();
        this.rows = new ArrayList<>(batchSize.intValue() * 2);
        this.dataPipes = new ArrayList<>();
        this.flushThreshold = batchSize;
        this.synchronizer = new CyclicBarrier(1, null);
    }

    /**
     * Set the concurrency number of the buffer object. Note: Once the write method is called to write
     * data, it cannot be set, otherwise an error will be reported
     *
     * @param count Concurrency
     * @throws MockerException Setting a negative value or repeating the setting will cause errors
     */
    public synchronized void setConcurrent(int count) {
        if (this.hasSet) {
            throw new MockerException(MockerError.OPERATION_FAILURE, "Concurrent count can not be set repeatedly");
        }
        if (count < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Concurrent for mock buffer can not be smaller than zero");
        }
        this.synchronizer = new CyclicBarrier(count, null);
    }

    /**
     * Get the number of concurrent buffers
     *
     * @return Return the number of concurrent
     */
    public int getParties() {
        return this.synchronizer.getParties();
    }

    /**
     * Register a data pipeline, this method can be called multiple times to register multiple data
     * pipelines in the buffer
     *
     * @param dataPipe Data pipeline
     */
    public void register(AbstractDataPipe dataPipe) {
        if (dataPipe == null) {
            return;
        }
        synchronized (this.dataPipes) {
            this.dataPipes.add(dataPipe);
        }
    }

    /**
     * Write a collection of column data to the buffer
     *
     * @param data Column data collection
     * @param timeout Write timeout
     * @param timeUnit time unit
     * @throws InterruptedException May be interrupted
     */
    public void write(Map<String, Pair<AbstractDataType, Object>> data, long timeout, TimeUnit timeUnit)
            throws Exception {
        Validate.notNull(timeUnit, "time unit for buffer write timeout can not be null");
        Validate.isTrue(timeout > 0, "timeout for buffer write can not be negative");
        if (isClosed()) {
            throw new MockerException(MockerError.OPERATION_FAILURE, "Buffer has been closed");
        }
        this.hasSet = Boolean.TRUE;
        lock.lock();
        try {
            if (this.currentRow == null) {
                this.currentRow = new HashMap<>();
            }
            Set<Map.Entry<String, Pair<AbstractDataType, Object>>> entrySet = data.entrySet();
            for (Map.Entry<String, Pair<AbstractDataType, Object>> entry : entrySet) {
                writeToCurrentRow(new Pair<>(entry.getKey(), entry.getValue()));
            }
            reload(timeout, timeUnit);
        } finally {
            lock.unlock();
        }
        this.synchronizer.await(30, TimeUnit.SECONDS);
    }

    /**
     * Write a column of data to the buffer
     *
     * @param column Column data
     * @param timeout Write timeout
     * @param timeUnit time unit
     * @throws InterruptedException May be interrupted
     */
    public void write(Pair<String, Pair<AbstractDataType, Object>> column, long timeout, TimeUnit timeUnit)
            throws Exception {
        Map<String, Pair<AbstractDataType, Object>> inputRow = new HashMap<>();
        inputRow.putIfAbsent(column.getKey(), column.getValue());
        write(inputRow, timeout, timeUnit);
    }

    /**
     * To write a column of data to the current cursor row, the column name of the column data must be
     * in the column set defined by the table schema, otherwise an error is reported, and the column
     * data is not written in the current cursor row
     *
     * @param column Column data
     */
    private void writeToCurrentRow(Pair<String, Pair<AbstractDataType, Object>> column) {
        if (!this.columnSet.contains(column.getKey())) {
            MockerException e = new MockerException(MockerError.UNKNOWN_COLUMN_NAME,
                    String.format("Custom column \"%s\" is not in column set [%s]", column,
                            this.columnSet.stream().collect(Collectors.joining(","))));
            log.error("Column error", e);
            throw e;
        }
        if (this.currentRow.get(column.getKey()) != null) {
            MockerException e =
                    new MockerException(MockerError.PARAMETER_ERROR, String.format("Custom column \"%s\" is duplicate",
                            column.getKey()));
            log.error("Column error", e);
            throw e;
        }
        this.currentRow.putIfAbsent(column.getKey(), column.getValue());
    }

    /**
     * Reload the cursor row and row data collection
     *
     * @param timeout Write timeout
     * @param timeUnit time unit
     * @throws InterruptedException Writing data to the pipeline is a blocking operation and may be
     *         interrupted
     */
    private void reload(long timeout, TimeUnit timeUnit) throws Exception {
        if (this.currentRow.size() > this.columnSet.size()) {
            MockerException e = new MockerException(MockerError.UNKNOWN_COLUMN_NAME,
                    String.format("There are unknown columns in current column [%s]",
                            this.currentRow.keySet().stream().collect(Collectors.joining(","))));
            log.error("Column error", e);
            throw e;
        } else if (this.currentRow.size() == this.columnSet.size()) {
            this.rows.add(this.currentRow);
            if (this.rows.size() >= this.flushThreshold) {
                this.flush(timeout, timeUnit);
            }
            this.currentRow = new HashMap<>();
        }
    }

    /**
     * Close the buffer object, this method is a blocking method. If multiple threads are referencing
     * the buffer, you need to block until the last thread call to properly close the buffer
     *
     * @throws BrokenBarrierException The barrier may be broken
     * @throws InterruptedException Blocking methods may be interrupted
     */
    public void close(long timeout, TimeUnit timeUnit) throws Exception {
        if (isClosed()) {
            return;
        }
        this.synchronizer.await();
        synchronized (this.dataPipes) {
            if (!isClosed()) {
                for (AbstractDataPipe dataPipe : this.dataPipes) {
                    if (!dataPipe.isClosed()) {
                        dataPipe.write(this.rows, timeout, timeUnit);
                        dataPipe.close();
                    }
                }
                this.isClose = Boolean.TRUE;
            }
        }
    }

    /**
     * Whether the buffer is closed
     *
     * @return Return whether to close
     */
    public Boolean isClosed() {
        return this.isClose;
    }

    /**
     * Forcibly refresh the cache, flush all the data in the row cache to the pipeline
     *
     * @param timeout Write timeout
     * @param timeUnit time unit
     * @throws InterruptedException Pipe write operation is a blocking operation and may be interrupted
     */
    public synchronized void flush(long timeout, TimeUnit timeUnit) throws Exception {
        if (dataPipes != null) {
            for (AbstractDataPipe dataPipe : dataPipes) {
                dataPipe.write(this.rows, timeout, timeUnit);
            }
        }
        this.rows = new ArrayList<>(this.flushThreshold.intValue() * 2);
    }
}
