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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.Validate;

/**
 * The context of mock data is also the handle of the task of operating mock data
 *
 * @author yh263208
 * @date 2021-01-18 11:14
 * @since OBMOCKER_snapshot_0.1.0
 */
public class TableTaskContext {
    /**
     * Task name
     */
    @Getter
    private final String taskName;
    /**
     * Table task ID
     */
    @Getter
    private final String tableTaskId;
    /**
     * Batch size
     */
    @Getter
    private final Long batchSize;
    /**
     * The current task status of mock data
     */
    @Getter
    private volatile MockTaskStatus status;
    /**
     * Total amount of data to be generated
     */
    @Getter
    private final Long totalCount;
    /**
     * Table structure definition, used to describe the structure of the table, including the mapping
     * relationship between field names and types
     */
    @Getter
    private final Map<String, AbstractDataType<?, ? extends Comparable<?>>> tableSchema;
    /**
     * table name
     */
    @Getter
    private final String tableName;
    /**
     * The schema where the table is located
     */
    @Getter
    private final String schema;
    /**
     * Whether to empty the table
     */
    @Getter
    private final Boolean truncate;
    /**
     * overtime time
     */
    @Getter
    private final Long timeoutMilliseconds;
    /**
     * Handle collection, used to control thread tasks
     */
    private final List<Future<?>> handlers;
    /**
     * Data write statistics. The Key here represents the names of different output sources: for
     * example, the name of the output source for writing DB and the name of the output source for
     * writing files. Value here represents the amount of data written by the output source.
     */
    @Getter
    private final Map<String, Long> writerName2writeCount;
    /**
     * Data generation statistics
     */
    @Getter
    private Long totalDataGenerateCount = null;
    /**
     * The number of records in the current database table
     */
    @Getter
    @Setter
    private Long currentRecordNum;
    /**
     * data source
     */
    @Getter
    private final DataSource dataSource;
    /**
     * file manager
     */
    @Getter
    private final List<MockerFile> fileManagers;
    @Getter
    private final ObModeType dialectType;
    /**
     * Top pointer index
     */
    @Getter
    private final int topIndex;
    @Getter
    private volatile boolean shutdown;

    public TableTaskContext(TableTaskInfo taskInfo, String taskName, int index) {
        Validate.notNull(taskInfo, "TaskInfo can not be null for TableTaskContext");
        TableTaskMetaData metaData = taskInfo.getMetaData();
        this.tableTaskId = metaData.getTableTaskId();
        this.taskName = taskName;
        this.batchSize = metaData.getBatchSize();
        this.totalCount = metaData.getTotalCount();
        this.tableSchema = metaData.getTableSchema();
        this.tableName = metaData.getTableName();
        this.schema = metaData.getSchema();
        this.truncate = metaData.getShouldTruncate();
        this.timeoutMilliseconds = metaData.getTimeoutMilliseconds();
        this.status = MockTaskStatus.CREATED;
        this.handlers = new LinkedList<>();
        this.writerName2writeCount = new HashMap<>();
        this.dataSource = taskInfo.getDataSource();
        this.fileManagers = taskInfo.getFileManagers();
        this.dialectType = metaData.getDialectType();
        this.topIndex = index;
    }

    public void appendHandle(Future<?> handle) {
        if (handle == null) {
            return;
        }
        if (MockTaskStatus.RUNNING.equals(this.status)) {
            this.handlers.add(handle);
        }
    }

    public boolean shutdown() {
        this.status = MockTaskStatus.CANCELED;
        return terminate();
    }

    public synchronized boolean terminate() {
        shutdown = true;
        boolean returnVal = Boolean.TRUE;
        for (Future<?> task : this.handlers) {
            if (!task.isCancelled() && !task.isDone()) {
                returnVal &= task.cancel(true);
            }
        }
        return returnVal;
    }

    /**
     * Add a writer's statistical information
     *
     * @param result statistical results
     */
    public synchronized void appendWriteInfo(Pair<String, Long> result) {
        if (result == null || result.getKey() == null || result.getValue() == null) {
            return;
        }
        Long value = this.writerName2writeCount.getOrDefault(result.getKey(), 0L);
        this.writerName2writeCount.put(result.getKey(), value + result.getValue());
    }

    /**
     * Append a piece of statistical information for data generation primitives
     *
     * @param result statistical results
     * @throws MockerException All data generation primitives must generate the same amount of data. If
     *         violated, an error will be reported
     */
    public synchronized void appendDataGenInfo(Long result) {
        if (result == null) {
            return;
        }
        if (this.totalDataGenerateCount == null) {
            this.totalDataGenerateCount = result;
        } else {
            if (!this.totalDataGenerateCount.equals(result)) {
                throw new MockerException(MockerError.OPERATION_FAILURE,
                        "All column readers have to generate same number of data");
            }
        }
    }

    public synchronized void setStatus(MockTaskStatus status) {
        this.status = status;
    }

    /**
     * get table task progress
     *
     * @return progress of task
     */
    public double getProgress() {
        if (totalCount != 0 && writerName2writeCount.size() != 0) {
            Collection<Long> values = writerName2writeCount.values();
            Iterator<Long> iter = values.iterator();
            double totalProgress = 0.0;
            while (iter.hasNext()) {
                Long value = iter.next();
                totalProgress += value.doubleValue() / totalCount;
            }
            return totalProgress / writerName2writeCount.size();
        }
        return 0.0;
    }
}
