package com.oceanbase.tools.datamocker.datatype.oracle;

import com.oceanbase.tools.datamocker.datatype.AbstractCharDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.enums.DialectType;

/**
 * oracle模式下char类型
 *
 * @author yh263208
 * @date 2020-12-16 15:33
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleCharType extends AbstractCharDataType {
    /**
     * 构造函数
     *
     * @param length      字符类型的长度
     * @param charsetType 字符类型的编码格式
     * @param generator   字符类型绑定的数据生成器
     * @param allowNull   是否允许空值
     */
    public OracleCharType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType, CharGeneratorBase generator,
            Boolean isUnicode) {
        super(generator, DialectType.OB_ORACLE, charsetType, length, defaultValue, allowNull, isUnicode);
    }

    /**
     * 构造函数
     *
     * @param length      字符类型的长度
     * @param charsetType 字符类型的编码格式
     * @param allowNull   是否允许空值
     */
    public OracleCharType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType, Boolean isUnicode) {
        super(DialectType.OB_ORACLE, charsetType, length, defaultValue, allowNull, isUnicode);
    }

    @Override
    public DataTypeFactory getFactory() {
        return DataTypeFactory.getInstance("OB_ORACLE_CHAR");
    }

    @Override
    public String toString() {
        return String.format("CHAR(%d)", this.maxValueForType());
    }
}
