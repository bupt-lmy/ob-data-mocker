package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.junit.Assert;
import org.junit.Test;

/**
 * 按步长数据生成器测试类
 *
 * @author yh263208
 * @date 2020-12-11 19:36
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepGeneratorTest {
    /**
     * 正向逻辑，按照步长生成数据观察生成的数据量和预期是否相同
     */
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

    /**
     * 逆向逻辑，在没有开启轮转开关时产生过多数据造成异常
     */
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

    /**
     * 正向逻辑，按照步长生成数据观察生成的数据量和预期是否相同
     */
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

    /**
     * 逆向逻辑，在没有开启轮转开关时产生过多数据造成异常
     */
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

    /**
     * 正向逻辑，开启轮转按钮的时候按照步长生成数据观察生成的数据量和预期是否相同
     */
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

    /**
     * 正向逻辑，开启轮转按钮的时候按照步长生成数据观察生成的数据量和预期是否相同
     */
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

    /**
     * 逆向逻辑测试，step步长不能为0
     */
    @Test(expected = MockerException.class)
    public void testStandardStepGeneratorWithZeroStep() {
        StepGenerator generator = new StepGenerator(0);
    }

    /**
     * 逆向逻辑测试，开启轮转情况下step步长不能为0
     */
    @Test(expected = MockerException.class)
    public void testStandardStepGeneratorWithZeroStepWithRoundOn() {
        StepGenerator generator = new StepGenerator(0, true);
    }
}
