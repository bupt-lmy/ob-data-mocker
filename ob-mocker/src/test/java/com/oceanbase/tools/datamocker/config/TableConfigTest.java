package com.oceanbase.tools.datamocker.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultColumnConfig;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultTableConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataTypeConfig;
import com.oceanbase.tools.datamocker.model.config.model.DigitDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.DuplicateStrategy;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Configuration object class test of table generation task
 *
 * @author yh263208
 * @date 2020-12-27 20:28
 * @since OBMOCKER-snapshot-0.1.0
 */
public class TableConfigTest extends MockerTestBase {
    /**
     * List task-related parameters
     */
    private Integer precision = 5;
    private Integer scale = 2;
    private Boolean allowNull = false;
    private String columnName = "SALARY";
    private Object defaultValue = "DEFAULT_VALUE";
    private String genName = "NORMAL_GENERATOR";
    private String typeName = "OB_ORACLE_NUMBER";
    private BigDecimal lowValue = BigDecimal.ZERO;
    private BigDecimal highValue = BigDecimal.TEN.multiply(BigDecimal.TEN);
    private Map<String, Double> builderParams = new HashMap<>();
    /**
     * Table task-related initialization parameters
     */
    private int configListSize = 3;
    private Long batchSize = 1024L;
    private Long totalCount = 1000000L;
    private String tableName = "test_table";
    private String schemaName = "schema_name";
    private DefaultTableConfig tableConfig = null;

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
     * Initialize the column task configuration object
     *
     * @param size Column task size
     * @return Return to the list of tasks
     */
    private List<DefaultColumnConfig> initColumnConfig(int size) {
        builderParams.put("average", 50.21);
        builderParams.put("variance", 16.43);
        List<DefaultColumnConfig> configList = new ArrayList<>();
        DataTypeConfig typeConfig = initDigitGen();
        for (int i = 0; i < size; i++) {
            DefaultColumnConfig config = new DefaultColumnConfig();
            config.setColumnName(columnName);
            config.setAllowNull(allowNull);
            config.setDefaultValue(defaultValue);
            config.setTypeConfig(typeConfig);
            configList.add(config);
        }
        return configList;

    }

    @Before
    public void initTableConfig() {
        tableConfig = new DefaultTableConfig();
        tableConfig.setColumns(initColumnConfig(configListSize));
        tableConfig.setTotalCount(totalCount);
        tableConfig.setStrategy(DuplicateStrategy.IGNORE);
        tableConfig.setBatchSize(batchSize);
        tableConfig.setWhetherTruncate(true);
        tableConfig.setTableName(tableName);
        tableConfig.setSchemaName(schemaName);
    }

    @Test
    public void testTableConfig() {
        Assert.assertEquals(batchSize, tableConfig.maxBatchSize());
        Assert.assertEquals(totalCount, tableConfig.getTotalCount());
        Assert.assertEquals(tableName, tableConfig.tableName());
        Assert.assertEquals(schemaName, tableConfig.schemaName());
        Assert.assertNotNull(tableConfig.columns());
        Assert.assertEquals(configListSize, tableConfig.columns().size());
        Assert.assertEquals(DuplicateStrategy.IGNORE, tableConfig.duplicateStrategy());
        Assert.assertTrue(tableConfig.truncated());

    }

    @Test(expected = MockerException.class)
    public void testTableConfigWithIllegalBatchSize() {
        tableConfig.setBatchSize(-100L);
        Assert.assertEquals(batchSize, tableConfig.maxBatchSize());
    }

    @Test(expected = MockerException.class)
    public void testTableConfigWithIllegalBatchSize1() {
        tableConfig.setBatchSize(100001L);
        Assert.assertEquals(batchSize, tableConfig.maxBatchSize());
    }

    @Test(expected = MockerException.class)
    public void testTableConfigWithIllegalMaxSize() {
        tableConfig.setTotalCount(-100L);
        Assert.assertEquals(totalCount, tableConfig.maxCount());
    }

    @Test(expected = MockerException.class)
    public void testTableConfigWithIllegalMaxSize1() {
        tableConfig.setTotalCount(1000001L);
        Assert.assertEquals(totalCount, tableConfig.maxCount());
    }

    @Test(expected = MockerException.class)
    public void testTableConfigWithNullBatchSize() {
        tableConfig.setBatchSize(null);
        Assert.assertEquals(batchSize, tableConfig.maxBatchSize());
    }

    @Test(expected = MockerException.class)
    public void testTableConfigWithNullMaxCount() {
        tableConfig.setTotalCount(null);
        Assert.assertEquals(totalCount, tableConfig.maxCount());
    }

    @After
    public void clearParams() {
        builderParams.clear();
    }
}
