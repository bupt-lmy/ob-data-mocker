/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.tools.datamocker.datatype.oracle;

import com.oceanbase.tools.datamocker.datatype.AbstractCharDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseCharGenerator;
import com.oceanbase.tools.datamocker.model.config.model.CharDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * Varchar type in oracle
 *
 * @author yh263208
 * @date 2020-12-16 21:30
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleVarCharType extends AbstractCharDataType {
    /**
     * Constructor
     *
     * @param length Length of character type
     * @param charsetType Character type encoding format
     * @param generator Character type binding data generator
     * @param allowNull Whether to allow null values
     * @param isUnicode Is it a unicode string
     * @param defaultValue default value for string type
     */
    public OracleVarCharType(BaseCharGenerator generator, Integer length, String defaultValue, Boolean allowNull,
            CharsetType charsetType,
            Boolean isUnicode) {
        super(generator, ObModeType.OB_ORACLE, charsetType, length, defaultValue, allowNull, isUnicode);
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
    public OracleVarCharType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType,
            Boolean isUnicode) {
        super(ObModeType.OB_ORACLE, charsetType, length, defaultValue, allowNull, isUnicode);
    }

    @Override
    public DataTypeFactory<OracleVarCharType, CharDataTypeConfig, BaseCharGenerator> getFactory() {
        return DataTypeFactory.getInstance("OB_ORACLE_VARCHAR");
    }

    @Override
    public String toString() {
        return String.format("VARCHAR(%d)", this.maxValueForType());
    }
}
