package com.oceanbase.tools.datamocker.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.core.write.output.MockerDataSource;
import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * 数据库连接池的测试对象
 *
 * @author yh263208
 * @date 2021-01-04 20:55
 * @since OBMOCKER_0.1.0_snapshot
 */
@Slf4j
public class DataSourceTest extends MockerTestBase {
    /**
     * mysql数据库连接配置文件所在地
     */
    private final String mysqlEnv = "db/mysql-env.properties";
    /**
     * oracle数据库连接配置文件所在地
     */
    private final String oracleEnv = "db/oracle-env.properties";
    @Rule
    public ExpectedException expect = ExpectedException.none();
    private static final Map<String, String> params = new HashMap<>();

    @BeforeClass
    public static void initParam() {
        params.put("socketTimeout", "8000");
    }

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
     * 测试数据源
     *
     * @param dataSource 数据源
     * @param sql        测试sql
     */
    private void testDataSource(DataSource dataSource, String sql) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
        } finally {
            close(connection, statement, resultSet);
        }
    }

    @Test
    public void testDataSourceForMysql() throws SQLException, IOException {
        DataBaseConfig config = getDBConfig(ObModeType.OB_MYSQL);
        DataSource dataSource = new MockerDataSource(config, 3, 5, 2, params);
        dataSource.setLoginTimeout(10);
        Assert.assertEquals(10, dataSource.getLoginTimeout());
        testDataSource(dataSource, "show tables;");
    }

    @Test
    public void testDataSourceForOracle() throws IOException, SQLException {
        DataBaseConfig config = getDBConfig(ObModeType.OB_ORACLE);
        DataSource dataSource = new MockerDataSource(config, params);
        dataSource.setLoginTimeout(5);
        Assert.assertEquals(5, dataSource.getLoginTimeout());
        testDataSource(dataSource, "select count(*) from all_tables;");
    }

    @Test
    public void testDataSourceWithIllegalParam() throws IOException, SQLException {
        DataBaseConfig config = getDBConfig(ObModeType.OB_MYSQL);
        config.setHost(null);
        expect.expectMessage("database's config is illegal");
        expect.expect(MockerException.class);
        DataSource dataSource = new MockerDataSource(config, 3, 5, 2, params);
    }

    @Test
    public void testDataSourceWithIllegalPoolSize() throws IOException, SQLException {
        DataBaseConfig config = getDBConfig(ObModeType.OB_MYSQL);
        expect.expectMessage("min pool size, max pool size or increase step can not be equal to or less than zero");
        expect.expect(MockerException.class);
        DataSource dataSource = new MockerDataSource(config, -3, -5, -4, params);
    }

    @Test
    public void testDataSourceWithIllegalPoolSize2() throws IOException, SQLException {
        DataBaseConfig config = getDBConfig(ObModeType.OB_MYSQL);
        expect.expectMessage("min pool size can not be bigger than max pool size");
        expect.expect(MockerException.class);
        DataSource dataSource = new MockerDataSource(config, 10, 5, 4, params);
    }

    @Test
    public void testDataSourceWithMultiThread() throws IOException, SQLException, InterruptedException, TimeoutException,
                                                       ExecutionException {
        DataBaseConfig config = getDBConfig(ObModeType.OB_ORACLE);
        DataSource dataSource = new MockerDataSource(config, 3, 5, 2, params);
        dataSource.setLoginTimeout(5);
        Assert.assertEquals(5, dataSource.getLoginTimeout());
        ThreadPoolExecutor executor = new ThreadPoolExecutor(15, 25, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
        List<Future<String>> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Callable<String> task = () -> {
                Connection connection = null;
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                String result = null;
                try {
                    connection = dataSource.getConnection();
                    String sql = "select count(*) from all_tables";
                    statement = connection.prepareStatement(sql);
                    resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        result = resultSet.getString(1);
                    }
                    return result;
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    close(connection, statement, resultSet);
                }
                return null;
            };
            list.add(executor.submit(task));
        }
        int totalCount = 0;
        for (Future<String> future : list) {
            totalCount++;
            Assert.assertNotNull(future.get(5, TimeUnit.SECONDS));
        }
        Assert.assertEquals(10, totalCount);
    }

    @Test
    public void testDataSourceWithDirectConnection() throws IOException, SQLException {
        DataBaseConfig config = getDBConfig(ObModeType.OB_ORACLE);
        DataSource dataSource = new MockerDataSource(config, 3, 5, 2, params);
        Connection connection = dataSource.getConnection(config.getUser() + "@" + config.getTenant(), config.getPassword());
        Assert.assertNotNull(connection);
        connection.close();
    }

    /**
     * 关闭数据库资源
     *
     * @param connection 数据库连接
     * @param statement  数据库操作句柄
     * @param resultSet  结果集
     */
    private void close(Connection connection, Statement statement, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                log.error("fail to close result set", e);
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                log.error("fail to close statement", e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("fail to close connection", e);
            }
        }
    }
}
