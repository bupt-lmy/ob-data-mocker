package com.oceanbase.tools.datamocker.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.constraint.ConstraintFactory;
import com.oceanbase.tools.datamocker.core.write.output.MockerDataSource;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * The test class of the constraint factory class is used to test the logic of the factory class
 *
 * @author yh263208
 * @date 2021-01-11 17:11
 * @since OBMOCKER-snapshot-0.1.0
 */
public class ConstraintFactoryTest extends MockerTestBase {
    /**
     * The location of the mysql database connection configuration file
     */
    private final String mysqlEnv = "db/mysql-env.properties";
    /**
     * The location of the oracle database connection configuration file
     */
    private final String oracleEnv = "db/oracle-env.properties";
    private final String ddlMysql = "CREATE TABLE `emp` (\n"
                                    + "  `col` decimal(10,0) NOT NULL,\n"
                                    + "  `col1` decimal(10,0) DEFAULT NULL,\n"
                                    + "  `col2` decimal(10,0) DEFAULT NULL,\n"
                                    + "  PRIMARY KEY (`col`),\n"
                                    + "  UNIQUE KEY `Hello` (`col1`, `col2`) BLOCK_SIZE 16384 GLOBAL\n"
                                    + ")";
    private final String ddlWithVirtualColumnMysql = "CREATE TABLE `emp1` (\n"
                                                     + "  `col` decimal(10,0) NOT NULL,\n"
                                                     + "  `col1` decimal(10,0) DEFAULT NULL,\n"
                                                     + "  `col2` decimal(10,0) DEFAULT NULL,\n"
                                                     + "  `col4` decimal(10,0) GENERATED ALWAYS AS ((`col2` + `col1`)) VIRTUAL,\n"
                                                     + "  PRIMARY KEY (`col`),\n"
                                                     + "  UNIQUE KEY `Hello` (`col1`, `col2`) BLOCK_SIZE 16384 GLOBAL,\n"
                                                     + "  UNIQUE KEY `hello2` (`col4`) BLOCK_SIZE 16384 GLOBAL\n"
                                                     + ") ";
    private final String ddlOracle = "CREATE TABLE \"EMP\" (\n"
                                     + "  \"COL\" NUMBER(5,2) NOT NULL,\n"
                                     + "  \"COL2\" NUMBER(5,2) NOT NULL,\n"
                                     + "  \"COL3\" NUMBER(4,2) NOT NULL,\n"
                                     + "  CONSTRAINT \"EMP_OBPK_1610357443362979\" PRIMARY KEY (\"COL\"),\n"
                                     + "  CONSTRAINT \"EMP_OBUNIQUE_1610357443363981\" UNIQUE (\"COL2\", \"COL3\"),\n"
                                     + "CONSTRAINT \"EMP_OBCHECK_1610453879284344\" CHECK ((\"COL3\" < 12))\n"
                                     + ") ";
    private final String ddlWithVirtualColumnOracle = "CREATE TABLE \"EMP1\" (\n"
                                                      + "  \"COL\" NUMBER(5,2) NOT NULL,\n"
                                                      + "  \"COL2\" NUMBER(5,2) NOT NULL,\n"
                                                      + "  \"COL3\" NUMBER(4,2) NOT NULL,\n"
                                                      + "  \"COL4\" NUMBER(5,3) GENERATED ALWAYS AS ((\"COL2\" + \"COL3\")) VIRTUAL,\n"
                                                      + "  CONSTRAINT \"EMP_OBPK\" PRIMARY KEY (\"COL\"),\n"
                                                      + "  CONSTRAINT \"EMP_OBUNIQUE_1231\" UNIQUE (\"COL2\", \"COL3\"),\n"
                                                      + "  CONSTRAINT \"EMP_OBUNIQUE_12343\" UNIQUE (\"COL4\")\n,"
                                                      + "CONSTRAINT \"EMP1_OBFK_1610454320318209\" FOREIGN KEY (\"COL2\") REFERENCES "
                                                      + "\"SYS\".\"EMP\"(\"COL\")\n"
                                                      + ");";
    private DataSource oracleDatasource = null;
    private DataSource mysqlDatasource = null;

    @Before
    public void initEnv() throws IOException, SQLException {
        if (oracleDatasource == null) {
            DataBaseConfig config = getDBConfig(ObModeType.OB_ORACLE);
            oracleDatasource = new MockerDataSource(config, 3, 5, 2, null);
        }
        if (mysqlDatasource == null) {
            DataBaseConfig config = getDBConfig(ObModeType.OB_MYSQL);
            mysqlDatasource = new MockerDataSource(config, 3, 5, 2, null);
        }
        try (Connection connection = oracleDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(ddlOracle);
                statement.execute("insert into emp(col,col2,col3) values(12.1,12.44,11.67);");
                statement.execute(ddlWithVirtualColumnOracle);
                statement.execute("insert into emp1(col,col2,col3) values(12.1,12.1,15.67);");
            }
        }
        try (Connection connection = mysqlDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(ddlMysql);
                statement.execute(ddlWithVirtualColumnMysql);
                statement.execute("insert into emp1(col,col1,col2) values(12.1,12.44,15.67);");
            }
        }
    }

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

    private Map<String, AbstractDataType> getSchema() {
        Map<String, AbstractDataType> schema = new HashMap<>();
        schema.put("COL", new OracleNumberType(5, 2, null, false));
        schema.put("COL2", new OracleNumberType(5, 2, null, false));
        schema.put("COL3", new OracleNumberType(4, 2, null, false));
        schema.put("COL4", new OracleNumberType(5, 3, null, false));
        schema.put("col", new OracleNumberType(5, 2, null, false));
        schema.put("col1", new OracleNumberType(5, 2, null, false));
        schema.put("col2", new OracleNumberType(5, 2, null, false));
        schema.put("col4", new OracleNumberType(5, 2, null, false));
        return schema;
    }

    @Test
    public void testConstraintFactoryForOracle() throws Throwable {
        Map<String, AbstractDataType> schema = getSchema();
        List<AbstractConstraint> list = ConstraintFactory.getInstance("UNIQUE_CONSTRAINT").make(oracleDatasource, ObModeType.OB_ORACLE,
                "SYS", "EMP", schema,
                15000);
        Assert.assertEquals(1, list.size());
    }

    @Test
    public void testPConstraintFactoryForOracle() throws Throwable {
        Map<String, AbstractDataType> schema = getSchema();
        List<AbstractConstraint> list = ConstraintFactory.getInstance("PRIMARY_CONSTRAINT").make(oracleDatasource, ObModeType.OB_ORACLE,
                "SYS", "EMP", schema,
                15000);
        Assert.assertEquals(1, list.size());
    }

    @Test
    public void testCConstraintFactoryForOracle() throws Throwable {
        Map<String, AbstractDataType> schema = getSchema();
        thrown.expectMessage("Check constraint is not support yet");
        thrown.expect(MockerException.class);
        List<AbstractConstraint> list = ConstraintFactory.getInstance("CHECK_CONSTRAINT").make(oracleDatasource, ObModeType.OB_ORACLE,
                "SYS", "EMP", schema,
                15000);
        Assert.assertNull(list);
    }

    @Test
    public void testFConstraintFactoryForOracle() throws Throwable {
        Map<String, AbstractDataType> schema = getSchema();
        thrown.expect(MockerException.class);
        thrown.expectMessage("Foreign constraint is not support yet");
        List<AbstractConstraint> list = ConstraintFactory.getInstance("FOREIGN_CONSTRAINT").make(oracleDatasource, ObModeType.OB_ORACLE,
                "SYS", "EMP1", schema,
                15000);
        Assert.assertNull(list);
    }

    @Test
    public void testConstraintFactoryWithVirtualColForOracle() throws Throwable {
        Map<String, AbstractDataType> schema = getSchema();
        thrown.expect(MockerException.class);
        thrown.expectMessage("Virtual column \"EMP1.COL4\" for constraint is not support yet");
        List<AbstractConstraint> list = ConstraintFactory.getInstance("UNIQUE_CONSTRAINT").make(oracleDatasource, ObModeType.OB_ORACLE,
                "SYS", "EMP1", schema,
                15000);
        Assert.assertEquals(1, list.size());
    }

    @Test
    public void testConstraintFactoryForMysql() throws Throwable {
        Map<String, AbstractDataType> schema = getSchema();
        List<AbstractConstraint> list = ConstraintFactory.getInstance("UNIQUE_CONSTRAINT").make(mysqlDatasource, ObModeType.OB_MYSQL,
                "test", "emp", schema,
                15000);
        Assert.assertEquals(1, list.size());
    }

    @Test
    public void testPConstraintFactoryForMysql() throws Throwable {
        Map<String, AbstractDataType> schema = getSchema();
        List<AbstractConstraint> list = ConstraintFactory.getInstance("PRIMARY_CONSTRAINT").make(mysqlDatasource, ObModeType.OB_MYSQL,
                "test", "emp1", schema,
                15000);
        Assert.assertEquals(1, list.size());
    }

    @Test
    public void testConstraintFactoryWithVirtualColForMysql() throws Throwable {
        Map<String, AbstractDataType> schema = getSchema();
        thrown.expect(MockerException.class);
        thrown.expectMessage("Virtual column \"emp1.col4\" for constraint is not support yet");
        List<AbstractConstraint> list = ConstraintFactory.getInstance("UNIQUE_CONSTRAINT").make(mysqlDatasource, ObModeType.OB_MYSQL,
                "test", "emp1", schema,
                15000);
        Assert.assertEquals(1, list.size());
    }

    @Test
    public void testConstraintFactoryInstances() {
        List<ConstraintFactory> list = ConstraintFactory.listInstances();
        Assert.assertEquals(4, list.size());
    }

    @After
    public void clearEnv() throws SQLException {
        try (Connection connection = oracleDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("drop table emp1");
                statement.execute("drop table emp");
            }
        }
        try (Connection connection = mysqlDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("drop table emp1");
                statement.execute("drop table emp");
            }
        }
    }
}
