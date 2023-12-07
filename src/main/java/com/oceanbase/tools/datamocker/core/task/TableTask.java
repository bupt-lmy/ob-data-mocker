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

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.oceanbase.tools.datamocker.core.write.DataWriter;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.schedule.MockExecutorService;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Task encapsulation object of mock data
 *
 * @author yh263208
 * @date 2021-01-17 22:52
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class TableTask {
    @Getter
    private final TableTaskContext context;
    @Getter
    private final AbstractMockTask beforeTask;
    private final AbstractMockTask afterTask;
    private final List<AbstractMockTask> businessTasks;
    private final AtomicInteger counter = new AtomicInteger(0);

    public TableTask(@NonNull TableTaskInfo tableTaskInfo,
            @NonNull Set<Set<String>> columnGroups, int index) {
        this.context = new TableTaskContext(tableTaskInfo, index);
        DataWriter dataWriter = new ConcurrentDataWriter(columnGroups.size(), tableTaskInfo.getDataWriters());
        this.businessTasks = columnGroups.stream().map(gs -> new MockDataTask(
                tableTaskInfo.getMetaData(), context, dataWriter, tableTaskInfo.getColumnReaders().stream()
                        .filter(r -> gs.contains(r.groupId())).collect(Collectors.toList()),
                tableTaskInfo.getConstraints())).collect(Collectors.toList());
        this.beforeTask = new MockDataBeforeTask(tableTaskInfo.getMetaData(), this.context,
                tableTaskInfo.getSqlBuilderSupplier().get(), tableTaskInfo.getDataSourceFactory());
        this.afterTask = new MockDataAfterTask(tableTaskInfo.getMetaData(), this.context);
        this.context.appendHandle(this.beforeTask);
        this.context.appendHandle(this.businessTasks);
        this.context.appendHandle(this.afterTask);
    }

    public void init(@NonNull MockExecutorService service, @NonNull AbstractCallBack<TableTaskContext> callBack) {
        TableTask that = this;
        this.beforeTask.bind(new AbstractCallBack<TableTaskContext>() {
            @Override
            public void doOnSuccess(TableTaskContext param) throws Throwable {
                if (service.isShutdown() || context.isShutdown()
                        || that.businessTasks.stream().anyMatch(AbstractMockTask::isCancelled)) {
                    log.warn("Mock task has been cancelled, and the mock data task will exit");
                    callBack.onFailure(param, new MockerException("Thread pool has been shutdown"));
                } else {
                    for (AbstractMockTask task : that.businessTasks) {
                        service.submitCallable(task, param);
                    }
                }
            }

            @Override
            public void doOnFailure(TableTaskContext param, Throwable e) throws Throwable {
                callBack.onFailure(param, e);
            }
        });
        for (AbstractMockTask businessTask : this.businessTasks) {
            businessTask.bind(new AbstractCallBack<TableTaskContext>() {
                @Override
                public void doOnSuccess(TableTaskContext param) throws Throwable {
                    startAfterTask(service, callBack);
                }

                @Override
                public void doOnFailure(TableTaskContext param, Throwable e) throws Throwable {
                    startAfterTask(service, callBack);
                }
            });
        }
        this.afterTask.bind(new AbstractCallBack<TableTaskContext>() {
            @Override
            public void doOnSuccess(TableTaskContext param) throws Throwable {
                if (!MockTaskStatus.FAILED.equals(that.getStatus())
                        && !MockTaskStatus.CANCELED.equals(that.getStatus())) {
                    param.setStatus(MockTaskStatus.SUCCESS);
                }
                callBack.onSuccess(param);
            }

            @Override
            public void doOnFailure(TableTaskContext param, Throwable e) throws Throwable {
                callBack.onFailure(param, e);
            }
        });
    }

    private void startAfterTask(MockExecutorService service, AbstractCallBack<TableTaskContext> callBack)
            throws Throwable {
        if (counter.incrementAndGet() == this.businessTasks.size()) {
            if (!service.isShutdown() && !context.isShutdown()
                    && this.businessTasks.stream().noneMatch(AbstractMockTask::isCancelled)) {
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

}
