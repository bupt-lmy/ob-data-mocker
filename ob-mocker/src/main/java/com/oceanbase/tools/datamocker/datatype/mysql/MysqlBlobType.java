package com.oceanbase.tools.datamocker.datatype.mysql;

import com.oceanbase.tools.datamocker.datatype.AbstractByteDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.ByteGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * Blob type in mysql mode
 *
 * @author yh263208
 * @date 2020-12-16 10:23
 * @since OBMOCKER_0.1.0_snapshot
 */
public class MysqlBlobType extends AbstractByteDataType {
    /**
     * The length of the data type
     */
    private final Integer length;

    /**
     * Constructor
     *
     * @param generator    Character type binding data generator
     * @param length       Data type length
     * @param allowNull    Whether to allow null values
     * @param defaultValue default value for byte type
     */
    public MysqlBlobType(Integer length, byte[] defaultValue, Boolean allowNull, ByteGeneratorBase generator) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull);
        this.length = length;
    }

    /**
     * Constructor
     *
     * @param length Data type length
     * @param allowNull Whether to allow null values
     * @param defaultValue default value for byte type
     */
    public MysqlBlobType(Integer length, byte[] defaultValue, Boolean allowNull) {
        super(ObModeType.OB_MYSQL, defaultValue, allowNull);
        this.length = length;
    }

    @Override
    public DataTypeFactory getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_BLOB");
    }

    @Override
    protected Integer maxValueForType() {
        return this.length;
    }

    @Override
    public String toString(byte[] value) {
        if (value == null) {
            return "NULL";
        }
        return String.format("'%s'", new String(value));
    }

    @Override
    public String toString() {
        return "BLOB";
    }
}
