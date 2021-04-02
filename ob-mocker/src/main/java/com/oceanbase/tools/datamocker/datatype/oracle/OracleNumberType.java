package com.oceanbase.tools.datamocker.datatype.oracle;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.datatype.AbstractDigitDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Oracle模式下的Number类型的数据
 *
 * @author yh263208
 * @date 2020-12-10 15:05
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleNumberType extends AbstractDigitDataType<BigDecimal> {
    /**
     * 有效数字位数，在oracle中该值为0～38
     */
    private final int precision;
    /**
     * 精度，在oracle中精度范围为-84～127
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
     * oracle模式下的number数据类型中有有效数字和精度的要求，因此需要知道在指定精度和有效数字的约束下该数据类型的最大最小值
     *
     * @return 返回最大最小值的绝对值，如果是最大值则直接使用返回值就可以了，如果是最小值则取负值
     * @throws MockerException 有效数字位数和精度有大小范围的要求，超过范围可能会抛错
     */
    private BigDecimal maxOrMinForNumber() {
        if (precision > 38 || precision < 0 || scale < -84 || scale > 127) {
            throw new MockerException("precision or scale is out of range");
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
