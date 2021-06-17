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
 * mock数据的缓冲区，用于缓冲产生出的临时数据
 *
 * @author yh263208
 * @date 2021-01-14 20:48
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class MockerBuffer {
    /**
     * s是否关闭
     */
    private Boolean isClose = Boolean.FALSE;
    /**
     * 线程同步器
     */
    private CyclicBarrier synchronizer;
    /**
     * 表示是否已经设定过线程同步器
     */
    private Boolean hasSet = Boolean.FALSE;
    /**
     * 数据缓冲区，缓冲一个batch的数据
     */
    private List<Map<String, Pair<AbstractDataType, Object>>> rows;
    /**
     * mock的表的列名集合
     */
    private Set<String> columnSet;
    /**
     * 缓冲中的当前行
     */
    private Map<String, Pair<AbstractDataType, Object>> currentRow;
    /**
     * 数据管道集合，通过该管道集合向外发送数据
     */
    private final List<AbstractDataPipe> dataPipes;
    /**
     * 数据刷写阀值，缓冲中的数据达到该值则会强制刷新值管道
     */
    private final Long flushThreshold;
    /**
     * 锁对象，用于保护currentRow对象
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
     * 设定缓冲对象的并发数，注意：一旦调用过write方法写数据之后就不能在进行设定，否则会报错
     *
     * @param count 并发数
     * @throws MockerException 设定一个负值或者重复设定都会出错
     */
    public synchronized void setConcurrent(int count) {
        if (this.hasSet) {
            throw new MockerException(MockerError.OPERATION_FAILURE, "Concurrent count can not be set repeatedly");
        }
        if (count < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Concurrent for mock buffer can not be smaller than zero");
        }
        this.synchronizer = new CyclicBarrier(count, null);
    }

    /**
     * 获取缓冲的并发数目
     *
     * @return 返回并发数
     */
    public int getParties() {
        return this.synchronizer.getParties();
    }

    /**
     * 注册一个数据管道，该方法可以调用多次向缓冲中注册多个数据管道
     *
     * @param dataPipe 数据管道
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
     * 向缓冲中写入列数据集合
     *
     * @param data     列数据集合
     * @param timeout  写入超时时间
     * @param timeUnit 时间单位
     * @throws InterruptedException 可能会被中断
     */
    public void write(Map<String, Pair<AbstractDataType, Object>> data, long timeout, TimeUnit timeUnit) throws Exception {
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
     * 向缓冲中写入一条列数据
     *
     * @param column   列数据
     * @param timeout  写入超时时间
     * @param timeUnit 时间单位
     * @throws InterruptedException 可能会被中断
     */
    public void write(Pair<String, Pair<AbstractDataType, Object>> column, long timeout, TimeUnit timeUnit) throws Exception {
        Map<String, Pair<AbstractDataType, Object>> inputRow = new HashMap<>();
        inputRow.putIfAbsent(column.getKey(), column.getValue());
        write(inputRow, timeout, timeUnit);
    }

    /**
     * 向当前游标行中写入一列数据，要求该列数据的列名必须在表schema定义的列集合中，否则报错，且当前游标行中未写入该列数据
     *
     * @param column 列数据
     */
    private void writeToCurrentRow(Pair<String, Pair<AbstractDataType, Object>> column) {
        if (!this.columnSet.contains(column.getKey())) {
            MockerException e = new MockerException(MockerError.UNKNOWN_COLUMN_NAME,
                    String.format("Custom column \"%s\" is not in column set [%s]", column,
                            this.columnSet.stream().collect(Collectors.joining(","))));
            log.error("column error", e);
            throw e;
        }
        if (this.currentRow.get(column.getKey()) != null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, String.format("Custom column \"%s\" is duplicate",
                    column.getKey()));
            log.error("column error", e);
            throw e;
        }
        this.currentRow.putIfAbsent(column.getKey(), column.getValue());
    }

    /**
     * 重加载游标行和行数据集合
     *
     * @param timeout  写入超时时间
     * @param timeUnit 时间单位
     * @throws InterruptedException 管道写入数据是一个阻塞操作，可能被中断
     */
    private void reload(long timeout, TimeUnit timeUnit) throws Exception {
        if (this.currentRow.size() > this.columnSet.size()) {
            MockerException e = new MockerException(MockerError.UNKNOWN_COLUMN_NAME,
                    String.format("There are unknown columns in current column [%s]",
                            this.currentRow.keySet().stream().collect(Collectors.joining(","))));
            log.error("column error", e);
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
     * 关闭缓冲对象，该方法是一个阻塞方法如果有多个线程都在引用该缓冲则需要阻塞到最后一个线程调用才能正确关闭缓冲
     *
     * @throws BrokenBarrierException 篱笆可能会被冲破
     * @throws InterruptedException 阻塞方法可能会被中断
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
     * 缓冲区是否关闭
     *
     * @return 返回是否关闭
     */
    public Boolean isClosed() {
        return this.isClose;
    }

    /**
     * 强制刷新缓存，将行缓存中的数据全部刷新至管道中
     *
     * @param timeout  写入超时时间
     * @param timeUnit 时间单位
     * @throws InterruptedException 管道写入操作是一个阻塞操作，可能被中断
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
