package com.oceanbase.tools.datamocker.schedule;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import com.oceanbase.tools.datamocker.core.task.TableTask;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * The mock data thread pool executes the serivce object,
 * used to encapsulate the call and execution of the thread pool
 *
 * @author yh263208
 * @date 2021-01-18 11:24
 * @since OBMOCKER_0.1.0_snapshot
 */
public class MockExecutorService {
    /**
     * Thread pool, this class needs to accept an incoming thread pool
     */
    private final ThreadPoolExecutor executor;

    public MockExecutorService(ThreadPoolExecutor executor) {
        if (executor == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Executor for mock executor service can not be null");
        }
        this.executor = executor;
    }

    /**
     * Return a new FutureTask
     *
     * @param task Tasks to be performed
     * @return FustureTask object
     */
    private <V> RunnableFuture<V> newTaskFor(Callable<V> task) {
        return new FutureTask<>(task);
    }

    private <V> RunnableFuture<V> newTaskFor(Runnable task, V result) {
        return new FutureTask<>(task, result);
    }

    /**
     * Submit a TaskBean for execution
     *
     * @param taskBean Submitted TaskBean
     */
    public synchronized TableTaskContext submit(TableTask taskBean) {
        if (taskBean == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Task bean for executor service can not be null");
        }
        taskBean.getContext().setStatus(MockTaskStatus.RUNNING);
        submitCallable(taskBean.getBeforeTask(), taskBean.getContext());
        return taskBean.getContext();
    }

    /**
     * Submit a specific callable task for execution, this method is not needed in normal calls
     *
     * @param task    Task to be performed
     * @param context Context object for mock data subtask
     */
    public synchronized <V> void submitCallable(Callable<V> task, TableTaskContext context) {
        if (task == null || context == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Callable or context for executor service can not be null");
        }
        if (!context.isShutdown() && !isShutdown()) {
            Future future = executor.submit(newTaskFor(task));
            context.appendHandle(future);
        }
    }

    /**
     * Submit a specific callable task for execution, this method is not needed in normal calls
     *
     * @param task Task to be performed
     */
    public synchronized <V> void submitCallable(Callable<V> task) {
        if (task == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Callable or context for executor service can not be null");
        }
        executor.submit(newTaskFor(task));
    }

    @Deprecated
    public synchronized <V> void submit(Runnable task, V result, TableTaskContext context) {
        if (task == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Callable for executor service can not be null");
        }
        RunnableFuture<V> f = newTaskFor(task, result);
        Future future = executor.submit(f);
        context.appendHandle(future);
    }

    public void shutdown() {
        this.executor.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return this.executor.shutdownNow();
    }

    public boolean isShutdown() {
        return this.executor.isShutdown();
    }

    public int getActiveCount() {
        return this.executor.getActiveCount();
    }

    public int getCorePoolSize() {
        return this.executor.getCorePoolSize();
    }

    public int getMaximumPoolSize() {
        return this.executor.getMaximumPoolSize();
    }
}
