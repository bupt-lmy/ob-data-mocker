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
package com.oceanbase.tools.datamocker.schedule.impl;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.core.task.AbstractCallBack;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.core.task.TableTaskMetaData;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.schedule.AbstractMockTask;
import com.oceanbase.tools.datamocker.util.DbObjectNameUtil;
import com.oceanbase.tools.datamocker.util.SqlUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

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

    private final DataSource dataSource;

    public MockDataBeforeTask(TableTaskMetaData metaData, TableTaskContext context, @NonNull DataSource dataSource) {
        super(metaData, context);
        this.dataSource = dataSource;
    }

    @Override
    public void execute(TableTaskMetaData metaData, TableTaskContext context) throws Throwable {
        log.info("Start the mock data preparation task");
        // If the empty table is set, the constraint needs to be reloaded
        if (Boolean.TRUE.equals(metaData.getShouldTruncate())) {
            String sql;
            if (ObModeType.OB_MYSQL.equals(metaData.getDialectType())) {
                sql = String.format("delete from `%s`.`%s` where 1=1; ",
                        DbObjectNameUtil.doubleCharToEscape(metaData.getSchema(), '`'),
                        DbObjectNameUtil.doubleCharToEscape(metaData.getTableName(), '`'));
            } else if (ObModeType.OB_ORACLE.equals(metaData.getDialectType())) {
                sql = String.format("delete from \"%s\".\"%s\" where 1=1; ",
                        DbObjectNameUtil.doubleCharToEscape(metaData.getSchema(), '"'),
                        DbObjectNameUtil.doubleCharToEscape(metaData.getTableName(), '"'));
            } else {
                MockerException e = new MockerException(MockerError.INVALID_OB_MODE);
                log.error("Fail to execute mock data preparation task because the ObModeType is illegal, obModeType={}",
                        metaData.getDialectType(), e);
                throw e;
            }
            SqlUtil.executeUpdate(this.dataSource, sql, null, new AbstractCallBack<Integer>() {
                @Override
                public void doOnSuccess(Integer effectRow) {
                    log.info("Truncate table successfully, schema={}, tableName={}, effectRow={}", metaData.getSchema(),
                            metaData.getTableName(), effectRow);
                }

                @Override
                public void doOnFailure(Integer effectRow, Throwable e) throws Throwable {
                    log.error("Fail to execute mock data preparation task", e);
                    throw e;
                }
            });
        }
    }

}
