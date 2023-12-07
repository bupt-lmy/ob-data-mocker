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

package com.oceanbase.tools.datamocker.core.write;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

/**
 * {@link SqlScriptWriter}
 *
 * @author yh263208
 * @date 2023-11-28 20:41
 * @since ODC_release_4.2.3
 */
@Slf4j
public class SqlScriptWriter implements DataWriter {

    private final String tableName;
    private final String schema;
    private final SqlScriptOutput output;
    private final Supplier<SqlBuilder> sqlBuilderSupplier;
    private volatile boolean closed = false;

    public SqlScriptWriter(@NonNull SqlScriptOutput output,
            @NonNull Supplier<SqlBuilder> sqlBuilderSupplier,
            @NonNull String schema, @NonNull String tableName) {
        this.tableName = tableName;
        this.schema = schema;
        this.output = output;
        this.sqlBuilderSupplier = sqlBuilderSupplier;
    }

    @Override
    public long write(List<MockRowData> rows) throws IOException {
        if (this.closed) {
            throw new IllegalStateException("SqlScriptWriter has been closed");
        }
        SqlBuilder prefixBuilder = this.sqlBuilderSupplier.get();
        prefixBuilder.append("INSERT INTO ")
                .identifier(this.schema)
                .append(".").identifier(this.tableName).append(" (");
        List<String> columnList = new ArrayList<>(rows.get(0).columnNames());
        int columnLength = columnList.size();
        for (int i = 0; i < columnLength; i++) {
            prefixBuilder.identifier(columnList.get(i));
            if (i < columnLength - 1) {
                prefixBuilder.append(", ");
            }
        }
        String prefix = prefixBuilder.append(") VALUES (").toString();

        StringBuilder sqlStringBuilder = new StringBuilder();
        rows.forEach(rowData -> {
            SqlBuilder sqlBuilder = sqlBuilderSupplier.get().append(prefix);
            for (int i = 0; i < columnLength; i++) {
                String columnName = columnList.get(i);
                MockColumnData<?> mockColumn = rowData.getMockColumn(columnName);
                String value = mockColumn.getColumnValueString();
                if (value == null) {
                    throw new IllegalStateException(String.format(
                            "Value for column \"%s\" is null", columnName));
                }
                sqlBuilder.append(value);
                if (i == columnLength - 1) {
                    sqlBuilder.append(");");
                } else {
                    sqlBuilder.append(", ");
                }
            }
            sqlStringBuilder.append(sqlBuilder.toString()).append("\n");
        });
        byte[] buffer = sqlStringBuilder.toString().getBytes();
        IOUtils.write(buffer, this.output.getOutputStream());
        return buffer.length;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() throws Exception {
        if (isClosed()) {
            return;
        }
        this.closed = true;
        this.output.close();
        log.info("SqlScriptWriter has been closed");
    }

}
