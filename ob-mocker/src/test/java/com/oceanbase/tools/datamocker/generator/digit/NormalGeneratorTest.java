package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.junit.Assert;
import org.junit.Test;

/**
 * 正态分布数据生成器测试
 *
 * @author yh263208
 * @date 2020-12-18 15:57
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NormalGeneratorTest {

    /**
     * 正向逻辑，计算一个01标准正态分布，观察均值和预期是否在一个标准差范围内
     */
    @Test
    public void testStandardNormalGenerator() {
        NormalGenerator generator = new NormalGenerator();
        BigDecimal minValue = new BigDecimal("-100");
        BigDecimal maxValue = new BigDecimal("100");
        Long count = generator.count(minValue, maxValue);
        Assert.assertEquals(null, count);
        BigDecimal result = new BigDecimal("0");
        int totalCount = 1000;
        for (int i = 0; i < totalCount; i++) {
            result = result.add(generator.generate(minValue, maxValue));
        }
        result = result.divide(new BigDecimal(Double.toString(totalCount)), BigDecimal.ROUND_DOWN);
        result = result.subtract(new BigDecimal("0")).abs();
        Assert.assertEquals(true, result.doubleValue() < 0.08);
    }

    /**
     * 逆向逻辑测试，给定一个超出范围的平均值，预期应该报错
     */
    @Test(expected = MockerException.class)
    public void testAverageSmallerThanBound() {
        NormalGenerator generator = new NormalGenerator(300, 20);
        BigDecimal minValue = new BigDecimal("-100");
        BigDecimal maxValue = new BigDecimal("100");
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
        NormalGenerator generator = new NormalGenerator(-101, 20);
        BigDecimal minValue = new BigDecimal("-100");
        BigDecimal maxValue = new BigDecimal("100");
        Long count = generator.count(minValue, maxValue);
        generator.preCheck(minValue, maxValue);
        Assert.assertEquals(null, count);
        for (int i = 0; i < 1000; i++) {
            generator.generate(minValue, maxValue);
        }
    }
}
