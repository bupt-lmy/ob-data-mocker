package com.oceanbase.tools.datamocker.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.ObDataMocker;
import com.oceanbase.tools.datamocker.ObMockerFactory;
import com.oceanbase.tools.datamocker.core.task.AbstractMockerFactory;
import com.oceanbase.tools.datamocker.core.write.output.MockerDataSource;
import com.oceanbase.tools.datamocker.model.config.AbstractTaskConfig;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultColumnConfig;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultTableConfig;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultTaskConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataTypeConfig;
import com.oceanbase.tools.datamocker.model.config.model.DigitDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.DuplicateStrategy;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 分发器工厂类的测试类
 *
 * @author yh263208
 * @date 2021-01-11 21:50
 * @since OBMOCKER_snaoshot_0.1.0
 */
public class DispatcherFactoryTest extends MockerTestBase {
    /**
     * mysql数据库连接配置文件所在地
     */
    private final String mysqlEnv = "db/mysql-env.properties";
    /**
     * oracle数据库连接配置文件所在地
     */
    private final String oracleEnv = "db/oracle-env.properties";
    private final String ddlOracle = "CREATE TABLE \"EMP\" (\n"
                                     + "  \"COL\" NUMBER(5,2) NOT NULL,\n"
                                     + "  \"COL2\" NUMBER(5,2) NOT NULL,\n"
                                     + "  \"COL3\" NUMBER(4,2) NOT NULL,\n"
                                     + "  CONSTRAINT \"EMP_OBPK_1610357443362979\" PRIMARY KEY (\"COL\"),\n"
                                     + "  CONSTRAINT \"EMP_OBUNIQUE_1610357443363981\" UNIQUE (\"COL2\", "
                                     + "\"COL3\")\n"
                                     + ") ";
    private final String ddlWithVirtualColumnOracle = "CREATE TABLE \"EMP1\" (\n"
                                                      + "  \"COL\" NUMBER(5,2) NOT NULL,\n"
                                                      + "  \"COL2\" NUMBER(5,2) NOT NULL,\n"
                                                      + "  \"COL3\" NUMBER(4,2) NOT NULL,\n"
                                                      + "  \"COL4\" NUMBER(5,3) GENERATED ALWAYS AS ((\"COL2\" + \"COL3\")) "
                                                      + "VIRTUAL,\n"
                                                      + "  CONSTRAINT \"EMP_OBPK\" PRIMARY KEY (\"COL\"),\n"
                                                      + "  CONSTRAINT \"EMP_OBUNIQUE_1231\" UNIQUE (\"COL2\", \"COL3\"),\n"
                                                      + "  CONSTRAINT \"EMP_OBUNIQUE_12343\" UNIQUE (\"COL4\")\n,"
                                                      + "CONSTRAINT \"EMP1_OBFK_1610454320318209\" FOREIGN KEY (\"COL2\") "
                                                      + "REFERENCES "
                                                      + "\"SYS\".\"EMP\"(\"COL\")\n"
                                                      + ");";
    private DataSource oracleDatasource = null;
    /**
     * 表任务有关的参数
     */
    private final Long maxBatchsize = 1024L;
    private final Long maxGenerateCount = 9800L;

    /**
     * 初始化一个数字类型的数据生成器配置
     */
    private DataTypeConfig initDigitGen(Map<String, Double> builderParams, String typeName, BigDecimal lowValue, BigDecimal highValue,
            String genName, Integer precision, Integer scale) {
        DigitDataTypeConfig digit = new DigitDataTypeConfig();
        digit.setColumnType(typeName);
        digit.setLowValue(lowValue);
        digit.setHighValue(highValue);
        digit.setGenParams(builderParams);
        digit.setGenerator(genName);
        digit.setPrecision(precision);
        digit.setScale(scale);
        return digit;
    }

    private List<DefaultColumnConfig> initColumnConfig(String tableName) {
        List<DefaultColumnConfig> configList = new ArrayList<>();
        if ("EMP".equals(tableName)) {
            Map<String, Double> builderParams = new HashMap<>();
            builderParams.put("average", 50.21);
            builderParams.put("variance", 16.43);

            DefaultColumnConfig col1 = new DefaultColumnConfig();
            col1.setColumnName("COL");
            col1.setAllowNull(false);
            col1.setDefaultValue(null);
            col1.setTypeConfig(initDigitGen(builderParams, "OB_ORACLE_NUMBER", BigDecimal.ZERO,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    "NORMAL_GENERATOR", 5, 2));
            configList.add(col1);

            DefaultColumnConfig col2 = new DefaultColumnConfig();
            col2.setColumnName("COL2");
            col2.setAllowNull(false);
            col2.setDefaultValue(null);
            col2.setTypeConfig(initDigitGen(builderParams, "OB_ORACLE_NUMBER", BigDecimal.ZERO,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    "NORMAL_GENERATOR", 5, 2));
            configList.add(col2);

            DefaultColumnConfig col3 = new DefaultColumnConfig();
            col3.setColumnName("COL3");
            col3.setAllowNull(false);
            col3.setDefaultValue(null);
            col3.setTypeConfig(initDigitGen(builderParams, "OB_ORACLE_NUMBER", BigDecimal.ZERO,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    "NORMAL_GENERATOR", 4, 2));
            configList.add(col3);
        } else if ("EMP1".equals(tableName)) {
            Map<String, Double> builderParams = new HashMap<>();
            builderParams.put("average", 50.21);
            builderParams.put("variance", 16.43);

            DefaultColumnConfig col1 = new DefaultColumnConfig();
            col1.setColumnName("COL");
            col1.setAllowNull(false);
            col1.setDefaultValue(null);
            col1.setTypeConfig(initDigitGen(builderParams, "OB_ORACLE_NUMBER", BigDecimal.ZERO,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    "NORMAL_GENERATOR", 5, 2));
            configList.add(col1);

            DefaultColumnConfig col2 = new DefaultColumnConfig();
            col2.setColumnName("COL2");
            col2.setAllowNull(false);
            col2.setDefaultValue(null);
            col2.setTypeConfig(initDigitGen(builderParams, "OB_ORACLE_NUMBER", BigDecimal.ZERO,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    "NORMAL_GENERATOR", 5, 2));
            configList.add(col2);

            DefaultColumnConfig col3 = new DefaultColumnConfig();
            col3.setColumnName("COL3");
            col3.setAllowNull(false);
            col3.setDefaultValue(null);
            col3.setTypeConfig(initDigitGen(builderParams, "OB_ORACLE_NUMBER", BigDecimal.ZERO,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    "NORMAL_GENERATOR", 4, 2));
            configList.add(col3);

            DefaultColumnConfig col4 = new DefaultColumnConfig();
            col4.setColumnName("COL4");
            col4.setAllowNull(false);
            col4.setDefaultValue(null);
            col4.setTypeConfig(initDigitGen(builderParams, "OB_ORACLE_NUMBER", BigDecimal.ZERO,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    "NORMAL_GENERATOR", 5, 3));
            configList.add(col4);
        }
        return configList;
    }

    private DefaultTableConfig initTableConfig(String tableName, String schemaName) {
        DefaultTableConfig tableConfig = new DefaultTableConfig();
        tableConfig.setColumns(initColumnConfig(tableName));
        tableConfig.setTotalCount(maxGenerateCount);
        tableConfig.setStrategy(DuplicateStrategy.IGNORE);
        tableConfig.setBatchSize(maxBatchsize);
        tableConfig.setWhetherTruncate(true);
        tableConfig.setTableName(tableName);
        tableConfig.setSchemaName(schemaName);
        tableConfig.setLocation("test/mock/test.txt");
        tableConfig.setTimeout(180000L);
        return tableConfig;
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

    private AbstractTaskConfig getTask(String tableName) throws IOException {
        DataBaseConfig config = getDBConfig(ObModeType.OB_ORACLE);
        DefaultTaskConfig taskConfig = new DefaultTaskConfig();
        DefaultTableConfig tableConfig = initTableConfig(tableName, config.getDefaultSchame());
        taskConfig.setTables(Arrays.asList(tableConfig));
        taskConfig.setDbConfig(config);
        taskConfig.setDialectType(ObModeType.OB_ORACLE);
        taskConfig.setConnectionIncreasementStep(2);
        taskConfig.setMaxConnectionSize(15);
        taskConfig.setMinConnectionSize(5);
        return taskConfig;
    }

    @Before
    public void initEnv() throws IOException, SQLException {
        if (oracleDatasource == null) {
            DataBaseConfig config = getDBConfig(ObModeType.OB_ORACLE);
            oracleDatasource = new MockerDataSource(config, 3, 5, 2, null);
        }
        try (Connection connection = oracleDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(ddlOracle);
                statement.execute("insert into emp(col,col2,col3) values(12.1,12.44,11.67);");
                statement.execute(ddlWithVirtualColumnOracle);
                statement.execute("insert into emp1(col,col2,col3) values(12.1,12.1,15.67);");
            }
        }
    }

    @Test
    public void testDispatcher() throws Exception {
        AbstractTaskConfig config = getTask("EMP");
        AbstractMockerFactory factory = new ObMockerFactory(config);
        ObDataMocker mocker = factory.create();
        Assert.assertEquals(1, mocker.size());
    }

    @Test
    public void testDispatcherWithVirtualColumn() throws Exception {
        AbstractTaskConfig config = getTask("EMP1");
        AbstractMockerFactory factory = new ObMockerFactory(config);
        thrown.expectMessage("virtual column \"EMP1.COL4\" for constraint is not support yet");
        thrown.expect(MockerException.class);
        ObDataMocker mocker = factory.create();
        Assert.assertEquals(1, mocker.size());
    }

    @Test
    public void testDispatcherWithNullTable() throws Exception {
        AbstractTaskConfig config = getTask("EMP2");
        AbstractMockerFactory factory = new ObMockerFactory(config);
        thrown.expectMessage("ORA-00942: table or view 'SYS.EMP2' does not exist");
        thrown.expect(MockerException.class);
        ObDataMocker mocker = factory.create();
        Assert.assertEquals(1, mocker.size());
    }

    @After
    public void clearEnv() throws SQLException {
        try (Connection connection = oracleDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("drop table emp1");
                statement.execute("drop table emp");
            }
        }
    }
}
