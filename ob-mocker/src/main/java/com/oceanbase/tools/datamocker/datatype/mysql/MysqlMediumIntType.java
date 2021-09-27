package com.oceanbase.tools.datamocker.datatype.mysql;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.datatype.AbstractDigitDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseDigitalGenerator;
import com.oceanbase.tools.datamocker.model.config.model.DigitDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * MiddleInt data type in mysql mode
 *
 * @author yh263208
 * @date 2021-01-28 20:09
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlMediumIntType extends AbstractDigitDataType<BigDecimal> {
    /**
     * Constructor of MiddleIntType type
     *
     * @param generator Data generator
     * @param defaultValue default value for int type
     * @param allowNull Whether it is allowed to be empty
     * @param signed Is it a signed number
     */
    public MysqlMediumIntType(BaseDigitalGenerator<BigDecimal> generator, BigDecimal defaultValue, Boolean allowNull,
            Boolean signed) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull, signed);
    }

    @Override
    protected Long limitForType(BigDecimal lowValue, BigDecimal highValue) {
        BigDecimal interval = highValue.subtract(lowValue);
        return Long.parseLong(interval.toPlainString()) + 1;
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
    public DataTypeFactory<MysqlMediumIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> getFactory() {
        if (signed()) {
            return DataTypeFactory.getInstance("OB_MYSQL_MEDIUMINT");
        }
        return DataTypeFactory.getInstance("OB_MYSQL_MEDIUMINT_UNSIGNED");
    }

    @Override
    protected BigDecimal minValueForType() {
        if (signed()) {
            return new BigDecimal("-8388608");
        }
        return new BigDecimal("0");
    }

    @Override
    protected BigDecimal maxValueForType() {
        if (signed()) {
            return new BigDecimal("8388607");
        }
        return new BigDecimal("16777215");
    }

    @Override
    protected BigDecimal preProcessingBeforeOutput(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(0, BigDecimal.ROUND_DOWN);
    }

    @Override
    public String toString() {
        if (signed()) {
            return "mediumint";
        }
        return "mediumint unsigned";
    }

    @Override
    public String convertToSqlString(BigDecimal value) {
        if (value == null) {
            return "NULL";
        }
        return value.toPlainString();
    }
}
