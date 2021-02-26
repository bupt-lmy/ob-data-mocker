package com.oceanbase.tools.datamocker.datatype.oracle;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.generator.chartype.RandomGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import org.junit.Assert;
import org.junit.Test;

/**
 * oracle模式下char类型的测试类
 *
 * @author yh263208
 * @date 2020-12-16 16:06
 * @since OBMOCKER_snapshot_0.1.0
 */
public class CharTest extends MockerTestBase {
    /**
     * 测试正向逻辑，测试针对char对象给定精度以及有效位数时返回的结果是否符合预期
     */
    @Test
    public void testMinAndMaxValueForChar() {
        OracleCharType charType = new OracleCharType(128, null, false, CharsetType.UTF_8, false);
        Assert.assertEquals(Integer.valueOf(1), charType.lowValue());
        Assert.assertEquals(Integer.valueOf(128), charType.highValue());
        Integer minValue = 32;
        Integer maxValue = 64;
        charType.setLowValue(minValue);
        charType.setHighValue(maxValue);
        Assert.assertEquals(minValue, charType.lowValue());
        Assert.assertEquals(maxValue, charType.highValue());
    }

    /**
     * 测试正向逻辑，测试针对char对象给定精度以及有效位数在数据生成器生成数据的情况下是否生成的数据符合范围
     */
    @Test
    public void testRandomGeneratorForChar() {
        RandomGenerator generator = new RandomGenerator(CharCaseOption.DEFAULT);
        OracleCharType charType = new OracleCharType(128, null, false, CharsetType.UTF_8, new RandomGenerator(CharCaseOption.DEFAULT),
                false);
        charType.bind(generator);
        Integer minValue = 32;
        Integer maxValue = 64;
        Assert.assertEquals(Long.MAX_VALUE, generator.count(minValue, maxValue).longValue());
        charType.setLowValue(minValue);
        charType.setHighValue(maxValue);
        Assert.assertEquals(minValue, charType.lowValue());
        Assert.assertEquals(maxValue, charType.highValue());
        for (int i = 0; i < 10000; i++) {
            int length = charType.acquire().getBytes().length;
            Assert.assertTrue(length <= 64);
            Assert.assertTrue(length >= 32);
        }
    }
}
