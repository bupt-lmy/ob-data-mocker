package com.oceanbase.tools.datamocker.datatype.mysql;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.datatype.AbstractDigitDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 单精度浮点型数据，mysql数据类型兼容
 *
 * @author yh263208
 * @date 2020-12-16 17:03
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlFloatType extends AbstractDigitDataType<BigDecimal> {
    /**
     * 有效数字位数，在mysql中该值为0～53
     */
    private final int precision;
    /**
     * 精度，在mysql中精度范围为0～30
     */
    private final int scale;

    public MysqlFloatType(int precision, int scale, DigitalGeneratorBase<BigDecimal> generator, BigDecimal defaultValue, Boolean allowNull,
            Boolean signed) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull, signed);
        validate(precision, scale);
        this.precision = precision;
        this.scale = scale;
    }

    public MysqlFloatType(DigitalGeneratorBase<BigDecimal> generator, BigDecimal defaultValue, Boolean allowNull, Boolean signed) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull, signed);
        this.precision = -1;
        this.scale = -1;
    }

    /**
     * 验证decimal类型的有效位数和精度是否合法
     *
     * @param precision 有效位数
     * @param scale     精度
     * @throws MockerException 校验未通过抛出异常
     */
    private void validate(int precision, int scale) {
        if (precision < -1 || precision > 53) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Precision for float can not larger than 53 or smaller than 0");
        } else if (scale < -1 || scale > 30) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Scale for float can not larger than 30 or smaller than 0");
        } else if (precision < scale) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Scale can not be bigger than precision");
        }
    }

    /**
     * 转换方法，由于mysql复用了oracle模式的数据生成器，数据使用BigDecimal进行计算必须使用转化方法进行数据类型转换
     *
     * @param value 原值
     * @return 转换值
     */
    @Override
    public BigDecimal convert(Object value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString());
    }

    @Override
    protected BigDecimal maxValueForType() {
        if (this.precision == -1 && this.scale == -1) {
            if (signed()) {
                return new BigDecimal("3.402823466351E+38");
            } else {
                return new BigDecimal("3.402823466E+38");
            }
        }
        return maxOrMinForNumber();
    }

    @Override
    protected BigDecimal minValueForType() {
        if (this.precision == -1 && this.scale == -1) {
            if (signed()) {
                return new BigDecimal("-3.402823466E+38");
            } else {
                return new BigDecimal("0");
            }
        } else if (this.precision >= 0 && this.scale >= 0) {
            if (signed()) {
                return maxOrMinForNumber().multiply(new BigDecimal(-1));
            }
            return new BigDecimal("0");
        } else {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Wrong precision or scale for float");
        }
    }

    @Override
    protected Long limitForType(BigDecimal lowValue, BigDecimal highValue) {
        if (this.precision == -1 && this.scale == -1) {
            return Long.MAX_VALUE;
        } else if (this.precision >= 0 && this.scale >= 0) {
            BigDecimal interval = highValue.subtract(lowValue);
            BigDecimal result = interval.multiply(new BigDecimal(Double.toString(Math.pow(10, scale))))
                    .setScale(0, BigDecimal.ROUND_DOWN);
            return Long.valueOf(result.toPlainString());
        } else {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Wrong precision or scale for float");
        }
    }

    @Override
    protected BigDecimal preTreat(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (this.precision == -1 && this.scale == -1) {
            return value;
        } else if (this.precision >= 0 && this.scale >= 0) {
            return value.setScale(scale, BigDecimal.ROUND_HALF_DOWN);
        } else {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Wrong precision or scale for float");
        }
    }

    @Override
    public String toString(BigDecimal value) {
        if (value == null) {
            return "NULL";
        }
        if (this.precision == -1 && this.scale == -1) {
            return value.toPlainString();
        } else if (this.precision >= 0 && this.scale >= 0) {
            return value.setScale(scale, BigDecimal.ROUND_HALF_DOWN).toPlainString();
        } else {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Wrong precision or scale for float");
        }
    }

    @Override
    public String toString() {
        if (this.precision == -1 && this.scale == -1) {
            return "float";
        } else if (this.precision >= 0 && this.scale >= 0) {
            return String.format("decimal(%d, %d)", this.precision, this.scale);
        } else {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Wrong precision or scale for float");
        }
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
            return DataTypeFactory.getInstance("OB_ORACLE_FLOAT");
        }
        return DataTypeFactory.getInstance("OB_ORACLE_FLOAT_UNSIGNED");
    }
}
