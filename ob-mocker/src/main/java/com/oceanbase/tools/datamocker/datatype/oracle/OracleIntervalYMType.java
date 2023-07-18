package com.oceanbase.tools.datamocker.datatype.oracle;

import com.oceanbase.jdbc.extend.datatype.INTERVALYM;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.model.config.model.DateDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * INTERVAL YEAR TO MONTH data type in oracle mode, this data type only provides the most basic
 * compatibility, so use String as the basic Java type
 *
 * @author yh263208
 * @date 2021-02-04 12:01
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleIntervalYMType extends AbstractDataType<INTERVALYM, Integer> {
    /**
     * Type precision, up to 9 for interval year to month, the default is 2
     */
    private final Integer scale;

    /**
     * The interval year to month data type is compatible with the java type constructor
     *
     * @param generator Data generator
     * @param defaultValue default value for interval type
     * @param allowNull Whether it is allowed to be empty
     * @param scale scale for type
     * @throws MockerException If the precision value is illegal, an exception will be thrown
     */
    public OracleIntervalYMType(BaseGenerator<Integer, INTERVALYM> generator, Integer scale, INTERVALYM defaultValue,
            Boolean allowNull) {
        super(generator, ObModeType.OB_ORACLE, defaultValue, allowNull);
        if (scale == null) {
            this.scale = 2;
        } else {
            if (scale > 9 || scale < 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        "Scale for inter year to month can not be larger than 9 or smaller than 0");
            }
            this.scale = scale;
        }
    }

    @Override
    public DataTypeFactory<OracleIntervalYMType, DateDataTypeConfig, BaseGenerator<Integer, INTERVALYM>> getFactory() {
        return DataTypeFactory.getInstance("OB_ORACLE_INTERVAL_YEAR_TO_MONTH");
    }

    @Override
    protected Integer minValueForType() {
        return 1;
    }

    @Override
    protected Integer maxValueForType() {
        return this.scale;
    }

    @Override
    public Long distinctLimit() {
        if (generator == null || generator.count(lowValue(), highValue()) == null) {
            return Long.MAX_VALUE;
        }
        return generator.count(lowValue(), highValue());
    }

    @Override
    protected INTERVALYM preProcessingBeforeOutput(INTERVALYM value) {
        if (value == null) {
            return null;
        }
        byte yearLen = value.getBytes()[value.getBytes().length - 1];
        if (yearLen > this.scale) {
            throw new MockerException(MockerError.VALUE_OUT_OFRANGE,
                    String.format("Scale for interval year(%d) to month is out of bound, [%d>%d]", this.scale, yearLen,
                            this.scale));
        }
        return value;
    }

    @Override
    public String convertToSqlString(INTERVALYM value) {
        if (value == null) {
            return "NULL";
        }
        return String.format("interval %s year to month", value.toString());
    }

    @Override
    public String toString() {
        return String.format("INTERVAL YEAR(%d) TO MONTH", this.scale);
    }

    @Override
    public INTERVALYM toDigest(INTERVALYM value) {
        return value;
    }
}
