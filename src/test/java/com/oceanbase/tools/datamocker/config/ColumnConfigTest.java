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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleCharType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.generator.GeneratorFactory;
import com.oceanbase.tools.datamocker.generator.chartype.RandomGenerator;
import com.oceanbase.tools.datamocker.generator.digit.UniformGenerator;
import com.oceanbase.tools.datamocker.model.config.impl.DefaultColumnConfig;
import com.oceanbase.tools.datamocker.model.config.model.CharDataTypeConfig;
import com.oceanbase.tools.datamocker.model.config.model.DigitDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * The test class of the configuration object of the column generation task
 *
 * @author yh263208
 * @date 2020-12-25 17:36
 * @since OBMOCKER-snapshot-0.1.0
 */
public class ColumnConfigTest extends MockerTestBase {
    private final Integer precision = 5;
    private final Integer scale = 2;
    private final String columnName = "SALARY";
    private final Object defaultValue = "123.55";
    private final BigDecimal lowValue = BigDecimal.ZERO;
    private final BigDecimal highValue = BigDecimal.TEN.multiply(BigDecimal.TEN);
    private final Map<String, Object> builderParams = new HashMap<>();
    private DefaultColumnConfig config = null;
    private final int length = 128;

    private DigitDataTypeConfig initDigitConfig() {
        DigitDataTypeConfig digit = new DigitDataTypeConfig();
        String typeName = "OB_ORACLE_NUMBER";
        digit.setColumnType(typeName);
        digit.setLowValue(lowValue);
        digit.setHighValue(highValue);
        digit.setGenParams(builderParams);
        String genName = "NORMAL_GENERATOR";
        digit.setGenerator(genName);
        digit.setPrecision(precision);
        digit.setScale(scale);
        return digit;
    }

    private CharDataTypeConfig initCharConfig() {
        CharDataTypeConfig charConfig = new CharDataTypeConfig();
        charConfig.setColumnType("OB_ORACLE_CHAR");
        charConfig.setGenerator("RANDOM_GENERATOR");
        charConfig.setWidth(length);
        charConfig.setLowValue(12);
        charConfig.setHighValue(15);
        charConfig.setCharset("UTF_8");
        return charConfig;
    }

    @Before
    public void initColumnConfig() {
        builderParams.put("average", 50.21);
        builderParams.put("variance", 16.43);
        config = new DefaultColumnConfig();
        config.setDefaultValue(defaultValue);
        Boolean allowNull = false;
        config.setAllowNull(allowNull);
        config.setColumnName(columnName);
    }

    @Test
    public void testDigitColumnConfig() {
        DigitDataTypeConfig typeConfig = initDigitConfig();
        config.setTypeConfig(typeConfig);
        Assert.assertEquals(columnName, config.columnName());
        Assert.assertFalse(config.allowNull());
        Assert.assertEquals(defaultValue, config.defaultValue());
        OracleNumberType expect = new OracleNumberType(precision, scale, null, false);
        expect.bind((UniformGenerator) GeneratorFactory.getInstance("UNIFORM_GENERATOR").make(null));
        AbstractDataType<?, ? extends Comparable<?>> real = config.columnType();
        Assert.assertEquals(expect, real);
        BigDecimal result = new BigDecimal("0");
        int totalCount = 1000;
        for (int i = 0; i < totalCount; i++) {
            result = result.add((BigDecimal) real.acquire());
        }
        result = result.divide(new BigDecimal(Double.toString(totalCount)), BigDecimal.ROUND_DOWN);
        result = result.subtract(new BigDecimal(builderParams.get("average").toString())).abs();
        Assert.assertTrue(result.doubleValue() < 1);
    }

    @Test
    public void testCharColumnConfig() {
        CharDataTypeConfig typeConfig = initCharConfig();
        config.setTypeConfig(typeConfig);
        Assert.assertEquals(columnName, config.columnName());
        Assert.assertFalse(config.allowNull());
        Assert.assertEquals(defaultValue, config.defaultValue());
        OracleCharType expect = new OracleCharType(length, null, false, CharsetType.UTF_8, false);
        expect.bind((RandomGenerator) GeneratorFactory.getInstance("RANDOM_GENERATOR").make(builderParams));
        AbstractDataType<?, ? extends Comparable<?>> real = config.columnType();
        Assert.assertEquals(expect, real);
        Map<Integer, Integer> result = new HashMap<>();
        int totalCount = 10000;
        for (int i = 0; i < totalCount; i++) {
            String tmp = (String) real.acquire();
            int length = tmp.getBytes().length;
            Assert.assertTrue(length >= 12);
            Assert.assertTrue(length <= 15);
            Integer count = result.getOrDefault(length, 0);
            result.put(length, count + 1);
        }
        Set<Entry<Integer, Integer>> entrySet = result.entrySet();
        int validateCount = totalCount / 4;
        for (Map.Entry<Integer, Integer> entry : entrySet) {
            Assert.assertTrue(entry.getValue() - validateCount <= 300);
        }
    }

    @After
    public void clearParams() {
        builderParams.clear();
    }

}
