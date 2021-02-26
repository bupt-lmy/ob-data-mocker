package com.oceanbase.tools.datamocker.datatype.oracle;

import com.oceanbase.tools.datamocker.datatype.AbstractByteDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.ByteGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.DialectType;

/**
 * oracle模式下的raw类型
 *
 * @author yh263208
 * @date 2021-01-31 10:46
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleRawType extends AbstractByteDataType {
    private final Integer width;

    /**
     * 构造函数
     *
     * @param generator 字符类型绑定的数据生成器
     * @param width     宽度
     * @param allowNull 是否允许空值
     */
    public OracleRawType(byte[] defaultValue, Boolean allowNull, Integer width, ByteGeneratorBase generator) {
        super(generator, DialectType.OB_ORACLE, defaultValue, allowNull);
        this.width = width;
    }

    /**
     * 构造函数
     *
     * @param allowNull 是否允许空值
     * @param width     宽度
     */
    public OracleRawType(byte[] defaultValue, Boolean allowNull, Integer width) {
        super(DialectType.OB_ORACLE, defaultValue, allowNull);
        this.width = width;
    }

    @Override
    public DataTypeFactory getFactory() {
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
