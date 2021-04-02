package com.oceanbase.tools.datamocker.datatype.oracle;

import com.oceanbase.tools.datamocker.datatype.AbstractByteDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.ByteGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * oracle模式中的blob类型
 *
 * @author yh263208
 * @date 2020-12-16 22:52
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleBlobType extends AbstractByteDataType {
    /**
     * 构造函数
     *
     * @param generator 字符类型绑定的数据生成器
     * @param allowNull 是否允许空值
     */
    public OracleBlobType(byte[] defaultValue, Boolean allowNull, ByteGeneratorBase generator) {
        super(generator, ObModeType.OB_ORACLE, defaultValue, allowNull);
    }

    /**
     * 构造函数
     *
     * @param allowNull 是否允许空值
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
