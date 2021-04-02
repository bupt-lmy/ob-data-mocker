package com.oceanbase.tools.datamocker.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
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
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.core.write.output.MockerDataSource;
import com.oceanbase.tools.datamocker.model.config.AbstractTableConfig;
import com.oceanbase.tools.datamocker.model.config.AbstractTaskConfig;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultTaskConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.enums.ScriptType;
import com.oceanbase.tools.datamocker.schedule.MockContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * mock数据任务测试类，用于测试mock数据的任务模块
 *
 * @author yh263208
 * @date 2021-01-17 16:14
 * @since OBMOCKER_0.1.0_snapshot
 */
public class MockerTaskOracleTest extends MockerTestBase {
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 5, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
    /**
     * 任务配置文件目录
     */
    private final String configFile = "task/config-oracle.json";
    /**
     * mysql数据库连接配置文件所在地
     */
    private final String mysqlEnv = "db/mysql-env.properties";
    /**
     * oracle数据库连接配置文件所在地
     */
    private final String oracleEnv = "db/oracle-env.properties";
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

    /**
     * 获取测试数据库连接配置信息
     *
     * @param dialectType 方言类型
     * @throws IOException 文件读取操作可能会抛出异常
     */
    private DataBaseConfig getDBConfig(ObModeType dialectType) throws IOException {
        DataBaseConfig config = new DataBaseConfig();
        Properties properties = new Properties();
        URL url = null;
        if (ObModeType.OB_MYSQL.equals(dialectType)) {
            url = this.getClass().getClassLoader().getResource(mysqlEnv);
        } else if (ObModeType.OB_ORACLE.equals(dialectType)) {
            url = this.getClass().getClassLoader().getResource(oracleEnv);
        } else {
            return null;
        }
        properties.load(new FileInputStream(url.getPath()));
        config.setDefaultSchame(properties.getProperty("schema"));
        config.setPassword(properties.getProperty("passwd"));
        config.setUser(properties.getProperty("user"));
        config.setCluster(properties.getProperty("cluster"));
        config.setTenant(properties.getProperty("tenant"));
        config.setPort(Integer.valueOf(properties.getProperty("port")));
        config.setHost(properties.getProperty("host"));
        return config;
    }

    /**
     * 从配置文件中读取任务配置封装成一个任务配置对象
     *
     * @return 返回任务对象
     * @throws IOException 可能找不到文件
     */
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
        return mapper.readValue(writer.toString(), DefaultTaskConfig.class);
    }

    @Before
    public void initEnv() throws IOException, SQLException {
        if (oracleDatasource == null) {
            DataBaseConfig config = getDBConfig(ObModeType.OB_ORACLE);
            oracleDatasource = new MockerDataSource(config, 3, 5, 2, null);
        }
        try (Connection connection = oracleDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String ddl : ddls) {
                    statement.execute(ddl);
                }
                statement.execute("insert into emp(col,col2,col3,col4,col5) values(12.1,'12.44','11.67','23.44', 'hello,world');");
            }
        }
    }

    @Test
    public void testMockTask() throws Exception {
        AbstractTaskConfig config = getTask();
        AbstractMockerFactory factory = new ObMockerFactory(config);
        ObDataMocker mocker = factory.create();
        MockContext context = mocker.start();
        Callable<Boolean> task = () -> {
            long start = System.currentTimeMillis();
            while (true) {
                Boolean flag = Boolean.TRUE;
                List<TableTaskContext> contexts = context.getTables();
                if (contexts.size() == 0) {
                    flag = false;
                }
                for (TableTaskContext item : contexts) {
                    String interval = (System.currentTimeMillis() - start) / 1000 + "s";
                    System.out.println(
                            String.format("[\"%s\" - \"%s\"] : %s - %s", item.getTaskName(), item.getTaskId(), item.getStatus(),
                                    interval));
                    if (MockTaskStatus.CANCELED.equals(item.getStatus()) || MockTaskStatus.FAILED.equals(item.getStatus())) {
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
        ((MockerDataSource) oracleDatasource).clear();
        clearFile();
    }
}
