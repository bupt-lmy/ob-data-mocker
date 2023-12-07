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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.core.DataSourceFactory;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.model.config.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for {@link JdbcWriter}
 *
 * @author yh263208
 * @date 2023-11-28 20:26
 * @since ODC_release_4.2.3
 */
public class JdbcWriterTest extends MockerTestBase {

    private static DataSourceFactory mysqlDataSource;
    private static DataSourceFactory oracleDataSource;
    private final static String tableName = "TEST_EMP";
    private final static List<String> columnList = Arrays.asList("COL1", "COL2", "COL3");

    @BeforeClass
    public static void initEnv() throws Exception {
        mysqlDataSource = new DataSourceFactory(getDBConfig(ObModeType.OB_MYSQL));
        oracleDataSource = new DataSourceFactory(getDBConfig(ObModeType.OB_ORACLE));
        DataSource ods = oracleDataSource.generate();
        try (Connection connection = ods.getConnection()) {
            initEnv(connection);
        } finally {
            if (ods instanceof AutoCloseable) {
                ((AutoCloseable) ods).close();
            }
        }
        DataSource mds = mysqlDataSource.generate();
        try (Connection connection = mds.getConnection()) {
            initEnv(connection);
        } finally {
            if (mds instanceof AutoCloseable) {
                ((AutoCloseable) mds).close();
            }
        }
    }

    @Test
    public void write_mysql_insertSucceed() throws IOException, SQLException {
        DataBaseConfig config = getDBConfig(ObModeType.OB_MYSQL);
        DataWriter dataWriter =
                new JdbcWriter(mysqlDataSource, MySQLSqlBuilder::new, 1, config.getDefaultSchame(), tableName);
        List<MockRowData> rows = getRows();
        Assert.assertEquals(rows.size(), dataWriter.write(rows));
    }

    @Test
    public void write_oracle_insertSucceed() throws Throwable {
        DataBaseConfig config = getDBConfig(ObModeType.OB_ORACLE);
        DataWriter dataWriter =
                new JdbcWriter(oracleDataSource, OracleSqlBuilder::new, 1, config.getDefaultSchame(), tableName);
        List<MockRowData> rows = getRows();
        Assert.assertEquals(rows.size(), dataWriter.write(rows));
    }

    @AfterClass
    public static void closeEnv() throws Exception {
        DataSource ods = oracleDataSource.generate();
        try (Connection connection = ods.getConnection()) {
            closeEnv(connection);
        } finally {
            if (ods instanceof AutoCloseable) {
                ((AutoCloseable) ods).close();
            }
        }
        DataSource mds = mysqlDataSource.generate();
        try (Connection connection = mds.getConnection()) {
            closeEnv(connection);
        } finally {
            if (mds instanceof AutoCloseable) {
                ((AutoCloseable) mds).close();
            }
        }
    }

    private static DataBaseConfig getDBConfig(ObModeType dialectType) {
        return dialectType == ObModeType.OB_MYSQL ? getMySqlConfig() : getOracleConfig();
    }

    private static void initEnv(Connection connection) throws SQLException {
        String sql = String.format(
                "create table %s (%s varchar(20) not null, %s varchar(20) not null, %s varchar(20) not null)",
                tableName, columnList.get(0), columnList.get(1), columnList.get(2));
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private List<MockRowData> getRows() {
        List<MockRowData> list = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            MockRowData mockRowData = new MockRowData(24);
            for (String column : columnList) {
                mockRowData.addMockColumn(new MockColumnData<>(column, new OracleNumberType(8, 5, null, false),
                        new BigDecimal(new Random().nextInt(1000))));
            }
            list.add(mockRowData);
        }
        return list;
    }

    private static void closeEnv(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(String.format("drop table %s", tableName))) {
            statement.executeUpdate();
        }
    }

}
