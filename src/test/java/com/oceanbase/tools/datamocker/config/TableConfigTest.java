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
    private final Object defaultValue = "DEFAULT_VALUE";
    private final BigDecimal lowValue = BigDecimal.ZERO;
    private final BigDecimal highValue = BigDecimal.TEN.multiply(BigDecimal.TEN);
    private final Map<String, Object> builderParams = new HashMap<>();
    /**
     * Table task-related initialization parameters
     */
    private final int configListSize = 3;
    private final Long batchSize = 1024L;
    private final Long totalCount = 1000000L;
    private final String tableName = "test_table";
    private final String schemaName = "schema_name";
    private DefaultTableConfig tableConfig = null;

    private DataTypeConfig initDigitGen() {
        DigitDataTypeConfig digit = new DigitDataTypeConfig();
        String typeName = "OB_ORACLE_NUMBER";
        digit.setColumnType(typeName);
        digit.setLowValue(lowValue);
        digit.setHighValue(highValue);
        digit.setGenParams(builderParams);
        String genName = "NORMAL_GENERATOR";
        digit.setGenerator(genName);
        /**
         * List task-related parameters
         */
        Integer precision = 5;
        digit.setPrecision(precision);
        Integer scale = 2;
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
            String columnName = "SALARY";
            config.setColumnName(columnName);
            Boolean allowNull = false;
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
