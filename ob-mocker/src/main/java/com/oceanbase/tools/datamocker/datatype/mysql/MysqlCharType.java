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
package com.oceanbase.tools.datamocker.datatype.mysql;

import com.oceanbase.tools.datamocker.datatype.AbstractCharDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseCharGenerator;
import com.oceanbase.tools.datamocker.model.config.model.CharDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Character type in mysql mode
 *
 * @author yh263208
 * @date 2020-12-16 17:53
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlCharType extends AbstractCharDataType {
    /**
     * Constructor
     *
     * @param length Length of character type
     * @param charsetType Character type encoding format
     * @param generator Character type binding data generator
     * @param allowNull Whether to allow null values
     * @param defaultValue defaultvalue for data type
     * @param isUnicode Whether it is a unicode character
     */
    public MysqlCharType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType,
            BaseCharGenerator generator,
            Boolean isUnicode) {
        super(generator, ObModeType.OB_MYSQL, charsetType, length, defaultValue, allowNull, isUnicode);
        if (length > 256 || length <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Length for char type of mysql can not be larger than 256 or smaller than 0");
        }
    }

    /**
     * Constructor
     *
     * @param length Length of character type
     * @param charsetType Character type encoding format
     * @param allowNull Whether to allow null values
     * @param defaultValue defaultvalue for data type
     * @param isUnicode Whether it is a unicode character
     */
    public MysqlCharType(Integer length, String defaultValue, Boolean allowNull, CharsetType charsetType,
            Boolean isUnicode) {
        super(ObModeType.OB_MYSQL, charsetType, length, defaultValue, allowNull, isUnicode);
        if (length > 256 || length <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Length for char type of mysql can not be larger than 256 or smaller than 0");
        }
    }

    @Override
    public DataTypeFactory<MysqlCharType, CharDataTypeConfig, BaseCharGenerator> getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_CHAR");
    }

    @Override
    public String toString() {
        return String.format("CHAR(%d)", this.maxValueForType());
    }
}
