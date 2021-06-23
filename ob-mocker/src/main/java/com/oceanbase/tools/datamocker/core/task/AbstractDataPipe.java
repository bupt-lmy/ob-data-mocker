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
 * Abstract data pipeline, used for data transfer between two threads. Paradigm T represents the
 * type of object passed in the data pipeline
 *
 * @author yh263208
 * @date 2021-01-14 15:32
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public abstract class AbstractDataPipe<T> {
    /**
     * Pipeline state, used to describe the current state of the pipeline. There are two states of on
     * and off, the default is on
     */
    private Boolean closed = Boolean.FALSE;
    /**
     * The maximum retention amount means the maximum amount of data retained in the data pipeline
     */
    private int maxRetained = Integer.MAX_VALUE;
    /**
     * Lock object, used to control the number of retained
     */
    private final Lock lock;
    /**
     * Maximum number of conditional control objects
     */
    private final Condition notFullCondition;
    /**
     * Data pipeline full empty condition control object
     */
    private final Condition notEmptyCondition;
    /**
     * Condition wait timeout
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
     * The write method of the pipeline, by which a record is written to the pipeline
     *
     * @param timeout timeout for write operation
     * @param timeUnit unit for timeout
     * @param row row of data
     * @throws Exception exception will be thrown when fail to write data
     */
    public void write(List<T> row, long timeout, TimeUnit timeUnit) throws Exception {
        Validate.isTrue(timeout >= 0, "Timeout for pipeline write can not be negative");
        Validate.notNull(timeUnit, "Timeout can not be null");
        if (isClosed()) {
            throw new MockerException(MockerError.OPERATION_FAILURE, "Data pipe has been closed");
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
                log.warn(
                        "Data pipeline write operation timed out and will return, currentSize={}, maxRetained={}, threadName={}",
                        size(), maxRetained, Thread.currentThread().getName());
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
     * The write method of the pipeline, by which a record is written to the pipeline
     *
     * @param row row of data
     * @exception Exception exception will be thrown when fail to write data
     */
    public void write(List<T> row) throws Exception {
        write(row, Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    /**
     * The write method of the pipeline, by which a record is written to the pipeline
     *
     * @param timeout timeout for write operation
     * @param timeUnit unit for timeout
     * @param row row of data
     * @throws Exception exception will be thrown when fail to write data
     */
    abstract public void doWrite(List<T> row, long timeout, TimeUnit timeUnit) throws Exception;

    /**
     * The read method of the pipeline, by which a record is read from the pipeline
     *
     * @return list of data
     */
    public List<T> read() throws Exception {
        return read(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    /**
     * The read method of the pipeline, by which a record is read from the pipeline
     *
     * @param timeout timeout for write operation
     * @param timeUnit unit for timeout
     * @return list of data
     * @exception Exception exception will be thrown when fail to read
     */
    public List<T> read(long timeout, TimeUnit timeUnit) throws Exception {
        Validate.isTrue(timeout >= 0, "Timeout for pipeline write can not be negative");
        Validate.notNull(timeUnit, "Timeout can not be null");
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
                log.warn(
                        "Data pipeline read operation timed out and will return, currentSize={}, maxRetained={}, threadName={}",
                        size(), maxRetained, Thread.currentThread().getName());
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
     * The read method of the pipeline, by which a record is read from the pipeline
     *
     * @param timeout timeout for write operation
     * @param timeUnit unit for timeout
     * @return list of data
     * @exception Exception exception will be thrown when fail to read
     */
    abstract public List<T> doRead(long timeout, TimeUnit timeUnit) throws Exception;

    /**
     * Returns the amount of data in the pipeline
     *
     * @return size for data pipe
     */
    abstract public Long size();

    /**
     * Whether the pipeline is closed
     *
     * @return Returns a boolean value of whether to close
     */
    public Boolean isClosed() {
        return this.closed;
    }

    /**
     * Method to close the pipeline
     */
    public synchronized void close() {
        this.closed = Boolean.TRUE;
    }

}
