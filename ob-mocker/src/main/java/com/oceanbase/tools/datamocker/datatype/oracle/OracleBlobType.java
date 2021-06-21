package com.oceanbase.tools.datamocker.datatype.oracle;

import com.oceanbase.tools.datamocker.datatype.AbstractByteDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.ByteGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * The blob type in oracle mode
 *
 * @author yh263208
 * @date 2020-12-16 22:52
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleBlobType extends AbstractByteDataType {
    /**
     * Constructor
     *
     * @param defaultValue default value for data type
     * @param generator    Character type binding data generator
     * @param allowNull    Whether to allow null values
     */
    public OracleBlobType(byte[] defaultValue, Boolean allowNull, ByteGeneratorBase generator) {
        super(generator, ObModeType.OB_ORACLE, defaultValue, allowNull);
    }

    /**
     * Constructor
     *
     * @param defaultValue default value for data type
     * @param allowNull Whether to allow null values
     */
    public OracleBlobType(byte[] defaultValue, Boolean allowNull) {
        super(ObModeType.OB_ORACLE, defaultValue, allowNull);
    }

    @Override
    public DataTypeFactory getFactory() {
        return DataTypeFactory.getInstance("OB_ORACLE_BLOB");
    }

    @Override
    protected Integer maxValueForType() {
        return 4096;
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
        return "BLOB";
    }
}
