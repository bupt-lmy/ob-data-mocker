package com.oceanbase.tools.datamocker.schedule;

import java.util.UUID;
import java.util.concurrent.Callable;

import com.oceanbase.tools.datamocker.core.task.CallBackMethod;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.core.task.TableTaskMetaData;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象操作任务，用于在mock任务开始前及结束前进行一些清理或初始化的操作
 *
 * @author yh263208
 * @date 2021-01-13 17:25
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public abstract class AbstractMockTask<T> implements Callable<T> {
    /**
     * 任务启动时的时间戳
     */
    private long startTimeStamp;
    /**
     * 回调函数
     */
    private CallBackMethod<Pair<Boolean, T>> callBack;
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
     * 任务执行状态判断方法
     *
     * @return 判断该任务是否执行成功
     */
    protected abstract boolean isTaskSuccess();

    /**
     * 任务执行方法，在该方法中进行用户的业务逻辑编写
     *
     * @param metaData 表任务配置对象
     * @return 返回任务执行所要返回的结果
     * @throws Exception 主要为了兼容Call方法抛出的异常
     */
    public abstract T execute(TableTaskMetaData metaData, TableTaskContext context);

    @Override
    public T call() {
        try {
            T result = execute(this.metaData, context);
            if (this.callBack != null) {
                this.callBack.execute(new Pair<>(isTaskSuccess(), result));
            }
            return result;
        } catch (Exception e) {
            log.error("some errors happened when mock task executed", e);
            if (this.callBack != null) {
                this.callBack.execute(null);
            }
        }
        return null;
    }

    /**
     * 绑定回掉函数
     *
     * @param callBack 要绑定的回掉函数
     */
    public void bind(CallBackMethod callBack) {
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
        AbstractMockTask<?> that = (AbstractMockTask<?>) o;
        return this.taskId == that.taskId;
    }

    @Override
    public int hashCode() {
        return this.taskId.hashCode();
    }
}
