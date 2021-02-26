package com.oceanbase.tools.datamocker.datatype.mysql;

import com.oceanbase.tools.datamocker.datatype.AbstractByteDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.ByteGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.DialectType;

/**
 * mysql模式下的blob类型
 *
 * @author yh263208
 * @date 2020-12-16 10:23
 * @since OBMOCKER_0.1.0_snapshot
 */
public class MysqlBlobType extends AbstractByteDataType {
    /**
     * 数据类型的长度
     */
    private final Integer length;

    /**
     * 构造函数
     *
     * @param generator 字符类型绑定的数据生成器
     * @param length    数据类型长度
     * @param allowNull 是否允许空值
     */
    public MysqlBlobType(Integer length, byte[] defaultValue, Boolean allowNull, ByteGeneratorBase generator) {
        super(generator, DialectType.OB_MYSQL, defaultValue, allowNull);
        this.length = length;
    }

    /**
     * 构造函数
     *
     * @param allowNull    是否允许空值
     * @param length       最大允许长度
     * @param defaultValue 默认值
     */
    public MysqlBlobType(Integer length, byte[] defaultValue, Boolean allowNull) {
        super(DialectType.OB_MYSQL, defaultValue, allowNull);
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
