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
 * 抽象操作任务，用于在mock任务开始前及结束前进行一些清理或初始化的操作
 *
 * @author yh263208
 * @date 2021-01-13 17:25
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public abstract class AbstractMockTask implements Callable<Void> {
    /**
     * 任务启动时的时间戳
     */
    private final long startTimeStamp;
    /**
     * 回调函数
     */
    private AbstractCallBack<TableTaskContext> callBack;
    /**
     * 任务的唯一标识
     */
    private final String taskId = UUID.randomUUID().toString();
    /**
     * 表生成任务配置对象
     */
    private final TableTaskMetaData metaData;
    /**
     * 表生成任务上下文
     */
    private final TableTaskContext context;

    public AbstractMockTask(TableTaskMetaData metaData, TableTaskContext context) {
        this.metaData = metaData;
        this.startTimeStamp = System.currentTimeMillis();
        this.context = context;
    }

    /**
     * 任务执行方法，在该方法中进行用户的业务逻辑编写
     *
     * @param metaData 表任务配置对象
     * @return 返回任务执行所要返回的结果
     * @throws Exception 主要为了兼容Call方法抛出的异常
     */
    public abstract Void execute(TableTaskMetaData metaData, TableTaskContext context) throws Throwable;

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
                            "some errors happened when onSuccess call back method executed, context has been shutdown. shutdownResult={},"
                            + "status={}", shutdownResult, "FAILED", e);
                }
            }
            return null;
        } catch (Throwable e) {
            log.error("some errors happened when mock task executed", e);
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
                    log.warn("data mock business thread has been interrupted. duration={}ms,status={}", interval(), context.getStatus());
                } else {
                    context.setStatus(MockTaskStatus.FAILED);
                }
            } else {
                context.setStatus(MockTaskStatus.FAILED);
            }
            context.terminate();
            if (this.callBack != null) {
                MockTaskStatus finalStatus = context.getStatus();
                try {
                    this.callBack.onFailure(context, e);
                } catch (Throwable e1) {
                    log.error("some errors happend when execute onFailure call back method. status={}", finalStatus, e);
                }
                context.setStatus(finalStatus);
            }
        }
        return null;
    }

    /**
     * 绑定回掉函数
     *
     * @param callBack 要绑定的回掉函数
     */
    public void bind(AbstractCallBack<TableTaskContext> callBack) {
        this.callBack = callBack;
    }

    /**
     * 获取当前任务已经执行的时间
     *
     * @return 返回毫秒数
     */
    protected long interval() {
        return System.currentTimeMillis() - this.startTimeStamp;
    }

    /**
     * 返回该任务的开始时间戳
     *
     * @return 返回开始时间
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
