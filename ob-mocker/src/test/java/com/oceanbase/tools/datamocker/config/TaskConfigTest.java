package com.oceanbase.tools.datamocker.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultColumnConfig;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultTableConfig;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultTaskConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataTypeConfig;
import com.oceanbase.tools.datamocker.model.config.model.DigitDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.DialectType;
import com.oceanbase.tools.datamocker.model.enums.DuplicateStrategy;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 总的生成任务配置
 *
 * @author yh263208
 * @date 2020-12-27 20:45
 * @since OBMOCKER-snapshot-0.1.0
 */
public class TaskConfigTest extends MockerTestBase {
    /**
     * 列任务有关的参数
     */
    private final Integer precision = 5;
    private final Integer scale = 2;
    private final Boolean allowNull = false;
    private final String columnName = "SALARY";
    private final Object defaultValue = "DEFAULT_VALUE";
    private final String genName = "NORMAL_GENERATOR";
    private final String typeName = "OB_ORACLE_NUMBER";
    private final BigDecimal lowValue = BigDecimal.ZERO;
    private final BigDecimal highValue = BigDecimal.TEN.multiply(BigDecimal.TEN);
    private final Map<String, Double> builderParams = new HashMap<>();
    /**
     * 表任务有关的参数
     */
    private final int configListSize = 3;
    private final Long maxBatchsize = 1024L;
    private final Long maxGenerateCount = 1000000L;
    private final String tableName = "test_table";
    private final String schemaName = "schema_name";
    /**
     * 总体任务有关的参数
     */
    private final Integer port = 3306;
    private final String host = "xxx.xxx.xxx.xxx";
    private final String user = "test_user";
    private final String tenant = "test_tenant";
    private final String cluster = "test_cluster";
    private final String passwd = "test_passwd";
    private final String defaultSchema = "defaule_schame";
    private DefaultTaskConfig taskConfig = null;
    private DataBaseConfig dbConfig = null;

    /**
     * 初始化一个数字类型的数据生成器配置
     */
    private DataTypeConfig initDigitGen() {
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

    /**
     * 初始化列任务配置对象
     *
     * @param size 列任务大小
     * @return 返回列任务集合
     */
    private List<DefaultColumnConfig> initColumnConfig(int size) {
        builderParams.put("average", 50.21);
        builderParams.put("variance", 16.43);
        List<DefaultColumnConfig> configList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            DefaultColumnConfig config = new DefaultColumnConfig();
            config.setColumnName(columnName);
            config.setAllowNull(allowNull);
            config.setDefaultValue(defaultValue);
            config.setTypeConfig(initDigitGen());
            configList.add(config);
        }
        return configList;
    }

    /**
     * 初始化表任务配置对象
     *
     * @param size 表任务大小
     * @return 返回表任务集合
     */
    private List<DefaultTableConfig> initTableConfig(int size) {
        List<DefaultTableConfig> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            DefaultTableConfig tableConfig = new DefaultTableConfig();
            tableConfig.setColumns(initColumnConfig(size));
            tableConfig.setTotalCount(maxGenerateCount);
            tableConfig.setStrategy(DuplicateStrategy.IGNORE);
            tableConfig.setBatchSize(maxBatchsize);
            tableConfig.setWhetherTruncate(true);
            tableConfig.setTableName(tableName);
            tableConfig.setSchemaName(schemaName);
            list.add(tableConfig);
        }
        return list;
    }

    /**
     * 初始化环境，包括数据库配置
     */
    @Before
    public void initEnv() {
        dbConfig = new DataBaseConfig();
        dbConfig.setHost(host);
        dbConfig.setCluster(cluster);
        dbConfig.setDefaultSchame(defaultSchema);
        dbConfig.setPassword(passwd);
        dbConfig.setPort(port);
        dbConfig.setDefaultSchame(defaultSchema);
        dbConfig.setUser(user);
        dbConfig.setTenant(tenant);

        taskConfig = new DefaultTaskConfig();
        taskConfig.setTables(initTableConfig(configListSize));
        taskConfig.setDbConfig(dbConfig);
        taskConfig.setDialectType(DialectType.OB_ORACLE);
    }

    @Test
    public void testTaskConfig() {
        Assert.assertEquals(dbConfig, taskConfig.dbConfig());
        Assert.assertEquals(DialectType.OB_ORACLE, taskConfig.obDialectType());
        Assert.assertNotNull(taskConfig.tasks());
        Assert.assertEquals(configListSize, taskConfig.tasks().size());
        Assert.assertNull(taskConfig.taskName());
    }

    @After
    public void clearParams() {
        builderParams.clear();
    }
}
