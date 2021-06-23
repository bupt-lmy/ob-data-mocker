package com.oceanbase.tools.datamocker.datatype.mysql;

import com.oceanbase.tools.datamocker.datatype.AbstractCharDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseCharGenerator;
import com.oceanbase.tools.datamocker.model.config.model.CharDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * Text type in mysql mode
 *
 * @author yh263208
 * @date 2021-01-13 12:29
 * @since OBMOCKER_0.1.0_snapshot
 */
public class MysqlTextType extends AbstractCharDataType {
    /**
     * Constructor
     *
     * @param length data length for text type
     * @param defaultValue default value for data type
     * @param isUnicode Whether it is a unicode character
     * @param charsetType Character type encoding format
     * @param generator Character type binding data generator
     * @param allowNull Whether to allow null values
     */
    public MysqlTextType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType,
            BaseCharGenerator generator,
            Boolean isUnicode) {
        super(generator, ObModeType.OB_MYSQL, charsetType, length, defaultValue, allowNull, isUnicode);
    }

    /**
     * Constructor
     *
     * @param length data length for text type
     * @param defaultValue default value for data type
     * @param isUnicode Whether it is a unicode character
     * @param charsetType Character type encoding format
     * @param allowNull Whether to allow null values
     */
    public MysqlTextType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType,
            Boolean isUnicode) {
        super(ObModeType.OB_MYSQL, charsetType, length, defaultValue, allowNull, isUnicode);
    }

    @Override
    public DataTypeFactory<MysqlTextType, CharDataTypeConfig, BaseCharGenerator> getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_TEXT");
    }

    @Override
    public String toString() {
        return "text";
    }
}
