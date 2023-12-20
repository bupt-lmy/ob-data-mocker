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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.oceanbase.tools.datamocker.constraint.Constraint;
import com.oceanbase.tools.datamocker.core.DataSourceFactory;
import com.oceanbase.tools.datamocker.core.read.ColumnReader;
import com.oceanbase.tools.datamocker.core.write.DataWriter;
import com.oceanbase.tools.datamocker.core.write.SqlScriptOutput;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Table generation task object, used to encapsulate all objects related to a table generation task
 *
 * @author yh263208
 * @date 2021-01-09 19:33
 * @since OBMOCKER_0.1.0_snapshot
 */
@Getter
public class TableTaskInfo {

    private final TableTaskMetaData metaData;
    private final List<DataWriter> dataWriters;
    private final List<ColumnReader<?>> columnReaders;
    private final List<Constraint> constraints;
    private final DataSourceFactory dataSourceFactory;
    private final SqlScriptOutput output;
    private final Supplier<SqlBuilder> sqlBuilderSupplier;

    public TableTaskInfo(@NonNull List<DataWriter> dataWriters,
            @NonNull List<ColumnReader<?>> columnReaders,
            @NonNull DataSourceFactory dataSourceFactory,
            @NonNull List<Constraint> constraints,
            @NonNull TableTaskMetaData metaData,
            @NonNull SqlScriptOutput output,
            @NonNull Supplier<SqlBuilder> sqlBuilderSupplier) {
        this.columnReaders = columnReaders;
        this.constraints = constraints;
        this.metaData = metaData;
        this.dataWriters = dataWriters;
        this.dataSourceFactory = dataSourceFactory;
        this.output = output;
        this.sqlBuilderSupplier = sqlBuilderSupplier;
    }

    public Set<String> columnGroups() {
        return this.columnReaders.stream().map(ColumnReader::groupId).collect(Collectors.toSet());
    }

}
