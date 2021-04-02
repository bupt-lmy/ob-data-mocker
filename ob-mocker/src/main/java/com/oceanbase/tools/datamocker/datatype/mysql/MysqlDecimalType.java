package com.oceanbase.tools.datamocker.datatype.mysql;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.datatype.AbstractDigitDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * mysql模式下的decimal类型
 *
 * @author yh263208
 * @date 2021-01-29 11:49
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlDecimalType extends AbstractDigitDataType<BigDecimal> {
    /**
     * 有效数字位数，在mysql中该值为0～65
     */
    private final int precision;
    /**
     * 精度，在mysql中精度范围为0～30
     */
    private final int scale;

    public MysqlDecimalType(int precision, int scale, DigitalGeneratorBase<BigDecimal> generator, BigDecimal defaultValue,
            Boolean allowNull, Boolean signed) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull, signed);
        validate(precision, scale);
        this.precision = precision;
        this.scale = scale;
    }

    public MysqlDecimalType(DigitalGeneratorBase<BigDecimal> generator, BigDecimal defaultValue, Boolean allowNull, Boolean signed) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull, signed);
        this.precision = 10;
        this.scale = 0;
    }

    /**
     * 验证decimal类型的有效位数和精度是否合法
     *
     * @param precision 有效位数
     * @param scale     精度
     * @throws MockerException 校验未通过抛出异常
     */
    private void validate(int precision, int scale) {
        if (precision <= 0 || precision > 65) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "precision for decaimal can not larger than 65 or smaller than 0");
        } else if (scale < 0 || scale > 30) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "scale for decimal can not larger than 30 or smaller than 0");
        } else if (precision < scale) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "scale can not be bigger than precision");
        }
    }

    @Override
    protected BigDecimal maxValueForType() {
        return maxOrMinForNumber();
    }

    @Override
    protected BigDecimal minValueForType() {
        if (signed()) {
            return maxOrMinForNumber().multiply(new BigDecimal(-1));
        }
        return new BigDecimal("0");
    }

    @Override
    protected Long limitForType(BigDecimal lowValue, BigDecimal highValue) {
        BigDecimal interval = highValue.subtract(lowValue);
        BigDecimal result = interval.multiply(new BigDecimal(Double.toString(Math.pow(10, scale))))
                .setScale(0, BigDecimal.ROUND_DOWN);
        return Long.valueOf(result.toPlainString());
    }

    @Override
    protected BigDecimal preTreat(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(scale, BigDecimal.ROUND_HALF_DOWN);
    }

    @Override
    public String toString(BigDecimal value) {
        if (value == null) {
            return "NULL";
        }
        return value.setScale(scale, BigDecimal.ROUND_HALF_DOWN).toPlainString();
    }

    @Override
    public String toString() {
        return String.format("decimal(%d, %d)", this.precision, this.scale);
    }

    /**
     * mysql模式下的decimal数据类型中有有效数字和精度的要求，因此需要知道在指定精度和有效数字的约束下该数据类型的最大最小值
     *
     * @return 返回最大最小值的绝对值，如果是最大值则直接使用返回值就可以了，如果是最小值则取负值
     * @throws MockerException 有效数字位数和精度有大小范围的要求，超过范围可能会抛错
     */
    private BigDecimal maxOrMinForNumber() {
        validate(precision, scale);
        BigDecimal secondPartMaxValue;
        int interval = precision - scale;
        int firstPartWidth = Math.max(interval, 0);
        BigDecimal firstPartMaxValue = new BigDecimal(Double.toString(Math.pow(10, firstPartWidth)))
                .subtract(BigDecimal.ONE);
        BigDecimal factor = new BigDecimal(Double.toString(Math.pow(10, -scale - 1)));
        if (scale >= 0) {
            if (firstPartWidth > 0) {
                factor = factor.multiply(new BigDecimal(5));
                secondPartMaxValue = BigDecimal.ONE
                        .subtract(new BigDecimal(Double.toString(Math.pow(10, -scale))))
                        .add(factor);
            } else {
                factor = factor.multiply(new BigDecimal(4));
                secondPartMaxValue = new BigDecimal(Double.toString(Math.pow(10, interval)))
                        .subtract(new BigDecimal(Double.toString(Math.pow(10, -scale))))
                        .add(factor);
            }
        } else {
            factor = factor.multiply(new BigDecimal(5));
            firstPartMaxValue = firstPartMaxValue.subtract(factor);
            secondPartMaxValue = BigDecimal.ZERO;
        }
        return firstPartMaxValue.add(secondPartMaxValue);
    }

    @Override
    public DataTypeFactory getFactory() {
        if (signed()) {
            return DataTypeFactory.getInstance("OB_ORACLE_DECIMAL");
        }
        return DataTypeFactory.getInstance("OB_ORACLE_DECIMAL_UNSIGNED");
    }
}
