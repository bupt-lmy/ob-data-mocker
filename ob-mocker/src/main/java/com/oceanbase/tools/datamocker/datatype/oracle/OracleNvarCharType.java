package com.oceanbase.tools.datamocker.datatype.oracle;

import com.oceanbase.tools.datamocker.datatype.AbstractCharDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * nvarchar2 type in oracle mode
 *
 * @author yh263208
 * @date 2020-12-16 22:05
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleNvarCharType extends AbstractCharDataType {
    /**
     * Constructor
     *
     * @param length       Length of character type
     * @param charsetType  Character type encoding format
     * @param generator    Character type binding data generator
     * @param allowNull    Whether to allow null values
     * @param defaultValue default value for data type
     */
    public OracleNvarCharType(CharGeneratorBase generator, Integer length, String defaultValue, Boolean allowNull,
            CharsetType charsetType) {
        super(generator, ObModeType.OB_ORACLE, charsetType, length, defaultValue, allowNull, Boolean.TRUE);
    }

    /**
     * Constructor
     *
     * @param length Length of character type
     * @param charsetType Character type encoding format
     * @param allowNull Whether to allow null values
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
