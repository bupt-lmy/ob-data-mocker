package com.oceanbase.tools.datamocker.datatype.mysql;

import com.oceanbase.tools.datamocker.datatype.AbstractByteDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseByteGenerator;
import com.oceanbase.tools.datamocker.model.config.model.CharDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * The binary type in mysql mode, including binary and varbinary
 *
 * @author yh263208
 * @date 2020-12-16 10:47
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlBinaryType extends AbstractByteDataType {
    /**
     * Binary width, eg. the width of varbinary(128) is 128
     */
    private final Integer width;

    /**
     * Constructor
     *
     * @param generator Character type binding data generator
     * @param width width of data type
     * @param allowNull Whether to allow null values
     */
    public MysqlBinaryType(byte[] defaultValue, Boolean allowNull, Integer width, BaseByteGenerator generator) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull);
        this.width = width;
    }

    /**
     * Constructor
     *
     * @param defaultValue default value for byte type
     * @param allowNull Whether to allow null values
     * @param width width of data type
     */
    public MysqlBinaryType(byte[] defaultValue, Boolean allowNull, Integer width) {
        super(ObModeType.OB_MYSQL, defaultValue, allowNull);
        this.width = width;
    }

    @Override
    public DataTypeFactory<MysqlBinaryType, CharDataTypeConfig, BaseByteGenerator> getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_BINARY");
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
        return String.format("'%s'", new String(value));
    }

    @Override
    public String toString() {
        return String.format("BINARY(%d)", this.width);
    }
}
