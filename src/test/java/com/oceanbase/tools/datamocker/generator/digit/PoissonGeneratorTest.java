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
 * Poisson distribution data generator test class
 *
 * @author yh263208
 * @date 2020-12-11 19:27
 * @since OBMOCKER_snapshot_0.1.0
 */
public class PoissonGeneratorTest {

    @Test
    public void testStandardPoissonGenerator() {
        double lambda = 100;
        PoissonGenerator generator = new PoissonGenerator(lambda);
        BigDecimal minValue = new BigDecimal("0");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        Assert.assertNull(count);
        BigDecimal result = new BigDecimal("0");
        int totalCount = 1000;
        for (int i = 0; i < totalCount; i++) {
            result = result.add(generator.generate(minValue, maxValue));
        }
        result = result.divide(new BigDecimal(Double.toString(totalCount)));
        result = result.subtract(new BigDecimal(Double.toString(lambda))).abs();
        Assert.assertTrue(result.doubleValue() < 1.0);
    }

    @Test(expected = MockerException.class)
    public void testAverageSmallerThanBound() {
        double lambda = 501;
        PoissonGenerator generator = new PoissonGenerator(lambda);
        BigDecimal minValue = new BigDecimal("0");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        generator.preCheck(minValue, maxValue);
        Assert.assertNull(count);
        for (int i = 0; i < 1000; i++) {
            generator.generate(minValue, maxValue);
        }
    }

    @Test(expected = MockerException.class)
    public void testAverageBiggerThanBound() {
        double lambda = -100;
        PoissonGenerator generator = new PoissonGenerator(lambda);
        BigDecimal minValue = new BigDecimal("0");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        generator.preCheck(minValue, maxValue);
        Assert.assertNull(count);
        for (int i = 0; i < 1000; i++) {
            generator.generate(minValue, maxValue);
        }
    }

    @Test(expected = MockerException.class)
    public void testMinValueIllegal() {
        double lambda = -100;
        PoissonGenerator generator = new PoissonGenerator(lambda);
        BigDecimal minValue = new BigDecimal("20");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        generator.preCheck(minValue, maxValue);
        Assert.assertNull(count);
        for (int i = 0; i < 1000; i++) {
            generator.generate(minValue, maxValue);
        }
    }
}
