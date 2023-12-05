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
package com.oceanbase.tools.datamocker.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.ObDataMocker;
import com.oceanbase.tools.datamocker.ObMockerFactory;
import com.oceanbase.tools.datamocker.core.DataSourceFactory;
import com.oceanbase.tools.datamocker.model.config.MockTaskConfig;
import com.oceanbase.tools.datamocker.model.config.MockColumnConfig;
import com.oceanbase.tools.datamocker.model.config.MockTableConfig;
import com.oceanbase.tools.datamocker.model.config.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.config.DataTypeConfig;
import com.oceanbase.tools.datamocker.model.config.DigitDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.DuplicateStrategy;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for dispatcher factory class
 *
 * @author yh263208
 * @date 2021-01-11 21:50
 * @since OBMOCKER_snaoshot_0.1.0
 */
public class DispatcherFactoryTest extends MockerTestBase {

    private static final String ddlOracle = "CREATE TABLE \"EMP\" (\n"
            + "  \"COL\" NUMBER(5,2) NOT NULL,\n"
            + "  \"COL2\" NUMBER(5,2) NOT NULL,\n"
            + "  \"COL3\" NUMBER(4,2) NOT NULL,\n"
            + "  CONSTRAINT \"EMP_OBPK_1610357443362979\" PRIMARY KEY (\"COL\"),\n"
            + "  CONSTRAINT \"EMP_OBUNIQUE_1610357443363981\" UNIQUE (\"COL2\", "
            + "\"COL3\")\n"
            + ") ";
    private static final String ddlWithVirtualColumnOracle = "CREATE TABLE \"EMP1\" (\n"
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

    @Before
    public void initEnv() throws SQLException {
        if (oracleDatasource == null) {
            DataBaseConfig config = getOracleConfig();
            oracleDatasource = new DataSourceFactory(config).generate();
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
    public void create_normalInput_createSucceed() {
        ObDataMocker mocker = new ObMockerFactory(getTask("EMP")).create();
        Assert.assertEquals(1, mocker.size());
    }

    @Test
    public void create_tableWithFk_expThrown() {
        thrown.expectMessage("Foreign constraint is not support yet");
        thrown.expect(MockerException.class);
        new ObMockerFactory(getTask("EMP1")).create();
    }

    @After
    public void clearEnv() throws Exception {
        try (Connection connection = oracleDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("drop table emp1");
                statement.execute("drop table emp");
            }
        }
        if (oracleDatasource instanceof AutoCloseable) {
            ((AutoCloseable) oracleDatasource).close();
        }
    }

    private DataTypeConfig initDigitGen(Map<String, Object> builderParams,
            BigDecimal highValue, Integer precision, Integer scale) {
        DigitDataTypeConfig digit = new DigitDataTypeConfig();
        digit.setColumnType("OB_ORACLE_NUMBER");
        digit.setLowValue(BigDecimal.ZERO);
        digit.setHighValue(highValue);
        digit.setGenParams(builderParams);
        digit.setGenerator("NORMAL_GENERATOR");
        digit.setPrecision(precision);
        digit.setScale(scale);
        return digit;
    }

    private List<MockColumnConfig> initColumnConfig(String tableName) {
        List<MockColumnConfig> configList = new ArrayList<>();
        if ("EMP".equals(tableName)) {
            Map<String, Object> builderParams = new HashMap<>();
            builderParams.put("average", 50.21);
            builderParams.put("variance", 16.43);

            MockColumnConfig col1 = new MockColumnConfig();
            col1.setColumnName("COL");
            col1.setAllowNull(false);
            col1.setDefaultValue(null);
            col1.setTypeConfig(initDigitGen(builderParams,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    5, 2));
            configList.add(col1);

            MockColumnConfig col2 = new MockColumnConfig();
            col2.setColumnName("COL2");
            col2.setAllowNull(false);
            col2.setDefaultValue(null);
            col2.setTypeConfig(initDigitGen(builderParams,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    5, 2));
            configList.add(col2);

            MockColumnConfig col3 = new MockColumnConfig();
            col3.setColumnName("COL3");
            col3.setAllowNull(false);
            col3.setDefaultValue(null);
            col3.setTypeConfig(initDigitGen(builderParams,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    4, 2));
            configList.add(col3);
        } else if ("EMP1".equals(tableName)) {
            Map<String, Object> builderParams = new HashMap<>();
            builderParams.put("average", 50.21);
            builderParams.put("variance", 16.43);

            MockColumnConfig col1 = new MockColumnConfig();
            col1.setColumnName("COL");
            col1.setAllowNull(false);
            col1.setDefaultValue(null);
            col1.setTypeConfig(initDigitGen(builderParams,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    5, 2));
            configList.add(col1);

            MockColumnConfig col2 = new MockColumnConfig();
            col2.setColumnName("COL2");
            col2.setAllowNull(false);
            col2.setDefaultValue(null);
            col2.setTypeConfig(initDigitGen(builderParams,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    5, 2));
            configList.add(col2);

            MockColumnConfig col3 = new MockColumnConfig();
            col3.setColumnName("COL3");
            col3.setAllowNull(false);
            col3.setDefaultValue(null);
            col3.setTypeConfig(initDigitGen(builderParams,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    4, 2));
            configList.add(col3);

            MockColumnConfig col4 = new MockColumnConfig();
            col4.setColumnName("COL4");
            col4.setAllowNull(false);
            col4.setDefaultValue(null);
            col4.setTypeConfig(initDigitGen(builderParams,
                    BigDecimal.TEN.multiply(BigDecimal.TEN).subtract(BigDecimal.ONE),
                    5, 3));
            configList.add(col4);
        }
        return configList;
    }

    private MockTableConfig initTableConfig(String tableName, String schemaName) {
        MockTableConfig tableConfig = new MockTableConfig();
        tableConfig.setColumns(initColumnConfig(tableName));
        Long maxGenerateCount = 9800L;
        tableConfig.setTotalCount(maxGenerateCount);
        tableConfig.setStrategy(DuplicateStrategy.IGNORE);
        Long maxBatchsize = 1024L;
        tableConfig.setBatchSize(maxBatchsize);
        tableConfig.setWhetherTruncate(true);
        tableConfig.setTableName(tableName);
        tableConfig.setSchemaName(schemaName);
        tableConfig.setLocation("test/mock/test.txt");
        tableConfig.setTimeoutMillis(180000L);
        return tableConfig;
    }

    private MockTaskConfig getTask(String tableName) {
        DataBaseConfig config = getOracleConfig();
        MockTaskConfig taskConfig = new MockTaskConfig();
        MockTableConfig tableConfig = initTableConfig(tableName, config.getDefaultSchame());
        taskConfig.setTables(Collections.singletonList(tableConfig));
        taskConfig.setDbConfig(config);
        taskConfig.setDialectType(ObModeType.OB_ORACLE);
        taskConfig.setMaxConnectionSize(15);
        return taskConfig;
    }

}
