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

import com.oceanbase.tools.datamocker.datatype.AbstractByteDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseByteGenerator;
import com.oceanbase.tools.datamocker.model.config.model.CharDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * The blob type in oracle mode
 *
 * @author yh263208
 * @date 2020-12-16 22:52
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleBlobType extends AbstractByteDataType {
    /**
     * Constructor
     *
     * @param defaultValue default value for data type
     * @param generator Character type binding data generator
     * @param allowNull Whether to allow null values
     */
    public OracleBlobType(byte[] defaultValue, Boolean allowNull, BaseByteGenerator generator) {
        super(generator, ObModeType.OB_ORACLE, defaultValue, allowNull);
    }

    /**
     * Constructor
     *
     * @param defaultValue default value for data type
     * @param allowNull Whether to allow null values
     */
    public OracleBlobType(byte[] defaultValue, Boolean allowNull) {
        super(ObModeType.OB_ORACLE, defaultValue, allowNull);
    }

    @Override
    public DataTypeFactory<OracleBlobType, CharDataTypeConfig, BaseByteGenerator> getFactory() {
        return DataTypeFactory.getInstance("OB_ORACLE_BLOB");
    }

    @Override
    protected Integer maxValueForType() {
        return 4096;
    }

    @Override
    public String convertToSqlString(byte[] value) {
        if (value == null) {
            return "NULL";
        }
        return String.format("utl_raw.cast_to_raw('%s')", new String(value));
    }

    @Override
    public String toString() {
        return "BLOB";
    }
}
