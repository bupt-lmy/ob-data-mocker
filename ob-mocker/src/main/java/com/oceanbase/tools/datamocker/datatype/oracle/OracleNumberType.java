package com.oceanbase.tools.datamocker.datatype.oracle;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.datatype.AbstractDigitDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Number type data in Oracle mode
 *
 * @author yh263208
 * @date 2020-12-10 15:05
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleNumberType extends AbstractDigitDataType<BigDecimal> {
    /**
     * The number of significant digits, the value is 0 to 38 in oracle
     */
    private final int precision;
    /**
     * Accuracy, the accuracy range in oracle is -84～127
     */
    private final int scale;

    public OracleNumberType(int precision, int scale, DigitalGeneratorBase<BigDecimal> generator, BigDecimal defaultValue,
            Boolean allowNull) {
        super(generator, ObModeType.OB_ORACLE, defaultValue, allowNull);
        this.precision = precision;
        this.scale = scale;
    }

    public OracleNumberType(int precision, DigitalGeneratorBase<BigDecimal> generator, BigDecimal defaultValue, Boolean allowNull) {
        super(generator, ObModeType.OB_ORACLE, defaultValue, allowNull);
        this.precision = precision;
        this.scale = 0;
    }

    public OracleNumberType(int precision, int scale, BigDecimal defaultValue, Boolean allowNull) {
        super(ObModeType.OB_ORACLE, defaultValue, allowNull);
        this.precision = precision;
        this.scale = scale;
    }

    @Override
    protected BigDecimal maxValueForType() {
        return maxOrMinForNumber();
    }

    @Override
    protected BigDecimal minValueForType() {
        return maxOrMinForNumber()
                .multiply(new BigDecimal(-1));
    }

    @Override
    protected Long limitForType(BigDecimal lowValue, BigDecimal highValue) {
        BigDecimal interval = highValue.subtract(lowValue);
        BigDecimal result = interval.multiply(
                new BigDecimal(Double.toString(Math.pow(10, scale))))
                .setScale(0, BigDecimal.ROUND_DOWN);
        if (result.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) {
            return Long.MAX_VALUE;
        }
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
        return String.format("NUMBER(%d, %d)", this.precision, this.scale);
    }

    /**
     * The number data type in oracle mode has significant digits and precision requirements,
     * so it is necessary to know the maximum and minimum values of the data type under the
     * constraints of the specified precision and significant digits
     *
     * @return Return the absolute value of the maximum and minimum values, if it is the maximum value,
     * just use the return value directly, if it is the minimum value, take a negative value
     * @throws MockerException The number of significant digits and precision are required for the size
     * range, and an error may be thrown if it exceeds the range
     */
    private BigDecimal maxOrMinForNumber() {
        if (precision > 38 || precision < 0 || scale < -84 || scale > 127) {
            throw new MockerException("Precision or scale is out of range");
        }
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
        return DataTypeFactory.getInstance("OB_ORACLE_NUMBER");
    }
}
