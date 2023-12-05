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
import com.oceanbase.tools.datamocker.model.config.MockColumnConfig;
import com.oceanbase.tools.datamocker.model.config.MockTableConfig;
import com.oceanbase.tools.datamocker.model.config.MockTaskConfig;
import com.oceanbase.tools.datamocker.model.config.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.config.DataTypeConfig;
import com.oceanbase.tools.datamocker.model.config.DigitDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.DuplicateStrategy;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Overall build task configuration
 *
 * @author yh263208
 * @date 2020-12-27 20:45
 * @since OBMOCKER-snapshot-0.1.0
 */
public class MockTaskConfigTest extends MockerTestBase {
    private final Object defaultValue = "DEFAULT_VALUE";
    private final BigDecimal lowValue = BigDecimal.ZERO;
    private final BigDecimal highValue = BigDecimal.TEN.multiply(BigDecimal.TEN);
    private final Map<String, Object> builderParams = new HashMap<>();
    /**
     * Table task-related parameters
     */
    private final int configListSize = 3;
    private MockTaskConfig taskConfig = null;
    private DataBaseConfig dbConfig = null;

    /**
     * Initialize a numeric data generator configuration
     */
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
    private List<MockColumnConfig> initColumnConfig(int size) {
        builderParams.put("average", 50.21);
        builderParams.put("variance", 16.43);
        List<MockColumnConfig> configList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            MockColumnConfig config = new MockColumnConfig();
            String columnName = "SALARY";
            config.setColumnName(columnName);
            Boolean allowNull = false;
            config.setAllowNull(allowNull);
            config.setDefaultValue(defaultValue);
            config.setTypeConfig(initDigitGen());
            configList.add(config);
        }
        return configList;
    }

    private List<MockTableConfig> initTableConfig(int size) {
        List<MockTableConfig> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            MockTableConfig tableConfig = new MockTableConfig();
            tableConfig.setColumns(initColumnConfig(size));
            Long maxGenerateCount = 1000000L;
            tableConfig.setTotalCount(maxGenerateCount);
            tableConfig.setStrategy(DuplicateStrategy.IGNORE);
            Long maxBatchsize = 1024L;
            tableConfig.setBatchSize(maxBatchsize);
            tableConfig.setWhetherTruncate(true);
            String tableName = "test_table";
            tableConfig.setTableName(tableName);
            String schemaName = "schema_name";
            tableConfig.setSchemaName(schemaName);
            list.add(tableConfig);
        }
        return list;
    }

    @Before
    public void initEnv() {
        dbConfig = new DataBaseConfig();
        String host = "xxx.xxx.xxx.xxx";
        dbConfig.setHost(host);
        String cluster = "test_cluster";
        dbConfig.setCluster(cluster);
        String defaultSchema = "defaule_schame";
        dbConfig.setDefaultSchame(defaultSchema);
        String passwd = "test_passwd";
        dbConfig.setPassword(passwd);
        /**
         * Parameters related to the overall task
         */
        Integer port = 3306;
        dbConfig.setPort(port);
        dbConfig.setDefaultSchame(defaultSchema);
        String user = "test_user";
        dbConfig.setUser(user);
        String tenant = "test_tenant";
        dbConfig.setTenant(tenant);

        taskConfig = new MockTaskConfig();
        taskConfig.setTables(initTableConfig(configListSize));
        taskConfig.setDbConfig(dbConfig);
        taskConfig.setDialectType(ObModeType.OB_ORACLE);
    }

    @Test
    public void testTaskConfig() {
        Assert.assertEquals(dbConfig, taskConfig.getDbConfig());
        Assert.assertEquals(ObModeType.OB_ORACLE, taskConfig.getDialectType());
        Assert.assertNotNull(taskConfig.getTables());
        Assert.assertEquals(configListSize, taskConfig.getTables().size());
        Assert.assertNull(taskConfig.getTaskName());
    }

    @After
    public void clearParams() {
        builderParams.clear();
    }
}
