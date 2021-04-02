package com.oceanbase.tools.datamocker.datatype.oracle;

import com.oceanbase.tools.datamocker.datatype.AbstractCharDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * oracle模式下的nvarchar2类型
 *
 * @author yh263208
 * @date 2020-12-16 22:05
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleNvarCharType extends AbstractCharDataType {
    /**
     * 构造函数
     *
     * @param length      字符类型的长度
     * @param charsetType 字符类型的编码格式
     * @param generator   字符类型绑定的数据生成器
     * @param allowNull   是否允许空值
     */
    public OracleNvarCharType(CharGeneratorBase generator, Integer length, String defaultValue, Boolean allowNull,
            CharsetType charsetType) {
        super(generator, ObModeType.OB_ORACLE, charsetType, length, defaultValue, allowNull, Boolean.TRUE);
    }

    /**
     * 构造函数
     *
     * @param length      字符类型的长度
     * @param charsetType 字符类型的编码格式
     * @param allowNull   是否允许空值
     */
    public OracleNvarCharType(Integer length, String defaultvalue, Boolean allowNull, CharsetType charsetType) {
        super(ObModeType.OB_ORACLE, charsetType, length, defaultvalue, allowNull, Boolean.TRUE);
    }

    @Override
    public DataTypeFactory getFactory() {
        return DataTypeFactory.getInstance("OB_ORACLE_NVARCHAR");
    }

    @Override
    public String toString() {
        return String.format("VARCHAR(%d)", this.maxValueForType());
    }
}
