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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.core.DataSourceFactory;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@link JdbcWriter}
 *
 * @author yh263208
 * @date 2023-11-28 19:51
 * @since ODC_release_4.2.3
 */
@Slf4j
public class JdbcWriter implements DataWriter {

    private final String tableName;
    private final String schema;
    private final DataSource dataSource;
    private final JdbcOperations jdbc;
    private final Supplier<SqlBuilder> sqlBuilderSupplier;
    private final Integer concurrent;
    private final ExecutorService executorService;
    private volatile boolean closed = false;

    public JdbcWriter(@NonNull DataSourceFactory dataSourceFactory,
            @NonNull Supplier<SqlBuilder> sqlBuilderSupplier,
            @NonNull Integer concurrent, @NonNull String schema,
            @NonNull String tableName) throws SQLException {
        this.tableName = tableName;
        this.schema = schema;
        this.dataSource = dataSourceFactory.generate();
        this.jdbc = new JdbcTemplate(this.dataSource);
        this.sqlBuilderSupplier = sqlBuilderSupplier;
        this.concurrent = concurrent;
        this.executorService = getThreadPoolExecutor();
    }

    @Override
    public long write(List<MockRowData> rows) {
        if (this.closed) {
            throw new IllegalStateException("JdbcWriter has been closed");
        }
        SqlBuilder sqlBuilder = this.sqlBuilderSupplier.get().append("INSERT INTO ")
                .identifier(this.schema)
                .append(".").identifier(this.tableName).append(" (");
        List<String> columnList = new ArrayList<>(rows.get(0).columnNames());
        int columnLength = columnList.size();
        for (int i = 0; i < columnLength; i++) {
            sqlBuilder.identifier(columnList.get(i));
            if (i < columnLength - 1) {
                sqlBuilder.append(", ");
            }
        }
        sqlBuilder.append(") VALUES (");
        for (int i = 0; i < columnLength; i++) {
            sqlBuilder.append("?");
            if (i < columnLength - 1) {
                sqlBuilder.append(", ");
            }
        }
        String sql = sqlBuilder.append(")").toString();
        int size = rows.size() / this.concurrent;
        if (rows.size() % this.concurrent != 0) {
            size += 1;
        }
        List<List<MockRowData>> lists = ListUtils.partition(rows, size);
        CompletionService<Integer> completionService = new ExecutorCompletionService<>(this.executorService);
        for (int i = 1; i < lists.size(); i++) {
            List<MockRowData> mockRowData = lists.get(i);
            completionService.submit(() -> doWrite(sql, columnList, mockRowData));
        }
        Integer totalAffectRows = 0;
        try {
            totalAffectRows += doWrite(sql, columnList, lists.get(0));
        } catch (Exception e) {
            log.warn("Failed to write jdbc, message={}", e.getMessage());
            throw new IllegalStateException(e);
        }
        for (int i = 1; i < lists.size(); i++) {
            try {
                totalAffectRows += completionService.take().get();
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to write jdbc, message={}", e.getMessage());
                throw new IllegalStateException(e);
            }
        }
        return totalAffectRows.longValue();
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
        try {
            this.executorService.shutdown();
        } catch (Exception e) {
            // eat exception
        }
        if (this.dataSource instanceof AutoCloseable) {
            ((AutoCloseable) this.dataSource).close();
        }
        log.info("JdbcWriter has been closed, concurrent={}", this.concurrent);
    }

    private ThreadPoolExecutor getThreadPoolExecutor() {
        int corePoolSize = Math.max(Runtime.getRuntime().availableProcessors(), 5);
        return new ThreadPoolExecutor(corePoolSize, corePoolSize, 0,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new BasicThreadFactory.Builder().namingPattern("ob-data-mocker-writer-thread-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private int doWrite(String sql, List<String> columnList, List<MockRowData> mockRowData) {
        int columnLength = columnList.size();
        int[] affectRows = jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public int getBatchSize() {
                return mockRowData.size();
            }

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                MockRowData row = mockRowData.get(i);
                for (int j = 0; j < columnLength; j++) {
                    ps.setObject(j + 1, row.getMockColumn(columnList.get(j)).getJdbcColumnValue());
                }
            }
        });
        return Arrays.stream(affectRows).map(value -> {
            switch (value) {
                case Statement.EXECUTE_FAILED:
                    throw new IllegalStateException("Failed to execute a batch");
                case Statement.SUCCESS_NO_INFO:
                    return 1;
                default:
                    return value;
            }
        }).sum();
    }

}
