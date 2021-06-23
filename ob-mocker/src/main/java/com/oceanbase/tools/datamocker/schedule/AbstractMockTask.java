package com.oceanbase.tools.datamocker.schedule;

import java.util.UUID;
import java.util.concurrent.Callable;

import com.oceanbase.tools.datamocker.core.task.AbstractCallBack;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.core.task.TableTaskMetaData;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * Abstract operation task, used to perform some cleaning or initialization operations before the
 * start and end of the mock task
 *
 * @author yh263208
 * @date 2021-01-13 17:25
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public abstract class AbstractMockTask implements Callable<Void> {
    /**
     * Timestamp when the task was started
     */
    private final long startTimeStamp;
    /**
     * Callback method
     */
    private AbstractCallBack<TableTaskContext> callBack;
    /**
     * Unique ID of the task
     */
    private final String taskId = UUID.randomUUID().toString();
    /**
     * Table generation task configuration object
     */
    private final TableTaskMetaData metaData;
    /**
     * Table generation task context
     */
    private final TableTaskContext context;

    public AbstractMockTask(TableTaskMetaData metaData, TableTaskContext context) {
        this.metaData = metaData;
        this.startTimeStamp = System.currentTimeMillis();
        this.context = context;
    }

    /**
     * Task execution method, in which the user’s business logic is written
     *
     * @param metaData Table task configuration object
     * @throws Exception Mainly to be compatible with exceptions thrown by the Call method
     */
    public abstract void execute(TableTaskMetaData metaData, TableTaskContext context) throws Throwable;

    @Override
    public Void call() {
        try {
            MDC.put("mocktask.workspace", metaData.getTaskId());
            execute(metaData, context);
            if (this.callBack != null) {
                try {
                    this.callBack.onSuccess(context);
                } catch (Throwable e) {
                    boolean shutdownResult = context.terminate();
                    context.setStatus(MockTaskStatus.FAILED);
                    log.error(
                            "Some errors happened when onSuccess call back method executed, context has been shutdown. shutdownResult={},"
                                    + "status={}",
                            shutdownResult, "FAILED", e);
                }
            }
            return null;
        } catch (Throwable e) {
            log.error("Fail to execute mock data task", e);
            Throwable exception = e;
            while (true) {
                if (exception instanceof InterruptedException) {
                    break;
                }
                exception = exception.getCause();
                if (exception == null) {
                    break;
                }
            }
            if (exception != null) {
                if (MockTaskStatus.CANCELED.equals(context.getStatus())) {
                    /**
                     * data mock task has been interrupted
                     */
                    log.warn("Mock data task is interrupted, duration={}ms,status={}", interval(), context.getStatus());
                } else {
                    context.setStatus(MockTaskStatus.FAILED);
                }
            } else {
                if (!MockTaskStatus.CANCELED.equals(context.getStatus())) {
                    context.setStatus(MockTaskStatus.FAILED);
                }
            }
            context.terminate();
            if (this.callBack != null) {
                MockTaskStatus finalStatus = context.getStatus();
                try {
                    this.callBack.onFailure(context, e);
                } catch (Throwable e1) {
                    log.error("Some errors happend when execute onFailure call back method. status={}", finalStatus, e);
                }
                context.setStatus(finalStatus);
            }
        }
        return null;
    }

    /**
     * Bind back function
     *
     * @param callBack Return function to be bound
     */
    public void bind(AbstractCallBack<TableTaskContext> callBack) {
        this.callBack = callBack;
    }

    /**
     * Get the time that the current task has been executed
     *
     * @return Returns the number of milliseconds
     */
    protected long interval() {
        return System.currentTimeMillis() - this.startTimeStamp;
    }

    /**
     * Returns the start timestamp of the task
     *
     * @return Return to start time
     */
    protected long startTime() {
        return this.startTimeStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractMockTask that = (AbstractMockTask) o;
        return this.taskId == that.taskId;
    }

    @Override
    public int hashCode() {
        return this.taskId.hashCode();
    }
}
