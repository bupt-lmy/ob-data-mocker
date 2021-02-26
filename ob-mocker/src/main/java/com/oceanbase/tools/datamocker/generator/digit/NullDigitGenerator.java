package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;

/**
 * 数字类型的空数据生成器
 *
 * @author yh263208
 * @date 2021-01-26 14:43
 * @since OBMOCKER_0.1.0_dev
 */
public class NullDigitGenerator extends DigitalGeneratorBase<BigDecimal> {

    @Override
    public Boolean preCheck(BigDecimal minValue, BigDecimal maxValue) {
        return true;
    }

    @Override
    public BigDecimal generate(BigDecimal minValue, BigDecimal maxValue) {
        return null;
    }

    @Override
    public Long count(BigDecimal minValue, BigDecimal maxValue) {
        return 1L;
    }
}
