package com.oceanbase.tools.datamocker.datatype.oracle;

import com.oceanbase.tools.datamocker.datatype.AbstractCharDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * char type in oracle mode
 *
 * @author yh263208
 * @date 2020-12-16 15:33
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleCharType extends AbstractCharDataType {
    /**
     * Constructor
     *
     * @param length       Length of character type
     * @param charsetType  Character type encoding format
     * @param generator    Character type binding data generator
     * @param allowNull    Whether to allow null values
     * @param defaultValue default value for data type
     * @param isUnicode    Is it a unicode string
     */
    public OracleCharType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType, CharGeneratorBase generator,
            Boolean isUnicode) {
        super(generator, ObModeType.OB_ORACLE, charsetType, length, defaultValue, allowNull, isUnicode);
    }

    /**
     * Constructor
     *
     * @param length Length of character type
     * @param charsetType Character type encoding format
     * @param allowNull Whether to allow null values
     * @param defaultValue default value for data type
     * @param isUnicode Is it a unicode string
     */
    public OracleCharType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType, Boolean isUnicode) {
        super(ObModeType.OB_ORACLE, charsetType, length, defaultValue, allowNull, isUnicode);
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
