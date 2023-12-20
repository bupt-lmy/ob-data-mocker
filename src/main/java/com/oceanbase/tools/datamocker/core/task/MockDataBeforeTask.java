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

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.core.DataSourceFactory;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The preparation logic before the start of the mock data business logic, here is mainly the
 * emptying of the table and the reloading logic of the constraints
 *
 * @author yh263208
 * @date 20210-01-13 22:37
 * @since OBMOCKER_0.1.0_snapshot
 */
@Slf4j
public class MockDataBeforeTask extends AbstractMockTask {

    private final SqlBuilder sqlBuilder;
    private final DataSourceFactory factory;

    public MockDataBeforeTask(TableTaskMetaData metaData, TableTaskContext context,
            @NonNull SqlBuilder sqlBuilder, @NonNull DataSourceFactory dataSourceFactory) {
        super(metaData, context);
        this.sqlBuilder = sqlBuilder;
        this.factory = dataSourceFactory;
    }

    @Override
    public void execute(TableTaskMetaData metaData, TableTaskContext context) throws Exception {
        log.info("Mock before task is running...");
        if (!Boolean.TRUE.equals(metaData.getShouldTruncate())) {
            log.info("Mock before task is succeed");
            return;
        }
        this.sqlBuilder.append("DELETE FROM ")
                .identifier(metaData.getSchema()).append(".")
                .identifier(metaData.getTableName())
                .append(" WHERE 1=1");
        DataSource dataSource = this.factory.generate();
        try {
            new JdbcTemplate(dataSource).execute(this.sqlBuilder.toString());
        } catch (Exception e) {
            log.warn("Failed to truncate table, sql={}", this.sqlBuilder.toString(), e);
            throw e;
        } finally {
            if (dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
        }
        log.info("Mock before task is succeed");
    }

}
