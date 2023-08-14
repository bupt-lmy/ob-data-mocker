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
 * Normally distributed data generator test
 *
 * @author yh263208
 * @date 2020-12-18 15:57
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NormalGeneratorTest {

    @Test
    public void testStandardNormalGenerator() {
        NormalGenerator generator = new NormalGenerator();
        BigDecimal minValue = new BigDecimal("-100");
        BigDecimal maxValue = new BigDecimal("100");
        Long count = generator.count(minValue, maxValue);
        Assert.assertNull(count);
        BigDecimal result = new BigDecimal("0");
        int totalCount = 1000;
        for (int i = 0; i < totalCount; i++) {
            result = result.add(generator.generate(minValue, maxValue));
        }
        result = result.divide(new BigDecimal(Double.toString(totalCount)), BigDecimal.ROUND_DOWN);
        result = result.subtract(new BigDecimal("0")).abs();
        Assert.assertTrue(result.doubleValue() < 0.08);
    }

    @Test(expected = MockerException.class)
    public void testAverageSmallerThanBound() {
        NormalGenerator generator = new NormalGenerator(300, 20);
        BigDecimal minValue = new BigDecimal("-100");
        BigDecimal maxValue = new BigDecimal("100");
        Long count = generator.count(minValue, maxValue);
        generator.preCheck(minValue, maxValue);
        Assert.assertNull(count);
        for (int i = 0; i < 1000; i++) {
            generator.generate(minValue, maxValue);
        }
    }

    @Test(expected = MockerException.class)
    public void testAverageBiggerThanBound() {
        NormalGenerator generator = new NormalGenerator(-101, 20);
        BigDecimal minValue = new BigDecimal("-100");
        BigDecimal maxValue = new BigDecimal("100");
        Long count = generator.count(minValue, maxValue);
        generator.preCheck(minValue, maxValue);
        Assert.assertNull(count);
        for (int i = 0; i < 1000; i++) {
            generator.generate(minValue, maxValue);
        }
    }
}
