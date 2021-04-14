package com.oceanbase.tools.datamocker.task.primitive;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.core.task.AbstractDataPipe;
import com.oceanbase.tools.datamocker.core.write.AbstractMockWriter;
import com.oceanbase.tools.datamocker.core.write.DataBaseWriter;
import com.oceanbase.tools.datamocker.core.write.output.MockerDataSource;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.MockDataPipe;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * JDBC操作原语测试类
 *
 * @author yh263208
 * @date 2021-01-04
 * @since OBMOCKER_0.1.0_snapshot
 */
@Slf4j
public class DataBasePrimitiveTest extends MockerTestBase {
    /**
     * mysql数据库连接配置文件所在地
     */
    private final static String mysqlEnv = "db/mysql-env.properties";
    /**
     * oracle数据库连接配置文件所在地
     */
    private final static String oracleEnv = "db/oracle-env.properties";
    @Rule
    public ExpectedException expect = ExpectedException.none();
    private final static String tableName = "TEST_EMP";
    /**
     * 列信息
     */
    private final static List<String> columnList = Arrays.asList("COL1", "COL2", "COL3");
    private static DataSource oracleDataSource;
    private static DataSource mysqlDataSource;

    /**
     * 获取测试数据库连接配置信息
     *
     * @param dialectType 方言类型
     * @throws IOException 文件读取操作可能会抛出异常
     */
    private static DataBaseConfig getDBConfig(ObModeType dialectType) throws IOException {
        DataBaseConfig config = new DataBaseConfig();
        Properties properties = new Properties();
        URL url = null;
        if (ObModeType.OB_MYSQL.equals(dialectType)) {
            url = DataBasePrimitiveTest.class.getClassLoader().getResource(mysqlEnv);
        } else if (ObModeType.OB_ORACLE.equals(dialectType)) {
            url = DataBasePrimitiveTest.class.getClassLoader().getResource(oracleEnv);
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
     * 初始化环境，创建一个目标表
     *
     * @param connection 一个数据连接
     */
    private static void initEnv(Connection connection) throws SQLException {
        String sql = String.format("create table %s (%s varchar(20) not null, %s varchar(20) not null, %s varchar(20) not null)", tableName,
                columnList.get(0), columnList.get(1), columnList.get(2));
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement(sql);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException(e);
        } finally {
            close(connection, statement, resultSet);
        }
    }

    /**
     * 获取一个测试用的随机数据块
     *
     * @param size 数据行数
     * @return 返回数据
     */
    private List<Map<String, Pair<AbstractDataType, Object>>> getRows(int size) {
        List<Map<String, Pair<AbstractDataType, Object>>> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Map<String, Pair<AbstractDataType, Object>> row = new HashMap<>();
            for (String column : columnList) {
                row.put(column, new Pair<>(new OracleNumberType(8, 5, null, false), String.valueOf(new Random().nextInt(1000))));
            }
            list.add(row);
        }
        return list;
    }

    /**
     * 关闭数据库资源
     *
     * @param connection 数据库连接
     * @param statement  数据库操作句柄
     * @param resultSet  结果集
     */
    private static void close(Connection connection, Statement statement, ResultSet resultSet) {
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

    @BeforeClass
    public static void initEnv() throws IOException, SQLException {
        DataBaseConfig mysqlConfig = getDBConfig(ObModeType.OB_MYSQL);
        mysqlDataSource = new MockerDataSource(mysqlConfig, 3, 5, 2, null);
        DataBaseConfig oracleConfig = getDBConfig(ObModeType.OB_ORACLE);
        oracleDataSource = new MockerDataSource(oracleConfig, 3, 5, 2, null);
        initEnv(oracleDataSource.getConnection());
        initEnv(mysqlDataSource.getConnection());
    }

    @Test
    public void testPrimitiveWithoutDataSource() {
        expect.expectMessage("data source can not be null");
        expect.expect(MockerException.class);
        AbstractMockWriter primitive = new DataBaseWriter(null, null, null, null);
    }

    @Test
    public void testPrimitiveWithoutDatabase() {
        expect.expectMessage("database can not be null");
        expect.expect(MockerException.class);
        AbstractMockWriter primitive = new DataBaseWriter(oracleDataSource, ObModeType.OB_ORACLE, null, null);
    }

    @Test
    public void testPrimitiveWithouttable() throws IOException {
        expect.expectMessage("table name can not be null");
        expect.expect(MockerException.class);
        ObModeType dialectType = ObModeType.OB_ORACLE;
        DataBaseConfig config = getDBConfig(dialectType);
        AbstractMockWriter primitive = new DataBaseWriter(oracleDataSource, dialectType, config.getDefaultSchame(), null);
    }

    @Test
    public void testInsertDataForMysql() throws Throwable {
        List<Map<String, Pair<AbstractDataType, Object>>> rows = getRows(24);
        ObModeType dialectType = ObModeType.OB_MYSQL;
        DataBaseConfig config = getDBConfig(dialectType);
        DataBaseWriter primitive = new DataBaseWriter(mysqlDataSource, dialectType, config.getDefaultSchame(), tableName);
        AbstractDataPipe pipe = new MockDataPipe(1);
        primitive.register(pipe);
        pipe.write(rows);
        Long count = primitive.write();
        Assert.assertEquals(rows.size(), count.intValue());
    }

    @Test
    public void testInsertDataForOracle() throws Throwable {
        List<Map<String, Pair<AbstractDataType, Object>>> rows = getRows(24);
        ObModeType dialectType = ObModeType.OB_ORACLE;
        DataBaseConfig config = getDBConfig(dialectType);
        DataBaseWriter primitive = new DataBaseWriter(oracleDataSource, dialectType, config.getDefaultSchame(), tableName);
        AbstractDataPipe pipe = new MockDataPipe(1);
        primitive.register(pipe);
        pipe.write(rows);
        Long count = primitive.write();
        Assert.assertEquals(rows.size(), count.intValue());
    }

    /**
     * 关闭环境，创建一个目标表
     *
     * @param connection 一个数据连接
     */
    private static void closeEnv(Connection connection) throws SQLException {
        String sql = String.format("drop table %s", tableName);
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement(sql);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException(e);
        } finally {
            close(connection, statement, resultSet);
        }
    }

    @AfterClass
    public static void closeEnv() throws SQLException {
        closeEnv(oracleDataSource.getConnection());
        closeEnv(mysqlDataSource.getConnection());
    }

}
