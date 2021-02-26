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
 * oracle模式下Number数据类型的测试类
 *
 * @author yh263208
 * @date 2020-12-11 19:59
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NumberTest {
    /**
     * 测试正向逻辑，测试针对number对象给定精度以及有效位数时返回的结果是否符合预期
     */
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

    /**
     * 测试正向逻辑，测试针对number对象给定精度以及有效位数在数据生成器生成数据的情况下是否生成的数据符合范围
     */
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

    /**
     * 测试正向逻辑，测试针对number对象给定精度以及有效位数在数据生成器生成数据的情况下是否生成的数据符合范围
     */
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

    /**
     * 测试正向逻辑，测试针对number对象给定精度以及有效位数在数据生成器生成数据的情况下是否生成的数据符合范围
     */
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

    /**
     * 测试逆向逻辑，stepGenerator数据生成器不开轮转的情况下会抛出异常
     */
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

    /**
     * 测试正向逻辑，测试针对number对象给定精度以及有效位数在数据生成器生成数据的情况下是否生成的数据符合范围
     */
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
