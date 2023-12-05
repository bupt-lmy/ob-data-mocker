/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import com.oceanbase.tools.datamocker.schedule.impl.GenerateDataTask;
import com.oceanbase.tools.datamocker.schedule.impl.MockDataAfterTask;
import com.oceanbase.tools.datamocker.schedule.impl.MockDataBeforeTask;
import com.oceanbase.tools.datamocker.schedule.impl.OutputDataTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.Validate;

/**
 * Task encapsulation object of mock data
 *
 * @author yh263208
 * @date 2021-01-17 22:52
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class TableTask {
    /**
     * Tasks that need to be executed before all tasks are executed, usually used to initialize the test
     * environment
     */
    @Getter
    private final AbstractMockTask beforeTask;
    /**
     * Tasks that need to be executed after all tasks are executed, usually used to clean up the
     * environment
     */
    private final AbstractMockTask afterTask;
    /**
     * List of Business Tasks
     */
    private final List<AbstractMockTask> businessTasks;
    /**
     * table task id
     */
    @Getter
    private final String tableTaskId;
    @Getter
    private final TableTaskContext context;
    /**
     * Counter, used to calibrate the number of tasks currently executed
     */
    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * Constructor for TableTask
     *
     * @param taskBean Bean package object of table generation task
     * @param columnGroups Column generation primitive grouping information
     * @param dataGroups Data write out primitive grouping information
     * @param index Used to indicate which task queue of the TaskBean data, the queue index of the task
     *        queue in the Dispatcher
     */
    public TableTask(TableTaskInfo taskBean, Set<Set<String>> columnGroups, Map<Set<String>, Integer> dataGroups,
            String taskName, int index) {
        this.tableTaskId = taskBean.getMetaData().getTableTaskId();
        this.context = new TableTaskContext(taskBean, taskName, index);
        this.businessTasks = new ArrayList<>();
        for (Set<String> groupSet : columnGroups) {
            List<ColumnReader<?>> tmpList = new LinkedList<>();
            for (String item : groupSet) {
                for (ColumnReader<?> reader : taskBean.getColumnReaders()) {
                    if (item.equals(reader.groupId())) {
                        tmpList.add(reader);
                    }
                }
            }
            GenerateDataTask genTask = new GenerateDataTask(taskBean.getMetaData(), this.context, taskBean.getBuffer(),
                    tmpList, taskBean.getConstraints());
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
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        "Task size can not be equal to or smaller than zero");
            }
            for (int i = 0; i < entry.getValue(); i++) {
                OutputDataTask outputTask = new OutputDataTask(taskBean.getMetaData(), this.context, writers);
                businessTasks.add(outputTask);
            }
        }
        this.beforeTask = new MockDataBeforeTask(taskBean.getMetaData(), this.context, taskBean.getDataSource());
        this.afterTask = new MockDataAfterTask(taskBean.getMetaData(), this.context, taskBean.getDataSource());

    }

    /**
     * Initialize TableTask
     *
     * @param service Incoming thread pool package object
     * @param callBack Callback function, called after TaskBean is executed
     * @throws MockerException Parameter verification fails and throws an exception
     */
    public void init(MockExecutorService service, AbstractCallBack<TableTaskContext> callBack) {
        Validate.notNull(callBack, "CallBack can not be null for TableTask#init");
        Validate.notNull(service, "ExecutorService can not be null for TableTask#init");
        TableTask thisTaskBean = this;
        beforeTask.bind(new AbstractCallBack<TableTaskContext>() {
            @Override
            public void doOnSuccess(TableTaskContext param) throws Throwable {
                log.info(
                        "The Mock data preparation task has been completed, and the business task has begun to run, taskStatus={}",
                        MockTaskStatus.RUNNING);
                for (AbstractMockTask task : thisTaskBean.businessTasks) {
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
                log.error(
                        "The mock data preparation task fails to execute, and the business task will not be executed, taskStatus={}",
                        param.getStatus(), e);
                callBack.onFailure(param, e);
            }
        });

        for (AbstractMockTask businessTask : businessTasks) {
            if (businessTask instanceof GenerateDataTask) {
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
            } else if (businessTask instanceof OutputDataTask) {
                businessTask.bind(new AbstractCallBack<TableTaskContext>() {
                    @Override
                    public void doOnSuccess(TableTaskContext param) throws Throwable {
                        StringBuilder builder = new StringBuilder();
                        for (Map.Entry<String, Long> item : param.getWriterName2writeCount().entrySet()) {
                            builder.append("{\"")
                                    .append(item.getKey())
                                    .append("\" : ")
                                    .append(item.getValue())
                                    .append("} ");
                        }
                        log.info("Data writing task is completed, taskStatus={}, writingInfo={}", param.getStatus(),
                                builder.toString());
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

    private void startAfterTask(MockExecutorService service, AbstractCallBack<TableTaskContext> callBack)
            throws Throwable {
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
