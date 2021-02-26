package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

/**
 * 标准均匀分布数据生成器测试类
 *
 * @author yh263208
 * @date 2020-12-11 19:54
 * @since OBMOCKER_snapshot_0.1.0
 */
public class UniformGeneratorTest {
    /**
     * 正向逻辑，计算一个标准的均匀分布，观察均值和预期是否在一个标准差范围内
     */
    @Test
    public void testStandardUniformGenerator() {
        UniformGenerator generator = new UniformGenerator();
        BigDecimal minValue = new BigDecimal("200");
        BigDecimal maxValue = new BigDecimal("300");
        Long count = generator.count(minValue, maxValue);
        Assert.assertEquals(null, count);
        BigDecimal result = new BigDecimal("0");
        int totalCount = 1000;
        for (int i = 0; i < totalCount; i++) {
            result = result.add(generator.generate(minValue, maxValue));
        }
        result = result.divide(new BigDecimal(Double.toString(totalCount)), BigDecimal.ROUND_DOWN);
        result = result.subtract(new BigDecimal("250")).abs();
        Assert.assertEquals(true, result.doubleValue() < 5);
    }
}