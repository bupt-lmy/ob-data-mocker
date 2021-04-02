package com.oceanbase.tools.datamocker.schedule;

import java.util.LinkedList;
import java.util.List;

import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.Getter;

public class MockContext {
    @Getter
    private String taskName;
    @Getter
    private List<TableTaskContext> tables;
    private final MockExecutorService service;
    private double progress;

    public MockContext(MockExecutorService service) {
        tables = new LinkedList<>();
        if (service == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "thread pool for schedule context can not be null");
        }
        this.service = service;
    }

    protected void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    /**
     * 追加一个mock数据上下文对象
     *
     * @param context 上下文对象
     */
    protected void appendContext(TableTaskContext context) {
        if (context == null) {
            return;
        }
        synchronized (this.tables) {
            this.tables.add(context);
        }
    }

    /**
     * 删除一个上下文对象
     *
     * @param taskId 传入一个子任务id
     */
    protected void removeContext(String taskId) {
        if (taskId == null) {
            return;
        }
        synchronized (this.tables) {
            int length = this.tables.size();
            for (int i = 0; i < length; i++) {
                if (taskId.equals(this.tables.get(i).getTaskId())) {
                    this.tables.remove(i);
                    length--;
                }
            }
        }
    }

    /**
     * 关闭mock数据调度器对象
     *
     * @return 返回关闭结果
     */
    public Boolean shutdown() {
        this.service.shutdown();
        Boolean returnVal = true;
        for (TableTaskContext context : this.tables) {
            returnVal &= context.shutdown();
        }
        return returnVal;
    }

    /**
     * get task progreee
     *
     * @return progress
     */
    public double getProgress() {
        return progress;
    }
}
