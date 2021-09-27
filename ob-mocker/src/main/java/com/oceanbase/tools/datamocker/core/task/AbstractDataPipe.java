package com.oceanbase.tools.datamocker.core.task;

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
    private volatile boolean closed = false;
    /**
     * The maximum retention amount means the maximum amount of data retained in the data pipeline
     */
    private int maxRetained = Integer.MAX_VALUE;
    /**
     * Lock object, used to control the number of retained
     */
    private final Lock writeLock = new ReentrantLock();
    /**
     * Lock object, used to control the number of retained
     */
    private final Lock readLock = new ReentrantLock();
    /**
     * Maximum number of conditional control objects
     */
    private final Condition notFullCondition = writeLock.newCondition();
    /**
     * Data pipeline full empty condition control object
     */
    private final Condition notEmptyCondition = readLock.newCondition();

    public AbstractDataPipe(int maxRetained) {
        if (maxRetained > 0) {
            this.maxRetained = maxRetained;
        }
    }

    /**
     * The write method of the pipeline, by which a record is written to the pipeline
     *
     * @param timeout timeout for write operation
     * @param timeUnit unit for timeout
     * @param element element of data
     * @throws InterruptedException exception will be thrown when fail to write data
     */
    public void write(T element, long timeout, TimeUnit timeUnit) throws InterruptedException {
        Validate.isTrue(timeout >= 0, "Timeout for pipeline write can not be negative");
        Validate.notNull(timeUnit, "Timeout can not be null");
        long methodStart = System.currentTimeMillis();
        long timeoutMillSecs = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
        if (isClosed()) {
            throw new MockerException(MockerError.OPERATION_FAILURE, "Data pipe has been closed");
        }
        if (element == null) {
            throw new NullPointerException("Input element can not be null");
        }
        writeLock.lock();
        try {
            long remainingTimeout = timeoutMillSecs - (System.currentTimeMillis() - methodStart);
            while (size() >= maxRetained && remainingTimeout > 0) {
                log.debug("Start write waiting, currentSize={}, maxRetained={}, threadName={}", size(), maxRetained,
                        Thread.currentThread().getName());
                notFullCondition.await(remainingTimeout, TimeUnit.MILLISECONDS);
                remainingTimeout = timeoutMillSecs - (System.currentTimeMillis() - methodStart);
                log.debug("End write waiting, currentSize={}, maxRetained={}, threadName={}", size(), maxRetained,
                        Thread.currentThread().getName());
            }
            if (remainingTimeout <= 0) {
                log.warn(
                        "Data pipeline write operation timed out and will return, currentSize={}, maxRetained={}, threadName={}",
                        size(), maxRetained, Thread.currentThread().getName());
                return;
            }
        } finally {
            writeLock.unlock();
        }
        long remainingTimeout = timeoutMillSecs - (System.currentTimeMillis() - methodStart);
        doWrite(element, remainingTimeout, TimeUnit.MILLISECONDS);
        readLock.lock();
        try {
            if (size() > 0) {
                notEmptyCondition.signalAll();
            }
        } finally {
            readLock.unlock();
        }
    }

    /**
     * The write method of the pipeline, by which a record is written to the pipeline
     *
     * @param element element of data
     * @exception InterruptedException exception will be thrown when fail to write data
     */
    public void write(T element) throws InterruptedException {
        write(element, Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    /**
     * The write method of the pipeline, by which a record is written to the pipeline
     *
     * @param timeout timeout for write operation
     * @param timeUnit unit for timeout
     * @param element element of data
     * @throws InterruptedException exception will be thrown when fail to write data
     */
    abstract protected void doWrite(T element, long timeout, TimeUnit timeUnit) throws InterruptedException;

    /**
     * The read method of the pipeline, by which a record is read from the pipeline
     *
     * @return list of data
     */
    public T read() throws InterruptedException {
        return read(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    /**
     * The read method of the pipeline, by which a record is read from the pipeline
     *
     * @param timeout timeout for write operation
     * @param timeUnit unit for timeout
     * @return list of data
     * @exception InterruptedException exception will be thrown when fail to read
     */
    public T read(long timeout, TimeUnit timeUnit) throws InterruptedException {
        Validate.isTrue(timeout >= 0, "Timeout for pipeline read can not be negative");
        Validate.notNull(timeUnit, "Timeout can not be null");
        long methodStart = System.currentTimeMillis();
        long timeoutMillSecs = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
        readLock.lock();
        try {
            long remainingTimeout = timeoutMillSecs - (System.currentTimeMillis() - methodStart);
            while (size() <= 0 && remainingTimeout > 0) {
                log.debug("Start read waiting, currentSize={}, maxRetained={}, threadName={}", size(), maxRetained,
                        Thread.currentThread().getName());
                notEmptyCondition.await(remainingTimeout, TimeUnit.MILLISECONDS);
                remainingTimeout = timeoutMillSecs - (System.currentTimeMillis() - methodStart);
                log.debug("End read waiting, currentSize={}, maxRetained={}, threadName={}", size(), maxRetained,
                        Thread.currentThread().getName());
            }
            if (remainingTimeout <= 0) {
                log.warn(
                        "Data pipeline read operation timed out and will return, currentSize={}, maxRetained={}, threadName={}",
                        size(), maxRetained, Thread.currentThread().getName());
                return null;
            }
        } finally {
            readLock.unlock();
        }
        long remainingTimeout = timeoutMillSecs - (System.currentTimeMillis() - methodStart);
        T returnVal = doRead(remainingTimeout, TimeUnit.MILLISECONDS);
        writeLock.lock();
        try {
            if (size() < maxRetained) {
                notFullCondition.signalAll();
            }
        } finally {
            writeLock.unlock();
        }
        return returnVal;
    }

    /**
     * The read method of the pipeline, by which a record is read from the pipeline
     *
     * @param timeout timeout for write operation
     * @param timeUnit unit for timeout
     * @return list of data
     * @exception InterruptedException exception will be thrown when fail to read
     */
    abstract protected T doRead(long timeout, TimeUnit timeUnit) throws InterruptedException;

    /**
     * Returns the amount of data in the pipeline
     *
     * @return size for data pipe
     */
    abstract public long size();

    /**
     * Whether the pipeline is closed
     *
     * @return Returns a boolean value of whether to close
     */
    public boolean isClosed() {
        return this.closed;
    }

    /**
     * Method to close the pipeline
     */
    public synchronized void close() {
        this.closed = true;
    }

}
