package com.oceanbase.tools.datamocker.datatype.mysql;

import com.oceanbase.tools.datamocker.datatype.AbstractByteDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.ByteGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * mysql模式下的binary类型，包括binary和varbinary
 *
 * @author yh263208
 * @date 2020-12-16 10:47
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlBinaryType extends AbstractByteDataType {
    /**
     * 宽度
     */
    private final Integer width;

    /**
     * 构造函数
     *
     * @param generator 字符类型绑定的数据生成器
     * @param width     宽度
     * @param allowNull 是否允许空值
     */
    public MysqlBinaryType(byte[] defaultValue, Boolean allowNull, Integer width, ByteGeneratorBase generator) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull);
        this.width = width;
    }

    /**
     * 构造函数
     *
     * @param allowNull 是否允许空值
     * @param width     宽度
     */
    public MysqlBinaryType(byte[] defaultValue, Boolean allowNull, Integer width) {
        super(ObModeType.OB_MYSQL, defaultValue, allowNull);
        this.width = width;
    }

    @Override
    public DataTypeFactory getFactory() {
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
