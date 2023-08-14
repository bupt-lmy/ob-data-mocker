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

import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.core.task.AbstractCallBack;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.datamocker.util.DbObjectNameUtil;
import com.oceanbase.tools.datamocker.util.DigestUtil;
import com.oceanbase.tools.datamocker.util.PrintUtil;
import com.oceanbase.tools.datamocker.util.SqlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.Validate;

/**
 * The database write writer is used to write data directly to the database
 *
 * @author yh263208
 * @date 2021-01-04 11:04
 * @since OBMOCKER_0.1.0_snapshot
 */
@Slf4j
public class JdbcWriter extends AbstractMockWriter {
    /**
     * Get a database connection pool, use the connection pool to get database connections for data
     * writing
     */
    private final DataSource dataSource;
    /**
     * The written target library, if the target library is specified when the connection is
     * established, this value can also be left blank
     */
    private final String database;
    /**
     * The target table to be written, this parameter must be passed, specify the incoming target table
     */
    private final String tableName;
    /**
     * The dialect mode of OB, the default is oracle mode
     */
    private ObModeType dialectType = ObModeType.OB_ORACLE;
    private final String groupId;
    /**
     * The flag bit that marks the existence of the database table
     */
    private volatile boolean ifCheck = false;

    /**
     * The constructor writes a data source, which is required
     *
     * @param dataSource datasource
     * @param dialectType dialect type
     * @param database schema or database name
     * @param tableName table name
     */
    public JdbcWriter(DataSource dataSource, ObModeType dialectType, String database, String tableName) {
        validate(dataSource, dialectType, database, tableName);
        this.dataSource = dataSource;
        this.database = database;
        this.tableName = tableName;
        this.dialectType = dialectType;
        try {
            this.groupId = DigestUtil.getToken(this.dialectType.name() + this.database + this.tableName);
        } catch (NoSuchAlgorithmException e) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, e.getMessage());
        }
    }

    /**
     * The constructor writes a data source, which is required
     *
     * @param dataSource datasource
     * @param dialectType dialect type
     * @param database schema or database name
     * @param tableName table name
     * @param groupId group Id for Database writer
     */
    public JdbcWriter(DataSource dataSource, ObModeType dialectType, String database, String tableName,
            String groupId) {
        validate(dataSource, dialectType, database, tableName);
        this.dataSource = dataSource;
        this.database = database;
        this.tableName = tableName;
        this.dialectType = dialectType;
        Validate.notNull(groupId, "Group id can not be null for JdbcWriter");
        this.groupId = groupId;
    }

    /**
     * Verify that the input to the constructor is legal
     *
     * @param dataSource datasource, can not be null
     * @param database database name or schema name
     * @param tableName table name
     * @param dialectType dialect type
     * @throws MockerException An exception is thrown if verification fails
     */
    private void validate(DataSource dataSource, ObModeType dialectType, String database, String tableName) {
        Validate.notNull(dataSource, "DataSource can not be null for JdbcWriter#validate");
        Validate.notNull(database, "Database can not be null for JdbcWriter#validate");
        Validate.notNull(tableName, "TableName can not be null for JdbcWriter#validate");
        if (!ObModeType.OB_ORACLE.equals(dialectType) && !ObModeType.OB_MYSQL.equals(dialectType)) {
            throw new MockerException(MockerError.INVALID_OB_MODE);
        }
    }

    /**
     * Pre-check method, mainly used to check whether the target table exists, if it does not exist,
     * throw an exception
     *
     * @throws SQLException Throw a table or database does not exist exception
     */
    private void detectExistenceOfTables() throws Throwable {
        String descSql;
        if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
            descSql = String.format("select count(*) from \"%s\".\"%s\"",
                    DbObjectNameUtil.doubleCharToEscape(database, '"'),
                    DbObjectNameUtil.doubleCharToEscape(tableName, '"'));
        } else if (ObModeType.OB_MYSQL.equals(this.dialectType)) {
            descSql =
                    String.format("select count(*) from `%s`.`%s`", DbObjectNameUtil.doubleCharToEscape(database, '`'),
                            DbObjectNameUtil.doubleCharToEscape(tableName, '`'));
        } else {
            throw new MockerException(MockerError.INVALID_OB_MODE);
        }
        SqlUtil.executeQuery(dataSource, descSql, null, new AbstractCallBack<ResultSet>() {
            @Override
            public void doOnSuccess(ResultSet result) {
                ifCheck = true;
            }

            @Override
            public void doOnFailure(ResultSet result, Throwable e) {
                throw new MockerException(e);
            }
        });
    }

    @Override
    protected long doWrite(List<MockRowData> rows) throws Throwable {
        if (!ifCheck) {
            detectExistenceOfTables();
        }
        MockRowData firstRow = rows.get(0);
        Set<String> columnSet = firstRow.columnNames();
        List<String> columnList = new ArrayList<>(columnSet);
        StringBuffer sqlBuffer;
        if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
            sqlBuffer = new StringBuffer(
                    String.format("insert into \"%s\".\"%s\"(", DbObjectNameUtil.doubleCharToEscape(database, '"'),
                            DbObjectNameUtil.doubleCharToEscape(tableName, '"')));
        } else if (ObModeType.OB_MYSQL.equals(this.dialectType)) {
            sqlBuffer = new StringBuffer(
                    String.format("insert into `%s`.`%s`(", DbObjectNameUtil.doubleCharToEscape(database, '`'),
                            DbObjectNameUtil.doubleCharToEscape(tableName, '`')));
        } else {
            throw new MockerException(MockerError.INVALID_OB_MODE);
        }
        int columnLength = columnList.size();
        for (int i = 0; i < columnLength; i++) {
            String columnName = columnList.get(i);
            if (i == columnLength - 1) {
                if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
                    sqlBuffer.append(
                            String.format("\"%s\") values (", DbObjectNameUtil.doubleCharToEscape(columnName, '"')));
                } else if (ObModeType.OB_MYSQL.equals(this.dialectType)) {
                    sqlBuffer.append(
                            String.format("`%s`) values (", DbObjectNameUtil.doubleCharToEscape(columnName, '`')));
                }
                for (int j = 0; j < columnLength; j++) {
                    if (j == columnLength - 1) {
                        sqlBuffer.append("?) ");
                    } else {
                        sqlBuffer.append("?,");
                    }
                }
            } else {
                if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
                    sqlBuffer.append(String.format("\"%s\", ", DbObjectNameUtil.doubleCharToEscape(columnName, '"')));
                } else if (ObModeType.OB_MYSQL.equals(this.dialectType)) {
                    sqlBuffer.append(String.format("`%s`, ", DbObjectNameUtil.doubleCharToEscape(columnName, '`')));
                }
            }
        }
        int rowLength = rows.size();
        Object[][] params = new Object[rows.size()][];
        for (int j = 0; j < rowLength; j++) {
            MockRowData row = rows.get(j);
            Object[] innerParam = new Object[columnLength];
            for (int i = 0; i < columnLength; i++) {
                String columnName = columnList.get(i);
                innerParam[i] = row.getMockColumn(columnName).getJdbcColumnValue();
            }
            params[j] = innerParam;
        }
        List<Long> returnVal = new ArrayList<>();
        long startTimestamp = System.currentTimeMillis();
        SqlUtil.executeBatch(dataSource, sqlBuffer.toString(), params, new AbstractCallBack<int[]>() {
            @Override
            public void doOnSuccess(int[] result) {
                String elapsedTime = PrintUtil.convertToReadableTimeString(System.currentTimeMillis() - startTimestamp,
                        TimeUnit.MILLISECONDS, TimeUnit.MINUTES, TimeUnit.MILLISECONDS);
                if (result != null) {
                    log.info("JDBC writer writes a batch successfully, effectRow={}, elapsedTime={}", result.length,
                            elapsedTime);
                    returnVal.add((long) result.length);
                } else {
                    log.warn("JDBC writer has finished writing, but no data has been written, elapsedTime={}",
                            elapsedTime);
                }
            }

            @Override
            public void doOnFailure(int[] result, Throwable e) throws Throwable {
                log.error("JDBC writer failed to write", e);
                throw e;
            }
        });
        return returnVal.get(0);
    }

    @Override
    public String groupId() {
        return this.groupId;
    }
}
