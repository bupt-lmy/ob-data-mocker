package com.oceanbase.tools.datamocker.core.task;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.core.read.ColumnReader;
import com.oceanbase.tools.datamocker.core.write.AbstractMockWriter;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.schedule.AbstractMockTask;
import com.oceanbase.tools.datamocker.schedule.MockExecutorService;
import com.oceanbase.tools.datamocker.schedule.impl.MockDataAfterTask;
import com.oceanbase.tools.datamocker.schedule.impl.MockDataBeforeTask;
import com.oceanbase.tools.datamocker.schedule.impl.MockDataGenTask;
import com.oceanbase.tools.datamocker.schedule.impl.MockDataOutputTask;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * mock数据的任务封装对象
 *
 * @author yh263208
 * @date 2021-01-17 22:52
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class TableTask {
    /**
     * 在一切任务执行前需要执行的任务，通常用来初始化测试环境
     */
    @Getter
    private AbstractMockTask beforeTask;
    /**
     * 在一切任务执行后需要执行的任务，通常用于清理环境
     */
    private AbstractMockTask afterTask;
    /**
     * 实际的任务
     */
    private List<AbstractMockTask> businessTasks;
    /**
     * 唯一的任务Id
     */
    @Getter
    private final String taskId;
    @Getter
    private final TableTaskContext context;
    /**
     * 计数器，用于标定当前执行完成的任务数量
     */
    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * 构造函数，已过时不要使用
     *
     * @param beforeTask    TaskBean的先遣任务
     * @param afterTask     TaskBean的收尾任务
     * @param businessTasks TaskBean的正常测试任务
     * @param context       mock数据的任务上下文
     */
    @Deprecated
    public TableTask(MockDataBeforeTask beforeTask, MockDataAfterTask afterTask, List<AbstractMockTask> businessTasks,
            TableTaskContext context, String taskId) {
        if (beforeTask == null || afterTask == null || businessTasks == null) {
            throw new RuntimeException("mock before task, after task or business tasks can not be null");
        }
        this.beforeTask = beforeTask;
        this.afterTask = afterTask;
        this.businessTasks = businessTasks;
        this.context = context;
        this.taskId = taskId;
    }

    /**
     * taskbean的构造方法
     *
     * @param taskBean     表生成任务的bean封装对象
     * @param columnGroups 列生成原语分组信息
     * @param dataGroups   数据写出原语分组信息
     * @param index        用于表明该TaskBean数据哪一个任务队列，任务队列在Dispatcher中的队列索引
     */
    public TableTask(TableTaskInfo taskBean, Set<Set<String>> columnGroups, Map<Set<String>, Integer> dataGroups, String taskName,
            String taskId, int index) {
        if (taskId == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "task id can not be null");
        }
        this.taskId = taskId;
        this.businessTasks = new ArrayList<>();
        for (Set<String> groupSet : columnGroups) {
            List<ColumnReader> tmpList = new LinkedList<>();
            for (String item : groupSet) {
                for (ColumnReader reader : taskBean.getColumnReaders()) {
                    if (item.equals(reader.groupId())) {
                        tmpList.add(reader);
                    }
                }
            }
            MockDataGenTask genTask = new MockDataGenTask(taskBean.getMetaData(), taskBean.getBuffer(), tmpList, taskBean.getConstraints());
            businessTasks.add(genTask);
        }
        taskBean.getBuffer().setConcurrent(businessTasks.size());
        Set<Map.Entry<Set<String>, Integer>> entrySet = dataGroups.entrySet();
        for (Map.Entry<Set<String>, Integer> entry : entrySet) {
            Set<String> groupId = entry.getKey();
            List<AbstractMockWriter> writers = new LinkedList<>();
            for (String id : groupId) {
                for (AbstractMockWriter item : taskBean.getDataWriters()) {
                    if (item.groupId().equals(id)) {
                        writers.add(item);
                    }
                }
            }
            if (entry.getValue() <= 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "task size can not be equal to or smaller than zero");
            }
            for (int i = 0; i < entry.getValue(); i++) {
                MockDataOutputTask outputTask = new MockDataOutputTask(taskBean.getMetaData(), writers);
                businessTasks.add(outputTask);
            }
        }
        this.beforeTask = new MockDataBeforeTask(taskBean.getMetaData(), taskBean.getDataSource());
        this.afterTask = new MockDataAfterTask(taskBean.getMetaData(), taskBean.getDataSource());
        TableTaskMetaData metaData = taskBean.getMetaData();
        this.context = new TableTaskContext(this.taskId, taskName, metaData.getBatchSize(), metaData.getTotalCount(),
                metaData.getDialectType(),
                metaData.getTableSchema(), metaData.getTableName(), metaData.getSchema(), metaData.getShouldTruncate(),
                metaData.getTimeout(),
                metaData.getStrategy(), taskBean.getDataSource(), taskBean.getFileManagers(), index);
    }

    /**
     * 初始化TaskBean
     *
     * @param service  传入线程池封装对象
     * @param callBack 回掉函数，TaskBean执行完毕后调用
     * @throws MockerException 参数校验不通过抛出异常
     */
    public void init(MockExecutorService service, CallBackMethod<TableTaskContext> callBack) {
        if (callBack == null || service == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "call back method or executor service can not be null for mock task bean");
        }
        TableTask thisTaskBean = this;
        beforeTask.bind((CallBackMethod<Pair<Boolean, List<AbstractConstraint>>>) result -> {
            if (result.getKey()) {
                log.info("mock before has been executed, task's status is {}, begin to execute business tasks",
                        MockTaskStatus.RUNNING.name());
                for (AbstractMockTask task : this.businessTasks) {
                    if (task instanceof MockDataGenTask) {
                        ((MockDataGenTask) task).setConstraints(result.getValue());
                    }
                    if (!service.isShutdown()) {
                        service.submitCallable(task, this.context);
                    } else {
                        log.warn("thread pool has been shut down, mock task will be exited");
                        callBack.execute(this.context);
                    }
                }
            } else {
                if (!MockTaskStatus.CANCELED.equals(thisTaskBean.getStatus())) {
                    thisTaskBean.setStatus(MockTaskStatus.FAILED);
                }
                log.error("fail to execute mock before task, task status is {}, mock business tasks will not be droped",
                        this.getStatus().name());
                callBack.execute(thisTaskBean.context);
            }
        });
        for (AbstractMockTask businessTask : businessTasks) {
            if (businessTask instanceof MockDataGenTask) {
                businessTask.bind((CallBackMethod<Pair<Boolean, Long>>) result -> {
                    if (result.getKey()) {
                        this.context.appendDataGenInfo(result.getValue());
                        log.info("data generate business task has been executed completely, generate {} items totally, task status is {}",
                                result.getValue(), this.getStatus().name());
                    } else {
                        if (!MockTaskStatus.CANCELED.equals(thisTaskBean.getStatus())) {
                            thisTaskBean.setStatus(MockTaskStatus.FAILED);
                        }
                        log.error("fail to execute data generate business task, task status is {}", this.getStatus().name());
                    }
                    startAfterTask(service, callBack);
                });
            } else if (businessTask instanceof MockDataOutputTask) {
                businessTask.bind((CallBackMethod<Pair<Boolean, Map<String, Long>>>) result -> {
                    Map<String, Long> resultValue = result.getValue();
                    StringBuilder builder = new StringBuilder();
                    for (Map.Entry<String, Long> item : resultValue.entrySet()) {
                        this.context.appendWriteInfo(new Pair<>(item.getKey(), item.getValue()));
                        builder.append("{\"" + item.getKey() + "\" : " + item.getValue() + "} ");
                    }
                    if (result.getKey()) {
                        log.info("data write business task has been executed completely, generate {}items totally, task status is {}",
                                builder.toString(), this.getStatus().name());
                    } else {
                        if (!MockTaskStatus.CANCELED.equals(thisTaskBean.getStatus())) {
                            thisTaskBean.setStatus(MockTaskStatus.FAILED);
                        }
                        log.error("fail to execute data write business task, task status is {}", this.getStatus().name());
                    }
                    startAfterTask(service, callBack);
                });
            } else {
                throw new MockerException(MockerError.PARAMETER_ERROR, "unknown business task type");
            }
        }
        afterTask.bind((CallBackMethod<Pair<Boolean, Long>>) result -> {
            if (result.getKey()) {
                if (!MockTaskStatus.FAILED.equals(thisTaskBean.getStatus()) && !MockTaskStatus.CANCELED.equals(
                        thisTaskBean.getStatus())) {
                    thisTaskBean.setStatus(MockTaskStatus.SUCCESS);
                }
                this.context.setCurrentRecordNum(result.getValue());
                log.info("mock after task has been executed successfully, current record size is {}, task status is {}", result.getValue(),
                        this.getStatus().name());
            } else {
                if (!MockTaskStatus.CANCELED.equals(thisTaskBean.getStatus())) {
                    thisTaskBean.setStatus(MockTaskStatus.FAILED);
                }
                log.error("fail to execute mock after task, task status is {}", this.getStatus().name());
            }
            callBack.execute(this.context);
        });
    }

    /**
     * 启动after任务
     *
     * @param service  线程service对象
     * @param callBack 回调方法
     */
    private void startAfterTask(MockExecutorService service, CallBackMethod callBack) {
        if (counter.incrementAndGet() == this.businessTasks.size()) {
            log.info("all mock business tasks has been executed, mock after task will begin");
            if (!service.isShutdown()) {
                service.submitCallable(this.afterTask, this.context);
            } else {
                log.warn("thread pool has been shut down, mock task will be exited");
                callBack.execute(this.context);
            }
        }
    }

    public void setStatus(MockTaskStatus status) {
        this.context.setStatus(status);
    }

    public MockTaskStatus getStatus() {
        return this.context.getStatus();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TableTask that = (TableTask) o;
        return this.taskId.equals(that.taskId);
    }

    @Override
    public int hashCode() {
        return this.taskId.hashCode();
    }
}
