package com.oceanbase.tools.datamocker.core.task;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象数据管道，用于在两个线程之间进行数据传递
 * 范型T代表数据管道中传递的对象类型
 *
 * @author yh263208
 * @date 2021-01-14 15:32
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public abstract class AbstractDataPipe<T> {
    /**
     * 管道状态，用于描述管道当前的状态。有开启和关闭两种状态，默认为开启
     */
    private Boolean closed = Boolean.FALSE;
    /**
     * 最大留存数量，意为留存在数据管道中最大的数据量
     */
    private int maxRetained = Integer.MAX_VALUE;
    /**
     * 锁对象，用于进行留存数量控制
     */
    private final Lock lock;
    /**
     * 最大数量的条件控制对象
     */
    private final Condition matchMaxSizeCondition;

    public AbstractDataPipe(int maxRetained) {
        if (maxRetained > 0) {
            this.maxRetained = maxRetained;
        }
        lock = new ReentrantLock();
        matchMaxSizeCondition = lock.newCondition();
    }

    /**
     * 管道的写入方法，通过该方法向管道中写入一条记录
     *
     * @param timout   最长阻塞时间
     * @param timeUnit 时间单位
     * @param row      写入的数据
     */
    public void write(List<T> row, long timout, TimeUnit timeUnit) throws Exception {
        if (isClosed()) {
            throw new MockerException(MockerError.OPERATION_FAILURE, "data pipe has been closed");
        }
        if (row == null) {
            return;
        }
        lock.lock();
        try {
            while (size() >= maxRetained) {
                log.warn(
                        "the data pipeline has reached the upper limit, the thread will be suspended for up to 120 seconds. "
                        + "maxRetained={},threadName={}",
                        maxRetained, Thread.currentThread().getName());
                matchMaxSizeCondition.await(120, TimeUnit.SECONDS);
            }
        } finally {
            lock.unlock();
        }
        doWrite(row, timout, timeUnit);
    }

    /**
     * 管道的写入方法，通过该方法向管道中写入一条记录
     *
     * @param row 写入的数据
     */
    public void write(List<T> row) throws Exception {
        write(row, -1, null);
    }

    /**
     * 管道的写入方法实现类
     *
     * @param timout   最长阻塞时间
     * @param timeUnit 时间单位
     * @param row      写入的数据
     */
    abstract public void doWrite(List<T> row, long timout, TimeUnit timeUnit)
            throws Exception;

    /**
     * 管道的读取方法，通过该方法从管道中读出一条记录
     *
     * @return 返回一条记录
     */
    public List<T> read() throws Exception {
        return read(-1, null);
    }

    /**
     * 管道的读取方法，通过该方法从管道中读出一条记录
     *
     * @param timout   最长阻塞时间
     * @param timeUnit 时间单位
     * @return 返回一条记录
     */
    public List<T> read(long timout, TimeUnit timeUnit) throws Exception {
        if (isClosed() && size() == 0) {
            return null;
        }
        lock.lock();
        try {
            if (size() < maxRetained) {
                log.info(
                        "the data in the current data pipeline is less than the maximum limit, and the suspended thread will be awakened."
                        + " maxRetained={},threadName={}",
                        maxRetained, Thread.currentThread().getName());
                matchMaxSizeCondition.signalAll();
            }
        } finally {
            lock.unlock();
        }
        return doRead(timout, timeUnit);
    }

    /**
     * 管道的读出实现逻辑
     *
     * @param timout   最长阻塞时间
     * @param timeUnit 时间单位
     * @return 返回读出的数据
     */
    abstract public List<T> doRead(long timout, TimeUnit timeUnit) throws Exception;

    /**
     * 返回管道中数据的数量
     *
     * @return 返回数量
     */
    abstract public Long size();

    /**
     * 管道是否关闭
     *
     * @return 返回是否关闭的布尔值
     */
    public Boolean isClosed() {
        return this.closed;
    }

    /**
     * 关闭管道
     */
    public synchronized void close() {
        this.closed = Boolean.TRUE;
    }

}
