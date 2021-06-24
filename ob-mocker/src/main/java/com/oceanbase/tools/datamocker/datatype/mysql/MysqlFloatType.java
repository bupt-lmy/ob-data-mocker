package com.oceanbase.tools.datamocker.datatype.mysql;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.datatype.AbstractDigitDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseDigitalGenerator;
import com.oceanbase.tools.datamocker.model.config.model.DigitDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Single-precision floating-point data, compatible with mysql data types
 *
 * @author yh263208
 * @date 2020-12-16 17:03
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlFloatType extends AbstractDigitDataType<BigDecimal> {
    /**
     * The number of significant digits, the value in mysql is 0～53
     */
    private final int precision;
    /**
     * Scale, the scale range in mysql is 0～30
     */
    private final int scale;

    public MysqlFloatType(int precision, int scale, BaseDigitalGenerator<BigDecimal> generator, BigDecimal defaultValue,
            Boolean allowNull,
            Boolean signed) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull, signed);
        validate(precision, scale);
        this.precision = precision;
        this.scale = scale;
    }

    public MysqlFloatType(BaseDigitalGenerator<BigDecimal> generator, BigDecimal defaultValue, Boolean allowNull,
            Boolean signed) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull, signed);
        this.precision = -1;
        this.scale = -1;
    }

    /**
     * Verify that the effective digits and precision of the decimal type are legal
     *
     * @param precision Effective digits
     * @param scale Floating point precision
     * @throws MockerException Validation fails and throws an exception
     */
    private void validate(int precision, int scale) {
        if (precision < -1 || precision > 53) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Precision for float can not larger than 53 or smaller than 0");
        } else if (scale < -1 || scale > 30) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Scale for float can not larger than 30 or smaller than 0");
        } else if (precision < scale) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Scale can not be bigger than precision");
        }
    }

    /**
     * Conversion method, because mysql reuses the data generator in oracle mode, the data must be
     * calculated using BigDecimal for data type conversion.
     *
     * @param value original value
     * @return converted value
     */
    @Override
    public BigDecimal convertFromJdbcObjectToJavaObject(Object value) {
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
    protected BigDecimal preProcessingBeforeOutput(BigDecimal value) {
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
     * The decimal data type in mysql mode has significant digits and precision requirements, so you
     * need to know the maximum and minimum values of the data type under the constraints of the
     * specified precision and significant digits
     *
     * @return Return the absolute value of the maximum and minimum values, if it is the maximum value,
     *         just use the return value directly, if it is the minimum value, take a negative value
     * @throws MockerException The number of significant digits and precision are required for the size
     *         range, and an error may be thrown if it exceeds the range
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
    public DataTypeFactory<MysqlFloatType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> getFactory() {
        if (signed()) {
            return DataTypeFactory.getInstance("OB_MYSQL_FLOAT");
        }
        return DataTypeFactory.getInstance("OB_MYSQL_FLOAT_UNSIGNED");
    }
}
