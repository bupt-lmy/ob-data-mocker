package com.oceanbase.tools.datamocker.datatype.mysql;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.generator.digit.NormalGenerator;
import com.oceanbase.tools.datamocker.generator.digit.PoissonGenerator;
import com.oceanbase.tools.datamocker.generator.digit.StepGenerator;
import com.oceanbase.tools.datamocker.generator.digit.UniformGenerator;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Decimal type test class in mysql mode
 *
 * @author yh263208
 * @date 2020-12-16 15:19
 * @since OBMOCKER_snapshot_0.1.0
 */
public class DecimalTest extends MockerTestBase {
    /**
     * Test the forward logic, test whether the result returned when
     * the precision and effective digits are given for the decimal object meets expectations
     */
    @Test
    public void testMinAndMaxValueForDecimal() {
        MysqlDecimalType decimal = new MysqlDecimalType(5, 3, new UniformGenerator(), null, false, true);
        Assert.assertEquals(0, decimal.lowValue().compareTo(new BigDecimal("-99.9995")));
        Assert.assertEquals(0, decimal.highValue().compareTo(new BigDecimal("99.9995")));
        int minValue = -10;
        int maxValue = 20;
        decimal.setLowValue(new BigDecimal(minValue));
        decimal.setHighValue(new BigDecimal(maxValue));
        Assert.assertEquals(new BigDecimal(minValue), decimal.lowValue());
        Assert.assertEquals(new BigDecimal(maxValue), decimal.highValue());
    }

    @Test
    public void testNormalGeneratorForDecimal() {
        MysqlDecimalType number = new MysqlDecimalType(5, 3, new NormalGenerator(), null, false, true);
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
    public void testNormalPoissonForDecimal() {
        MysqlDecimalType number = new MysqlDecimalType(5, 3, new PoissonGenerator(15), null, false, true);
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
    public void testNormalStepForDecimal() {
        MysqlDecimalType number = new MysqlDecimalType(5, 3, new StepGenerator(1.3, true), null, false, true);
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
    public void testUnsignedDecimal() {
        MysqlDecimalType decimal = new MysqlDecimalType(5, 3, new UniformGenerator(), null, false, false);
        Assert.assertEquals(0, decimal.lowValue().compareTo(new BigDecimal("0")));
        Assert.assertEquals(0, decimal.highValue().compareTo(new BigDecimal("99.9995")));
        Assert.assertTrue(decimal.distinctLimit() == 99999L);
        int minValue = -10;
        int maxValue = 20;
        thrown.expectMessage("Max or min value -10 for data type decimal(5, 3) is out of range [0,99.99950]");
        thrown.expect(MockerException.class);
        decimal.setLowValue(new BigDecimal(minValue));
        decimal.setHighValue(new BigDecimal(maxValue));
    }

    @Test
    public void testUnsignedDecimalWithErrInput() {
        thrown.expectMessage("Precision for decaimal can not larger than 65 or smaller than 0");
        thrown.expect(MockerException.class);
        new MysqlDecimalType(-1, 3, new UniformGenerator(), null, false, false);
    }

    @Test
    public void testUnsignedDecimalWithErrInput1() {
        thrown.expectMessage("Scale for decimal can not larger than 30 or smaller than 0");
        thrown.expect(MockerException.class);
        new MysqlDecimalType(1, -3, new UniformGenerator(), null, false, false);
    }

    @Test
    public void testUnsignedDecimalWithErrInput2() {
        thrown.expectMessage("Scale can not be bigger than precision");
        thrown.expect(MockerException.class);
        new MysqlDecimalType(5, 8, new UniformGenerator(), null, false, false);
    }
}
