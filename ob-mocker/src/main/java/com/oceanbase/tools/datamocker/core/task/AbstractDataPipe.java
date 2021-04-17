package com.oceanbase.tools.datamocker.core.task;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.Validate;

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
    private final Condition notFullCondition;
    /**
     * 数据管道全空条件控制对象
     */
    private final Condition notEmptyCondition;
    /**
     * 条件等待超时时间
     */
    private final static long CONDITION_WAIT_TIMEOUTSEC = 5;

    public AbstractDataPipe(int maxRetained) {
        if (maxRetained > 0) {
            this.maxRetained = maxRetained;
        }
        lock = new ReentrantLock();
        notFullCondition = lock.newCondition();
        notEmptyCondition = lock.newCondition();
    }

    /**
     * 管道的写入方法，通过该方法向管道中写入一条记录
     *
     * @param timeout  最长阻塞时间
     * @param timeUnit 时间单位
     * @param row      写入的数据
     */
    public void write(List<T> row, long timeout, TimeUnit timeUnit) throws Exception {
        Validate.isTrue(timeout >= 0, "timeout for pipeline write can not be negative");
        Validate.notNull(timeUnit, "timeout can not be null");
        if (isClosed()) {
            throw new MockerException(MockerError.OPERATION_FAILURE, "data pipe has been closed");
        }
        if (row == null) {
            return;
        }
        lock.lock();
        try {
            long maxLoopCount = TimeUnit.SECONDS.convert(timeout, timeUnit) / CONDITION_WAIT_TIMEOUTSEC + 1;
            while (size() >= maxRetained && (maxLoopCount--) > 0) {
                notFullCondition.await(CONDITION_WAIT_TIMEOUTSEC, TimeUnit.SECONDS);
            }
            if (maxLoopCount == -1) {
                log.warn("timeout for pipeline write in, will return. currentSize={},maxRetained={},threadName={}", size(), maxRetained,
                        Thread.currentThread().getName());
                return;
            }
        } finally {
            lock.unlock();
        }
        doWrite(row, timeout, timeUnit);
        lock.lock();
        try {
            if (size() > 0) {
                notEmptyCondition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 管道的写入方法，通过该方法向管道中写入一条记录
     *
     * @param row 写入的数据
     */
    public void write(List<T> row) throws Exception {
        write(row, Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    /**
     * 管道的写入方法实现类
     *
     * @param timeout  最长阻塞时间
     * @param timeUnit 时间单位
     * @param row      写入的数据
     */
    abstract public void doWrite(List<T> row, long timeout, TimeUnit timeUnit)
            throws Exception;

    /**
     * 管道的读取方法，通过该方法从管道中读出一条记录
     *
     * @return 返回一条记录
     */
    public List<T> read() throws Exception {
        return read(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    /**
     * 管道的读取方法，通过该方法从管道中读出一条记录
     *
     * @param timeout  最长阻塞时间
     * @param timeUnit 时间单位
     * @return 返回一条记录
     */
    public List<T> read(long timeout, TimeUnit timeUnit) throws Exception {
        Validate.isTrue(timeout >= 0, "timeout for pipeline write can not be negative");
        Validate.notNull(timeUnit, "timeout can not be null");
        if (isClosed() && size() == 0) {
            return null;
        }
        lock.lock();
        try {
            long maxLoopCount = TimeUnit.SECONDS.convert(timeout, timeUnit) / CONDITION_WAIT_TIMEOUTSEC + 1;
            while (size() <= 0 && (maxLoopCount--) > 0) {
                notEmptyCondition.await(CONDITION_WAIT_TIMEOUTSEC, TimeUnit.SECONDS);
            }
            if (maxLoopCount == -1) {
                log.warn("timeout for pipeline read out, will return. currentSize={},maxRetained={},threadName={}", size(), maxRetained,
                        Thread.currentThread().getName());
                return Collections.emptyList();
            }
        } finally {
            lock.unlock();
        }
        List<T> returnVal = doRead(timeout, timeUnit);
        lock.lock();
        try {
            if (size() < maxRetained) {
                notFullCondition.signalAll();
            }
        } finally {
            lock.unlock();
        }
        return returnVal;

    }

    /**
     * 管道的读出实现逻辑
     *
     * @param timeout  最长阻塞时间
     * @param timeUnit 时间单位
     * @return 返回读出的数据
     */
    abstract public List<T> doRead(long timeout, TimeUnit timeUnit) throws Exception;

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
