package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.junit.Assert;
import org.junit.Test;

/**
 * 泊松分布数据生成器测试类
 *
 * @author yh263208
 * @date 2020-12-11 19:27
 * @since OBMOCKER_snapshot_0.1.0
 */
public class PoissonGeneratorTest {
    /**
     * 正向逻辑，泊松分布，观察均值和预期是否在一定范围内
     */
    @Test
    public void testStandardPoissonGenerator() {
        double lambda = 100;
        PoissonGenerator generator = new PoissonGenerator(lambda);
        BigDecimal minValue = new BigDecimal("0");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        Assert.assertEquals(null, count);
        BigDecimal result = new BigDecimal("0");
        int totalCount = 1000;
        for (int i = 0; i < totalCount; i++) {
            result = result.add(generator.generate(minValue, maxValue));
        }
        result = result.divide(new BigDecimal(Double.toString(totalCount)));
        result = result.subtract(new BigDecimal(Double.toString(lambda))).abs();
        Assert.assertEquals(true, result.doubleValue() < 1.0);
    }

    /**
     * 逆向逻辑测试，给定一个超出范围的平均值，预期应该报错
     */
    @Test(expected = MockerException.class)
    public void testAverageSmallerThanBound() {
        double lambda = 501;
        PoissonGenerator generator = new PoissonGenerator(lambda);
        BigDecimal minValue = new BigDecimal("0");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        generator.preCheck(minValue, maxValue);
        Assert.assertEquals(null, count);
        for (int i = 0; i < 1000; i++) {
            generator.generate(minValue, maxValue);
        }
    }

    /**
     * 逆向逻辑测试，给定一个超出范围的平均值，预期应该报错
     */
    @Test(expected = MockerException.class)
    public void testAverageBiggerThanBound() {
        double lambda = -100;
        PoissonGenerator generator = new PoissonGenerator(lambda);
        BigDecimal minValue = new BigDecimal("0");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        generator.preCheck(minValue, maxValue);
        Assert.assertEquals(null, count);
        for (int i = 0; i < 1000; i++) {
            generator.generate(minValue, maxValue);
        }
    }

    /**
     * 逆向逻辑测试，给定一个超出范围的平均值，预期应该报错
     */
    @Test(expected = MockerException.class)
    public void testMinValueIllegal() {
        double lambda = -100;
        PoissonGenerator generator = new PoissonGenerator(lambda);
        BigDecimal minValue = new BigDecimal("20");
        BigDecimal maxValue = new BigDecimal("500");
        Long count = generator.count(minValue, maxValue);
        generator.preCheck(minValue, maxValue);
        Assert.assertEquals(null, count);
        for (int i = 0; i < 1000; i++) {
            generator.generate(minValue, maxValue);
        }
    }
}
