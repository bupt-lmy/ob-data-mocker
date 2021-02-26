package com.oceanbase.tools.datamocker.datatype.mysql;

import com.oceanbase.tools.datamocker.datatype.AbstractCharDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.enums.DialectType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * mysql模式下的字符类型
 *
 * @author yh263208
 * @date 2020-12-16 17:53
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlCharType extends AbstractCharDataType {
    /**
     * 构造函数
     *
     * @param length      字符类型的长度
     * @param charsetType 字符类型的编码格式
     * @param generator   字符类型绑定的数据生成器
     * @param allowNull   是否允许空值
     */
    public MysqlCharType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType, CharGeneratorBase generator,
            Boolean isUnicode) {
        super(generator, DialectType.OB_MYSQL, charsetType, length, defaultValue, allowNull, isUnicode);
        if (length > 256 || length <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "length for char type of mysql can not be larger than 256 or smaller than 0");
        }
    }

    /**
     * 构造函数
     *
     * @param length      字符类型的长度
     * @param charsetType 字符类型的编码格式
     * @param allowNull   是否允许空值
     */
    public MysqlCharType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType, Boolean isUnicode) {
        super(DialectType.OB_MYSQL, charsetType, length, defaultValue, allowNull, isUnicode);
        if (length > 256 || length <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "length for char type of mysql can not be larger than 256 or smaller than 0");
        }
    }

    @Override
    public DataTypeFactory getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_CHAR");
    }

    @Override
    public String toString() {
        return String.format("CHAR(%d)", this.maxValueForType());
    }
}
