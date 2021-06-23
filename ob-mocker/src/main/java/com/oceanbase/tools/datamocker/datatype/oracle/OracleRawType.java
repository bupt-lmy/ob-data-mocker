package com.oceanbase.tools.datamocker.datatype.oracle;

import com.oceanbase.tools.datamocker.datatype.AbstractByteDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseByteGenerator;
import com.oceanbase.tools.datamocker.model.config.model.CharDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * Raw type in oracle mode
 *
 * @author yh263208
 * @date 2021-01-31 10:46
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleRawType extends AbstractByteDataType {
    /**
     * Raw width, eg. the width of raw(128) is 128
     */
    private final Integer width;

    /**
     * Constructor
     *
     * @param defaultValue default value for raw
     * @param generator Character type binding data generator
     * @param width width of data type
     * @param allowNull Whether to allow null values
     */
    public OracleRawType(byte[] defaultValue, Boolean allowNull, Integer width, BaseByteGenerator generator) {
        super(generator, ObModeType.OB_ORACLE, defaultValue, allowNull);
        this.width = width;
    }

    /**
     * Constructor
     *
     * @param defaultValue default value for raw
     * @param width width of data type
     * @param allowNull Whether to allow null values
     */
    public OracleRawType(byte[] defaultValue, Boolean allowNull, Integer width) {
        super(ObModeType.OB_ORACLE, defaultValue, allowNull);
        this.width = width;
    }

    @Override
    public DataTypeFactory<OracleRawType, CharDataTypeConfig, BaseByteGenerator> getFactory() {
        return DataTypeFactory.getInstance("OB_ORACLE_RAW");
    }

    @Override
    protected Integer maxValueForType() {
        return this.width;
    }

    @Override
    public String toString(byte[] value) {
        if (value == null) {
            return "NULL";
        }
        return String.format("utl_raw.cast_to_raw('%s')", new String(value));
    }

    @Override
    public String toString() {
        return String.format("RAW(%d)", this.width);
    }
}
