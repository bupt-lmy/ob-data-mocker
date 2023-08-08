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
package com.oceanbase.tools.datamocker.datatype.oracle;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.generator.digit.NormalGenerator;
import com.oceanbase.tools.datamocker.generator.digit.PoissonGenerator;
import com.oceanbase.tools.datamocker.generator.digit.StepGenerator;
import com.oceanbase.tools.datamocker.generator.digit.UniformGenerator;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class of Number data type in oracle mode
 *
 * @author yh263208
 * @date 2020-12-11 19:59
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NumberTest {

    @Test
    public void testMinAndMaxValueForNumber() {
        OracleNumberType number = new OracleNumberType(5, 3, new UniformGenerator(), null, false);
        Assert.assertEquals(0, number.lowValue().compareTo(new BigDecimal("-99.9995")));
        Assert.assertEquals(0, number.highValue().compareTo(new BigDecimal("99.9995")));
        int minValue = -10;
        int maxValue = 20;
        number.setLowValue(new BigDecimal(minValue));
        number.setHighValue(new BigDecimal(maxValue));
        Assert.assertEquals(new BigDecimal(minValue), number.lowValue());
        Assert.assertEquals(new BigDecimal(maxValue), number.highValue());
    }

    @Test
    public void testNormalGeneratorForNumber() {
        OracleNumberType number = new OracleNumberType(5, 3, new NormalGenerator(), null, false);
        Assert.assertEquals(0, number.lowValue().compareTo(new BigDecimal("-99.9995")));
        Assert.assertEquals(0, number.highValue().compareTo(new BigDecimal("99.9995")));
        int minValue = -10;
        int maxValue = 20;
        number.setLowValue(new BigDecimal(minValue));
        number.setHighValue(new BigDecimal(maxValue));
        Assert.assertEquals(new BigDecimal(minValue), number.lowValue());
        Assert.assertEquals(new BigDecimal(maxValue), number.highValue());
        for (int i = 0; i < 10000; i++) {
            Assert.assertNotEquals(1, new BigDecimal(minValue).compareTo(number.acquire()));
            Assert.assertNotEquals(-1, new BigDecimal(maxValue).compareTo(number.acquire()));
        }
    }

    @Test
    public void testNormalPoissonForNumber() {
        OracleNumberType number = new OracleNumberType(5, 3, new PoissonGenerator(15), null, false);
        Assert.assertEquals(0, number.lowValue().compareTo(new BigDecimal("-99.9995")));
        Assert.assertEquals(0, number.highValue().compareTo(new BigDecimal("99.9995")));
        int minValue = 0;
        int maxValue = 20;
        number.setLowValue(new BigDecimal(minValue));
        number.setHighValue(new BigDecimal(maxValue));
        Assert.assertEquals(new BigDecimal(minValue), number.lowValue());
        Assert.assertEquals(new BigDecimal(maxValue), number.highValue());
        for (int i = 0; i < 10000; i++) {
            Assert.assertNotEquals(1, new BigDecimal(minValue).compareTo(number.acquire()));
            Assert.assertNotEquals(-1, new BigDecimal(maxValue).compareTo(number.acquire()));
        }
    }

    @Test
    public void testNormalStepForNumber() {
        OracleNumberType number = new OracleNumberType(5, 3, new StepGenerator(1.3, true), null, false);
        Assert.assertEquals(0, number.lowValue().compareTo(new BigDecimal("-99.9995")));
        Assert.assertEquals(0, number.highValue().compareTo(new BigDecimal("99.9995")));
        int minValue = 0;
        int maxValue = 20;
        number.setLowValue(new BigDecimal(minValue));
        number.setHighValue(new BigDecimal(maxValue));
        Assert.assertEquals(new BigDecimal(minValue), number.lowValue());
        Assert.assertEquals(new BigDecimal(maxValue), number.highValue());
        for (int i = 0; i < 10000; i++) {
            Assert.assertNotEquals(1, new BigDecimal(minValue).compareTo(number.acquire()));
            Assert.assertNotEquals(-1, new BigDecimal(maxValue).compareTo(number.acquire()));
        }
    }

    @Test(expected = MockerException.class)
    public void testNormalStepForNumberWitoutRoundOn() {
        OracleNumberType number = new OracleNumberType(5, 3, new StepGenerator(1.3), null, false);
        Assert.assertEquals(0, number.lowValue().compareTo(new BigDecimal("-99.9995")));
        Assert.assertEquals(0, number.highValue().compareTo(new BigDecimal("99.9995")));
        int minValue = 0;
        int maxValue = 20;
        number.setLowValue(new BigDecimal(minValue));
        number.setHighValue(new BigDecimal(maxValue));
        Assert.assertEquals(new BigDecimal(minValue), number.lowValue());
        Assert.assertEquals(new BigDecimal(maxValue), number.highValue());
        for (int i = 0; i < 10000; i++) {
            Assert.assertNotEquals(1, new BigDecimal(minValue).compareTo(number.acquire()));
            Assert.assertNotEquals(-1, new BigDecimal(maxValue).compareTo(number.acquire()));
        }
    }

    @Test
    public void testNormalUniformForNumber() {
        OracleNumberType number = new OracleNumberType(5, 3, new UniformGenerator(), null, false);
        Assert.assertEquals(0, number.lowValue().compareTo(new BigDecimal("-99.9995")));
        Assert.assertEquals(0, number.highValue().compareTo(new BigDecimal("99.9995")));
        int minValue = 0;
        int maxValue = 20;
        number.setLowValue(new BigDecimal(minValue));
        number.setHighValue(new BigDecimal(maxValue));
        Assert.assertEquals(new BigDecimal(minValue), number.lowValue());
        Assert.assertEquals(new BigDecimal(maxValue), number.highValue());
        for (int i = 0; i < 10000; i++) {
            Assert.assertNotEquals(1, new BigDecimal(minValue).compareTo(number.acquire()));
            Assert.assertNotEquals(-1, new BigDecimal(maxValue).compareTo(number.acquire()));
        }
    }
}
