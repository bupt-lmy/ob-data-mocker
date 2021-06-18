package com.oceanbase.tools.datamocker.datatype.mysql;

import com.oceanbase.tools.datamocker.datatype.AbstractCharDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * mysq模式下的text类型
 *
 * @author yh263208
 * @date 2021-01-13 12:29
 * @since OBMOCKER_0.1.0_snapshot
 */
public class MysqlTextType extends AbstractCharDataType {
    /**
     * 构造函数
     *
     * @param charsetType 字符类型的编码格式
     * @param generator   字符类型绑定的数据生成器
     * @param allowNull   是否允许空值
     */
    public MysqlTextType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType, CharGeneratorBase generator,
            Boolean isUnicode) {
        super(generator, ObModeType.OB_MYSQL, charsetType, length, defaultValue, allowNull, isUnicode);
    }

    /**
     * 构造函数
     *
     * @param charsetType 字符类型的编码格式
     * @param allowNull   是否允许空值
     */
    public MysqlTextType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType, Boolean isUnicode) {
        super(ObModeType.OB_MYSQL, charsetType, length, defaultValue, allowNull, isUnicode);
    }

    @Override
    public DataTypeFactory getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_TEXT");
    }

    @Override
    public String toString() {
        return "text";
    }
}
