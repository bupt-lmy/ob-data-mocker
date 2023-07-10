package com.oceanbase.tools.datamocker.service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Test object for database connection pool
 *
 * @author yh263208
 * @date 2021-01-04 20:55
 * @since OBMOCKER_0.1.0_snapshot
 */
@Slf4j
public class DataSourceTest extends MockerTestBase {

    @Rule
    public ExpectedException expect = ExpectedException.none();
    private static final Map<String, String> params = new HashMap<>();

    @BeforeClass
    public static void initParam() {
        params.put("socketTimeout", "8000");
    }

    private DataBaseConfig getDBConfig(ObModeType dialectType) {
        return dialectType == ObModeType.OB_MYSQL ? getMySqlConfig() : getOracleConfig();
    }

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
    public void testDataSourceForMysql() throws SQLException {
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
        assert config != null;
        config.setHost(null);
        expect.expectMessage("Host can not be blank for MockerDataSource#validate");
        expect.expect(IllegalArgumentException.class);
        DataSource dataSource = new MockerDataSource(config, 3, 5, 2, params);
    }

    @Test
    public void testDataSourceWithIllegalPoolSize() throws IOException, SQLException {
        DataBaseConfig config = getDBConfig(ObModeType.OB_MYSQL);
        expect.expectMessage("Min pool size, max pool size or increase step can not be equal to or less than zero");
        expect.expect(MockerException.class);
        DataSource dataSource = new MockerDataSource(config, -3, -5, -4, params);
    }

    @Test
    public void testDataSourceWithIllegalPoolSize2() throws IOException, SQLException {
        DataBaseConfig config = getDBConfig(ObModeType.OB_MYSQL);
        expect.expectMessage("Min pool size can not be bigger than max pool size");
        expect.expect(MockerException.class);
        DataSource dataSource = new MockerDataSource(config, 10, 5, 4, params);
    }

    @Test
    public void testDataSourceWithMultiThread()
            throws IOException, SQLException, InterruptedException, TimeoutException,
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
    public void testDataSourceWithDirectConnection() throws SQLException {
        DataBaseConfig config = getDBConfig(ObModeType.OB_ORACLE);
        DataSource dataSource = new MockerDataSource(config, 3, 5, 2, params);
        String username = config.getUser() + "@" + config.getTenant();
        if (config.getCluster() != null) {
            username += "#" + config.getCluster();
        }
        Connection connection = dataSource.getConnection(username, config.getPassword());
        Assert.assertNotNull(connection);
        connection.close();
    }

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
