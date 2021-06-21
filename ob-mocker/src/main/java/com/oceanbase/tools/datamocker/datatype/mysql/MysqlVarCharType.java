package com.oceanbase.tools.datamocker.datatype.mysql;

import com.oceanbase.tools.datamocker.datatype.AbstractCharDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Variable string type in mysql mode
 *
 * @author yh263208
 * @date 2020-12-16 18:48
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlVarCharType extends AbstractCharDataType {
    /**
     * Constructor
     *
     * @param length       Length of character type
     * @param charsetType  Character type encoding format
     * @param generator    Character type binding data generator
     * @param allowNull    Whether to allow null values
     * @param isUnicode    Is it a unicode string
     * @param defaultValue default value for string type
     */
    public MysqlVarCharType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType, CharGeneratorBase generator,
            Boolean isUnicode) {
        super(generator, ObModeType.OB_MYSQL, charsetType, length, defaultValue, allowNull, isUnicode);
        if (length > 65535 || length <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Length for char type of mysql can not be larger than 65535 or smaller than 0");
        }
    }

    /**
     * Constructor
     *
     * @param length Length of character type
     * @param charsetType Character type encoding format
     * @param allowNull Whether to allow null values
     * @param isUnicode Is it a unicode string
     * @param defaultValue default value for string type
     */
    public MysqlVarCharType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType, Boolean isUnicode) {
        super(ObModeType.OB_MYSQL, charsetType, length, defaultValue, allowNull, isUnicode);
        if (length > 65535 || length <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Length for char type of mysql can not be larger than 65535 or smaller than 0");
        }
    }

    @Override
    public DataTypeFactory getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_VARCHAR");
    }

    @Override
    public String toString() {
        return String.format("VARCHAR(%d)", this.maxValueForType());
    }
}
