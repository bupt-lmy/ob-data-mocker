package com.oceanbase.tools.datamocker.core.task;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final String tableTaskId;
    @Getter
    private final TableTaskContext context;
    /**
     * 计数器，用于标定当前执行完成的任务数量
     */
    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * taskbean的构造方法
     *
     * @param taskBean     表生成任务的bean封装对象
     * @param columnGroups 列生成原语分组信息
     * @param dataGroups   数据写出原语分组信息
     * @param index        用于表明该TaskBean数据哪一个任务队列，任务队列在Dispatcher中的队列索引
     */
    public TableTask(TableTaskInfo taskBean, Set<Set<String>> columnGroups, Map<Set<String>, Integer> dataGroups, String taskName,
            int index) {
        this.tableTaskId = taskBean.getMetaData().getTableTaskId();
        this.context = new TableTaskContext(taskBean, taskName, index);
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
            MockDataGenTask genTask = new MockDataGenTask(taskBean.getMetaData(), this.context, taskBean.getBuffer(), tmpList,
                    taskBean.getConstraints());
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
                throw new MockerException(MockerError.PARAMETER_ERROR, "Task size can not be equal to or smaller than zero");
            }
            for (int i = 0; i < entry.getValue(); i++) {
                MockDataOutputTask outputTask = new MockDataOutputTask(taskBean.getMetaData(), this.context, writers);
                businessTasks.add(outputTask);
            }
        }
        this.beforeTask = new MockDataBeforeTask(taskBean.getMetaData(), this.context, taskBean.getDataSource());
        this.afterTask = new MockDataAfterTask(taskBean.getMetaData(), this.context, taskBean.getDataSource());

    }

    /**
     * 初始化TaskBean
     *
     * @param service  传入线程池封装对象
     * @param callBack 回掉函数，TaskBean执行完毕后调用
     * @throws MockerException 参数校验不通过抛出异常
     */
    public void init(MockExecutorService service, AbstractCallBack<TableTaskContext> callBack) {
        if (callBack == null || service == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Call back method or executor service can not be null for mock task bean");
        }
        TableTask thisTaskBean = this;
        beforeTask.bind(new AbstractCallBack<TableTaskContext>() {
            @Override
            public void doOnSuccess(TableTaskContext param) throws Throwable {
                log.info("The Mock data preparation task has been completed, and the business task has begun to run, taskStatus={}",
                        MockTaskStatus.RUNNING);
                for (AbstractMockTask task : thisTaskBean.businessTasks) {
                    if (task instanceof MockDataGenTask) {
                        ((MockDataGenTask) task).setConstraints(param.getConstraints());
                    }
                    if (!service.isShutdown() && !context.isShutdown()) {
                        service.submitCallable(task, param);
                    } else {
                        log.warn("The thread pool has been closed, and the mock data task will exit");
                        callBack.onFailure(param, new MockerException("Thread pool has been shutdown"));
                    }
                }
            }

            @Override
            public void doOnFailure(TableTaskContext param, Throwable e) throws Throwable {
                log.error("The mock data preparation task fails to execute, and the business task will not be executed, taskStatus={}",
                        param.getStatus(), e);
                callBack.onFailure(param, e);
            }
        });

        for (AbstractMockTask businessTask : businessTasks) {
            if (businessTask instanceof MockDataGenTask) {
                businessTask.bind(new AbstractCallBack<TableTaskContext>() {
                    @Override
                    public void doOnSuccess(TableTaskContext param) throws Throwable {
                        log.info("Data generation task completed, numberOfDataGeneration={}, taskStatus={}",
                                param.getTotalDataGenerateCount(), param.getStatus());
                        startAfterTask(service, callBack);
                    }

                    @Override
                    public void doOnFailure(TableTaskContext param, Throwable e) throws Throwable {
                        log.error("Data generation task execution failed, taskStatus={}", param.getStatus(), e);
                        startAfterTask(service, callBack);
                    }
                });
            } else if (businessTask instanceof MockDataOutputTask) {
                businessTask.bind(new AbstractCallBack<TableTaskContext>() {
                    @Override
                    public void doOnSuccess(TableTaskContext param) throws Throwable {
                        StringBuilder builder = new StringBuilder();
                        for (Map.Entry<String, Long> item : param.getWriterName2writeCount().entrySet()) {
                            builder.append("{\"" + item.getKey() + "\" : " + item.getValue() + "} ");
                        }
                        log.info("Data writing task is completed, taskStatus={}, writingInfo={}", param.getStatus(), builder.toString());
                        startAfterTask(service, callBack);
                    }

                    @Override
                    public void doOnFailure(TableTaskContext param, Throwable e) throws Throwable {
                        log.error("Fail to execute data writing task, taskStatus={}", param.getStatus(), e);
                        startAfterTask(service, callBack);
                    }
                });
            } else {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Unknown business task type");
            }
        }
        afterTask.bind(new AbstractCallBack<TableTaskContext>() {
            @Override
            public void doOnSuccess(TableTaskContext param) throws Throwable {
                if (!MockTaskStatus.FAILED.equals(thisTaskBean.getStatus()) && !MockTaskStatus.CANCELED.equals(
                        thisTaskBean.getStatus())) {
                    param.setStatus(MockTaskStatus.SUCCESS);
                }
                log.info("The mock data destruction task is executed successfully, currentRecordNum={}, taskStatus={}",
                        param.getCurrentRecordNum(), param.getStatus());
                callBack.onSuccess(param);
            }

            @Override
            public void doOnFailure(TableTaskContext param, Throwable e) throws Throwable {
                log.error("Failed to execute simulation data destruction task, taskStatus={}", param.getStatus(), e);
                callBack.onFailure(param, e);
            }
        });
    }

    /**
     * 启动after任务
     *
     * @param service  线程service对象
     * @param callBack 回调方法
     */
    private void startAfterTask(MockExecutorService service, AbstractCallBack callBack) throws Throwable {
        if (counter.incrementAndGet() == this.businessTasks.size()) {
            log.info("All mock data business tasks are completed, and the destructuring task is started");
            if (!service.isShutdown() && !context.isShutdown()) {
                service.submitCallable(this.afterTask, this.context);
            } else {
                log.warn("The thread pool has been closed, and the mock data task will exit");
                callBack.onFailure(this.context, new MockerException("Thread pool has been shutdown"));
            }
        }
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
        return this.tableTaskId.equals(that.tableTaskId);
    }

    @Override
    public int hashCode() {
        return this.tableTaskId.hashCode();
    }
}
