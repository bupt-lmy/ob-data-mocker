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
package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for data generator by step
 *
 * @author yh263208
 * @date 2020-12-11 19:36
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepGeneratorTest {

    @Test
    public void testStandardIncreaseStepGenerator() {
        StepGenerator generator = new StepGenerator(0.3);
        BigDecimal minValue = new BigDecimal("0");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        Assert.assertEquals(1666, count.intValue());
        for (; count > 0; count--) {
            generator.generate(minValue, maxValue);
        }
        Assert.assertEquals(0, count.intValue());
    }

    @Test
    public void testExcessiveDataForIncreaseStepGenerator() {
        StepGenerator generator = new StepGenerator(0.3);
        BigDecimal minValue = new BigDecimal("0");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        Assert.assertEquals(1666, count.intValue());
        for (count += 1; count > 0; count--) {
            generator.generate(minValue, maxValue);
        }
    }

    @Test
    public void testStandardReduceStepGenerator() {
        StepGenerator generator = new StepGenerator(-0.3);
        BigDecimal minValue = new BigDecimal("0");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        Assert.assertEquals(1666, count.intValue());
        for (; count > 0; count--) {
            generator.generate(minValue, maxValue);
        }
        Assert.assertEquals(0, count.intValue());
    }

    @Test
    public void testExcessiveDataForReduceStepGenerator() {
        StepGenerator generator = new StepGenerator(-0.3);
        BigDecimal minValue = new BigDecimal("0");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        Assert.assertEquals(1666, count.intValue());
        for (count += 1; count > 0; count--) {
            generator.generate(minValue, maxValue);
        }
    }

    @Test
    public void testStandardReduceStepGeneratorWithRound() {
        StepGenerator generator = new StepGenerator(-0.3, true);
        BigDecimal minValue = new BigDecimal("0");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        Assert.assertEquals(1666, count.intValue());
        for (count += 100; count > 0; count--) {
            generator.generate(minValue, maxValue);
        }
        Assert.assertEquals(0, count.intValue());
    }

    @Test
    public void testStandardIncreaseStepGeneratorWithRound() {
        StepGenerator generator = new StepGenerator(0.3, true);
        BigDecimal minValue = new BigDecimal("0");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        Assert.assertEquals(1666, count.intValue());
        for (count += 100; count > 0; count--) {
            generator.generate(minValue, maxValue);
        }
        Assert.assertEquals(0, count.intValue());
    }

    @Test(expected = MockerException.class)
    public void testStandardStepGeneratorWithZeroStep() {
        StepGenerator generator = new StepGenerator(0);
    }

    @Test(expected = MockerException.class)
    public void testStandardStepGeneratorWithZeroStepWithRoundOn() {
        StepGenerator generator = new StepGenerator(0, true);
    }
}
