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
import com.oceanbase.tools.datamocker.model.enums.DialectType;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
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
public class MockerTaskMysqlTest extends MockerTestBase {
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 5, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
    /**
     * 任务配置文件目录
     */
    private final String configFile = "task/config-mysql.json";
    /**
     * mysql数据库连接配置文件所在地
     */
    private final String mysqlEnv = "db/mysql-env.properties";
    /**
     * oracle数据库连接配置文件所在地
     */
    private final String oracleEnv = "db/oracle-env.properties";
    private final String[] ddls = new String[] {
            " CREATE TABLE `emp` (\n"
            + "  `col` tinyint(4) DEFAULT NULL,\n"
            + "  `col2` tinyint(3) unsigned DEFAULT NULL,\n"
            + "  `col3` smallint(6) NOT NULL,\n"
            + "  `col4` smallint(5) unsigned DEFAULT NULL,\n"
            + "  `col5` mediumint(9) DEFAULT NULL,\n"
            + "  `col6` mediumint(8) unsigned DEFAULT NULL,\n"
            + "  `col7` int(11) DEFAULT NULL,\n"
            + "  `col8` int(10) unsigned DEFAULT NULL,\n"
            + "  `col9` bigint(20) DEFAULT NULL,\n"
            + "  `col10` bigint(20) unsigned DEFAULT NULL,\n"
            + "  `col11` decimal(10,0) DEFAULT NULL,\n"
            + "  `col12` decimal(12,5) unsigned DEFAULT NULL,\n"
            + "  `col13` float unsigned DEFAULT NULL,\n"
            + "  `col14` float DEFAULT NULL,\n"
            + "  `col15` float(5,3) DEFAULT NULL,\n"
            + "  `col16` char(256),\n"
            + "  `col17` varchar(512) DEFAULT NULL,\n"
            + "  `col18` tinytext DEFAULT NULL,\n"
            + "  `col19` text DEFAULT NULL,\n"
            + "  `col20` mediumtext DEFAULT NULL,\n"
            + "  `col21` longtext DEFAULT NULL,\n"
            + "  `col22` tinyblob DEFAULT NULL,\n"
            + "  `col23` blob DEFAULT NULL,\n"
            + "  `col24` mediumblob DEFAULT NULL,\n"
            + "  `col25` longblob DEFAULT NULL,\n"
            + "  `col26` binary(128) DEFAULT NULL,\n"
            + "  `col27` varbinary(256) DEFAULT NULL,\n"
            + "  `col28` date DEFAULT NULL,\n"
            + "  `col29` timestamp(5) DEFAULT NULL,\n"
            + "  `col30` time(2) DEFAULT NULL,\n"
            + "  `col31` datetime DEFAULT NULL,\n"
            + "  `col32` year(4) DEFAULT NULL,\n"
            + "  `col33` bit(16) DEFAULT NULL\n"
            + ") ;"
    };
    private DataSource mysqlDatasource = null;

    /**
     * 获取测试数据库连接配置信息
     *
     * @param dialectType 方言类型
     * @throws IOException 文件读取操作可能会抛出异常
     */
    private DataBaseConfig getDBConfig(DialectType dialectType) throws IOException {
        DataBaseConfig config = new DataBaseConfig();
        Properties properties = new Properties();
        URL url = null;
        if (DialectType.OB_MYSQL.equals(dialectType)) {
            url = this.getClass().getClassLoader().getResource(mysqlEnv);
        } else if (DialectType.OB_ORACLE.equals(dialectType)) {
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
        if (mysqlDatasource == null) {
            DataBaseConfig config = getDBConfig(DialectType.OB_MYSQL);
            mysqlDatasource = new MockerDataSource(config, 3, 5, 2, null);
        }
        try (Connection connection = mysqlDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String ddl : ddls) {
                    statement.execute(ddl);
                }
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
        try (Connection connection = mysqlDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("drop table emp");
            }
        }
        ((MockerDataSource) mysqlDatasource).clear();
        clearFile();
    }
}
