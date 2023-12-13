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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.core.DataSourceFactory;
import com.oceanbase.tools.datamocker.core.write.DataWriter;
import com.oceanbase.tools.datamocker.core.write.SqlScriptOutput;
import com.oceanbase.tools.datamocker.core.write.SqlScriptWriter;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.model.config.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for {@link ConcurrentDataWriter}
 *
 * @author yh263208
 * @date 2023-11-28 21:45
 * @since ODC_release_4.2.3
 */
public class ConcurrentDataWriterTest extends MockerTestBase {

    private final static String DEST_DIR = "test/mock";
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private static DataSource dataSource;
    private final static String tableName = "TEST_EMP";

    @BeforeClass
    public static void initEnv() throws SQLException {
        dataSource = new DataSourceFactory(getDBConfig()).generate();
        try (Connection connection = dataSource.getConnection()) {
            initEnv(connection);
        }
    }

    @Before
    public void setUp() throws IOException {
        FileUtils.forceMkdir(new File(DEST_DIR));
    }

    @After
    public void clear() throws IOException {
        FileUtils.deleteDirectory(new File(DEST_DIR));
    }

    @Test
    public void write_2ThreadWrite_writeSucceed() throws ExecutionException, InterruptedException, IOException {
        List<DataWriter> dataWriters = new ArrayList<>();
        dataWriters.add(new SqlScriptWriter(getOutput(), 0L, OracleSqlBuilder::new, "test", "emp"));
        ConcurrentDataWriter dataWriter = new ConcurrentDataWriter(2, dataWriters);
        Future<Long> f1 = executor.submit(() -> dataWriter.write(getRows(Arrays.asList("COL1", "COL2"))));
        Future<Long> f2 = executor.submit(() -> dataWriter.write(getRows(Collections.singletonList("COL3"))));
        long actual = f1.get() + f2.get();
        FileInputStream inputStream = new FileInputStream(new File(DEST_DIR).listFiles()[0]);
        Assert.assertEquals(inputStream.available(), actual);
    }

    @Test(expected = ExecutionException.class)
    public void write_dupCol_writeSucceed() throws ExecutionException, InterruptedException, IOException {
        List<DataWriter> dataWriters = new ArrayList<>();
        dataWriters.add(new SqlScriptWriter(getOutput(), 0L, OracleSqlBuilder::new, "test", "emp"));
        ConcurrentDataWriter dataWriter = new ConcurrentDataWriter(2, dataWriters);
        Future<Long> f1 = executor.submit(() -> dataWriter.write(getRows(Arrays.asList("COL1", "COL2"))));
        Future<Long> f2 = executor.submit(() -> dataWriter.write(getRows(Collections.singletonList("COL2"))));
        f1.get();
        f2.get();
    }

    @AfterClass
    public static void closeEnv() throws SQLException {
        closeEnv(dataSource.getConnection());
    }

    private static DataBaseConfig getDBConfig() {
        return getOracleConfig();
    }

    private SqlScriptOutput getOutput() throws IOException {
        return new SqlScriptOutput(new File(DEST_DIR), "emp", Long.MAX_VALUE);
    }

    private static void initEnv(Connection connection) throws SQLException {
        String sql = String.format("create table %s (COL1 varchar(20) not null, "
                + "COL2 varchar(20) not null, COL3 varchar(20) not null)", tableName);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private List<MockRowData> getRows(List<String> columnList) {
        List<MockRowData> list = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            MockRowData mockRowData = new MockRowData(24);
            for (String column : columnList) {
                mockRowData.addMockColumn(
                        new MockColumnData<>(column, new OracleNumberType(8, 5, null, false),
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
