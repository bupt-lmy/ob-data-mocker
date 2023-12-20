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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import com.oceanbase.tools.datamocker.core.write.SqlScriptOutput;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

/**
 * The context of mock data is also the handle of the task of operating mock data
 *
 * @author yh263208
 * @date 2021-01-18 11:14
 * @since OBMOCKER_snapshot_0.1.0
 */
@Getter
public class TableTaskContext {

    private final Long batchSize;
    private final Long totalCount;
    /**
     * Table structure definition, used to describe the structure of the table, including the mapping
     * relationship between field names and types
     */
    @Getter
    private final Map<String, AbstractDataType<?, ? extends Comparable<?>>> tableSchema;
    private final String tableName;
    private final String schema;
    private final Boolean truncate;
    private final Long timeoutMilliseconds;
    /**
     * Handle collection, used to control thread tasks
     */
    @Getter(AccessLevel.NONE)
    private final List<Future<?>> handlers;
    private final Map<String, Long> threadName2WriteCount;
    private final Map<String, Long> threadName2GenerateCount;
    private final int topIndex;
    private final SqlScriptOutput output;
    private final List<AbstractMockTask> tableTasks;
    private volatile boolean shutdown;
    private volatile MockTaskStatus status;

    public TableTaskContext(@NonNull TableTaskInfo taskInfo, int index) {
        TableTaskMetaData metaData = taskInfo.getMetaData();
        this.output = taskInfo.getOutput();
        this.topIndex = index;
        this.batchSize = metaData.getBatchSize();
        this.totalCount = metaData.getTotalCount();
        this.tableSchema = metaData.getTableSchema();
        this.tableName = metaData.getTableName();
        this.schema = metaData.getSchema();
        this.truncate = metaData.getShouldTruncate();
        this.timeoutMilliseconds = metaData.getTimeoutMillis();
        this.status = MockTaskStatus.CREATED;
        this.handlers = new ArrayList<>();
        this.tableTasks = new ArrayList<>();
        this.threadName2WriteCount = new ConcurrentHashMap<>();
        this.threadName2GenerateCount = new ConcurrentHashMap<>();
    }

    public void appendHandle(Future<?> handle) {
        if (handle == null) {
            return;
        }
        if (MockTaskStatus.RUNNING.equals(this.status)) {
            this.handlers.add(handle);
        }
    }

    public Long getTotalGenerateCount() {
        OptionalDouble optional = this.threadName2GenerateCount.values().stream().mapToLong(v -> v).average();
        if (!optional.isPresent()) {
            return 0L;
        }
        return Double.valueOf(optional.getAsDouble()).longValue();
    }

    public Long getTotalWriteCount() {
        return this.threadName2WriteCount.values().stream().mapToLong(v -> v).sum();
    }

    public Long getTotalWriteCountByCurrentThread() {
        Long value = this.threadName2WriteCount.get(Thread.currentThread().getName());
        return value == null ? 0L : value;
    }

    public void appendHandle(@NonNull List<AbstractMockTask> tableTasks) {
        this.tableTasks.addAll(tableTasks);
    }

    public void appendHandle(@NonNull AbstractMockTask tableTask) {
        this.tableTasks.add(tableTask);
    }

    public boolean shutdown() {
        this.status = MockTaskStatus.CANCELED;
        return terminate();
    }

    public synchronized boolean terminate() {
        shutdown = true;
        boolean returnVal = Boolean.TRUE;
        this.tableTasks.forEach(AbstractMockTask::cancel);
        for (Future<?> task : this.handlers) {
            if (!task.isCancelled() && !task.isDone()) {
                returnVal &= task.cancel(true);
            }
        }
        return returnVal;
    }

    public long accumulateGenerateCountAndGet(long count) {
        String threadName = Thread.currentThread().getName();
        Long value = this.threadName2GenerateCount.get(threadName);
        if (value == null) {
            value = count;
        } else {
            value += count;
        }
        this.threadName2GenerateCount.put(threadName, value);
        return this.threadName2GenerateCount.values().stream().mapToLong(v -> v).sum();
    }

    public long accumulateWriteCountAndGet(long count) {
        String threadName = Thread.currentThread().getName();
        Long value = this.threadName2WriteCount.get(threadName);
        if (value == null) {
            value = count;
        } else {
            value += count;
        }
        this.threadName2WriteCount.put(threadName, value);
        return this.threadName2WriteCount.values().stream().mapToLong(v -> v).sum();
    }

    public synchronized void setStatus(MockTaskStatus status) {
        this.status = status;
    }

    public double getProgress() {
        if (totalCount != 0 && this.threadName2GenerateCount.size() != 0) {
            Collection<Long> values = this.threadName2GenerateCount.values();
            Iterator<Long> iter = values.iterator();
            double totalProgress = 0.0;
            while (iter.hasNext()) {
                Long value = iter.next();
                totalProgress += value.doubleValue() / totalCount;
            }
            return totalProgress / this.threadName2GenerateCount.size();
        }
        return 0.0;
    }

}
