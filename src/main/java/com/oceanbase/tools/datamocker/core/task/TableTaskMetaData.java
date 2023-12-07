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

import java.util.Map;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.config.MockTableConfig;
import lombok.Getter;
import lombok.NonNull;

/**
 * Table generation task metadata information
 *
 * @author yh263208
 * @date 2021-01-13 17:35
 * @since OBMOCKER_snapshot_0.1.0
 */
@Getter
public class TableTaskMetaData {

    private final Long totalCount;
    /**
     * Table structure definition, used to describe the structure of the table, including the mapping
     * relationship between field names and types key：Column name value：Data type corresponding to
     * column name
     */
    private final Map<String, AbstractDataType<?, ? extends Comparable<?>>> tableSchema;
    private final String tableName;
    private final String schema;
    private final Boolean shouldTruncate;
    private final Long timeoutMillis;
    private final Long batchSize;
    private final String logDir;
    private final int maxErrors;
    private final Integer concurrent;

    public TableTaskMetaData(@NonNull Map<String, AbstractDataType<?, ? extends Comparable<?>>> tableSchema,
            @NonNull MockTableConfig tableConfig, @NonNull String logDir) {
        this.tableSchema = tableSchema;
        this.tableName = tableConfig.getTableName();
        this.schema = tableConfig.getSchemaName();
        this.shouldTruncate = tableConfig.getWhetherTruncate();
        this.timeoutMillis = tableConfig.getTimeoutMillis();
        this.batchSize = tableConfig.getMaxBatchSize();
        this.totalCount = tableConfig.getTotalCount();
        this.logDir = logDir;
        this.maxErrors = tableConfig.getMaxErrors();
        this.concurrent = tableConfig.getConcurrent();
    }

}
