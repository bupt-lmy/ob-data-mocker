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
package com.oceanbase.tools.datamocker.task;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.ObDataMocker;
import com.oceanbase.tools.datamocker.ObMockerFactory;
import com.oceanbase.tools.datamocker.core.task.AbstractMockerFactory;
import com.oceanbase.tools.datamocker.core.task.DataSourceFactory;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.model.config.AbstractTableConfig;
import com.oceanbase.tools.datamocker.model.config.AbstractTaskConfig;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultTaskConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import com.oceanbase.tools.datamocker.model.enums.ScriptType;
import com.oceanbase.tools.datamocker.schedule.MockContext;
import com.oceanbase.tools.datamocker.util.PrintUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Mock data task test class, used to test the task module of mock data
 *
 * @author yh263208
 * @date 2021-01-17 16:14
 * @since OBMOCKER_0.1.0_snapshot
 */
public class MockerTaskOracleTest extends MockerTestBase {
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 5, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
    private final String configFile = "task/config-oracle.json";
    private final String[] ddls = new String[] {
            "CREATE TABLE \"EMP\" (\n"
                    + "  \"COL\" NUMBER(5,2) NOT NULL,\n"
                    + "  \"COL2\" VARCHAR(64) NOT NULL,\n"
                    + "  \"COL3\" VARCHAR2(128) NOT NULL,\n"
                    + "  \"COL4\" CHAR(128) NOT NULL,\n"
                    + "  \"COL5\" NVARCHAR2(128) NOT NULL,\n"
                    + "  \"COL6\" date,\n"
                    + "  \"COL7\" interval year(5) to month,\n"
                    + "  \"COL8\" interval day(2) to second(6),\n"
                    + "  PRIMARY KEY (\"COL\", \"COL4\"),\n"
                    + "  UNIQUE (\"COL2\", \"COL3\"),\n"
                    + "  UNIQUE (\"COL5\")\n"
                    + ");"
    };
    private DataSource oracleDatasource = null;

    private AbstractTaskConfig getTask() throws IOException {
        URL url = this.getClass().getClassLoader().getResource(this.configFile);
        FileReader reader = new FileReader(url.getPath());
        StringWriter writer = new StringWriter();
        char[] buffer = new char[1024];
        int len = reader.read(buffer);
        while (len != -1) {
            writer.write(buffer, 0, len);
            len = reader.read(buffer);
        }
        reader.close();
        writer.close();
        ObjectMapper mapper = new ObjectMapper();
        DefaultTaskConfig config = mapper.readValue(writer.toString(), DefaultTaskConfig.class);
        config.setDbConfig(getOracleConfig());
        return config;
    }

    @Before
    public void initEnv() throws SQLException {
        if (oracleDatasource == null) {
            DataBaseConfig config = getOracleConfig();
            oracleDatasource = new DataSourceFactory(config).generate();
        }
        try (Connection connection = oracleDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String ddl : ddls) {
                    statement.execute(ddl);
                }
                statement.execute(
                        "insert into emp(col,col2,col3,col4,col5) values(12.1,'12.44','11.67','23.44', 'hello,world');");
            }
        }
    }

    @Test
    public void testMockTask() throws Throwable {
        AbstractTaskConfig config = getTask();
        AbstractMockerFactory factory = new ObMockerFactory(config);
        ObDataMocker mocker = factory.create();
        MockContext context = mocker.start();
        Callable<Boolean> task = () -> {
            long start = System.currentTimeMillis();
            while (true) {
                boolean flag = true;
                List<TableTaskContext> contexts = context.getTables();
                if (contexts.size() == 0) {
                    flag = false;
                }
                for (TableTaskContext item : contexts) {
                    long interval = System.currentTimeMillis() - start;
                    System.out.printf("[\"%s\" - \"%s\"] : %s - %s - %.2f %%%n", item.getTaskName(),
                            item.getTableTaskId(), item.getStatus(), PrintUtil.convertToReadableTimeString(interval,
                                    TimeUnit.MILLISECONDS, TimeUnit.MINUTES, TimeUnit.SECONDS),
                            context.getProgress());
                    if (MockTaskStatus.CANCELED.equals(item.getStatus())
                            || MockTaskStatus.FAILED.equals(item.getStatus())) {
                        return false;
                    }
                    flag &= MockTaskStatus.SUCCESS.equals(item.getStatus());
                }
                Thread.sleep(3000);
                if (flag) {
                    return true;
                }
            }
        };
        Future<Boolean> res = this.executor.submit(task);
        Assert.assertTrue(res.get());
    }

    private void clearFile() throws IOException {
        AbstractTaskConfig config = getTask();
        for (AbstractTableConfig tableConfig : config.tasks()) {
            for (ScriptType type : ScriptType.values()) {
                String location = tableConfig.dataWriteLocation(type);
                File file = new File(location);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }

    @After
    public void clearEnv() throws SQLException, IOException {
        try (Connection connection = oracleDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("drop table emp");
            }
        }
        try {
            if (oracleDatasource instanceof AutoCloseable) {
                ((AutoCloseable) oracleDatasource).close();
            }
        } catch (Exception e) {
            // eat exception
        }
        clearFile();
    }
}
