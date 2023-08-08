/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.tools.datamocker.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.oceanbase.tools.datamocker.core.task.AbstractDataPipe;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
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
    private volatile boolean isClose = false;
    /**
     * Thread synchronizer
     */
    private CyclicBarrier synchronizer;
    /**
     * Indicates whether the thread synchronizer has been set
     */
    private volatile boolean hasSet = false;
    /**
     * Data buffer, buffer a batch of data
     */
    private List<MockRowData> rows;
    /**
     * Collection of column names of mock table
     */
    private final Set<String> columnSet;
    /**
     * The current line in the buffer
     */
    private MockRowData currentRow;
    /**
     * Data pipeline collection through which data is sent out
     */
    private final List<AbstractDataPipe<List<MockRowData>>> dataPipes;
    /**
     * The data flushing threshold, the data in the buffer reaches this value and the value pipeline
     * will be forced to refresh
     */
    private final Long flushThreshold;
    /**
     * Lock object, used to protect the currentRow object
     */
    private final Lock lock = new ReentrantLock();

    public MockerBuffer(Map<String, AbstractDataType<?, ? extends Comparable<?>>> tableSchema, Long batchSize,
            int concurrent) {
        Validate.isTrue(batchSize > 0, "Batch size can not be negative for MockerBuffer");
        Validate.notEmpty(tableSchema, "Table schame can not be empty for MockerBuffer");
        this.columnSet = tableSchema.keySet();
        this.rows = new ArrayList<>(batchSize.intValue() * 2);
        this.dataPipes = new ArrayList<>();
        this.flushThreshold = batchSize;
        this.synchronizer = new CyclicBarrier(concurrent, null);
    }

    public MockerBuffer(Map<String, AbstractDataType<?, ? extends Comparable<?>>> tableSchema, Long batchSize) {
        Validate.isTrue(batchSize > 0, "Batch size can not be negative for MockerBuffer");
        Validate.notEmpty(tableSchema, "Table schame can not be empty for MockerBuffer");
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
        Validate.isTrue(count >= 0, "Concurrent can not be negative for MockBuffer#setConcurrent");
        if (this.hasSet) {
            throw new MockerException(MockerError.OPERATION_FAILURE, "Concurrent count can not be set repeatedly");
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
    public void register(AbstractDataPipe<List<MockRowData>> dataPipe) {
        if (dataPipe == null) {
            return;
        }
        synchronized (this.dataPipes) {
            this.dataPipes.add(dataPipe);
        }
    }

    /**
     * Write a row of data to the buffer
     *
     * @param mockRowData row data
     * @param timeout Write timeout
     * @param timeUnit time unit
     * @throws InterruptedException May be interrupted
     */
    public void write(MockRowData mockRowData, long timeout, TimeUnit timeUnit) throws Exception {
        Validate.notNull(timeUnit, "TimeUnit for buffer write can not be null");
        Validate.isTrue(timeout > 0, "Timeout for buffer write can not be negative");
        if (this.isClose) {
            throw new MockerException(MockerError.OPERATION_FAILURE, "Buffer has been closed");
        }
        this.hasSet = true;
        lock.lock();
        try {
            if (this.currentRow == null) {
                this.currentRow = new MockRowData();
            }
            List<MockColumnData<?>> mockColumns = mockRowData.getMockColumns();
            for (MockColumnData<?> mockColumn : mockColumns) {
                writeToCurrentRow(mockColumn);
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
     * @param mockColumn Column data
     * @param timeout Write timeout
     * @param timeUnit time unit
     * @throws InterruptedException May be interrupted
     */
    public void write(MockColumnData<?> mockColumn, long timeout, TimeUnit timeUnit) throws Exception {
        write(new MockRowData(Collections.singletonList(mockColumn)), timeout, timeUnit);
    }

    /**
     * To write a column of data to the current cursor row, the column name of the column data must be
     * in the column set defined by the table schema, otherwise an error is reported, and the column
     * data is not written in the current cursor row
     *
     * @param column Column data
     */
    private void writeToCurrentRow(MockColumnData<?> column) {
        Validate.notNull(column, "MockColumn can not be null for MockBuffer#writeToCurrentRow");
        String columName = column.getColumnName();
        if (!this.columnSet.contains(columName)) {
            throw new MockerException(MockerError.UNKNOWN_COLUMN_NAME, String.format(
                    "Custom column \"%s\" is not in column set [%s]", columName, String.join(",", this.columnSet)));
        }
        if (this.currentRow.getMockColumn(columName) != null) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    String.format("Custom column \"%s\" is duplicate", columName));
        }
        this.currentRow.addMockColumn(column);
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
        if (this.currentRow.columnNum() > this.columnSet.size()) {
            throw new MockerException(MockerError.UNKNOWN_COLUMN_NAME,
                    String.format("There are unknown columns in current column [%s]", this.currentRow.columnNames()));
        } else if (this.currentRow.columnNum() == this.columnSet.size()) {
            this.rows.add(this.currentRow);
            if (this.rows.size() >= this.flushThreshold) {
                this.flush(timeout, timeUnit);
            }
            this.currentRow = new MockRowData();
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
        if (this.isClose) {
            return;
        }
        this.synchronizer.await();
        synchronized (this.dataPipes) {
            if (!this.isClose) {
                for (AbstractDataPipe<List<MockRowData>> dataPipe : this.dataPipes) {
                    if (!dataPipe.isClosed()) {
                        dataPipe.write(this.rows, timeout, timeUnit);
                        dataPipe.close();
                    }
                }
                this.isClose = true;
            }
        }
    }

    /**
     * Whether the buffer is closed
     *
     * @return Return whether to close
     */
    public boolean isClosed() {
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
            for (AbstractDataPipe<List<MockRowData>> dataPipe : dataPipes) {
                dataPipe.write(this.rows, timeout, timeUnit);
            }
        }
        this.rows = new ArrayList<>(this.flushThreshold.intValue() * 2);
    }

}
