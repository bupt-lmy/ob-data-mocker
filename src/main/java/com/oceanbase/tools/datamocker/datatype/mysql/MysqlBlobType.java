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

import com.oceanbase.tools.datamocker.datatype.AbstractByteDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseByteGenerator;
import com.oceanbase.tools.datamocker.model.config.CharDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * Blob type in mysql mode
 *
 * @author yh263208
 * @date 2020-12-16 10:23
 * @since OBMOCKER_0.1.0_snapshot
 */
public class MysqlBlobType extends AbstractByteDataType {
    /**
     * The length of the data type
     */
    private final Integer length;

    /**
     * Constructor
     *
     * @param generator Character type binding data generator
     * @param length Data type length
     * @param allowNull Whether to allow null values
     * @param defaultValue default value for byte type
     */
    public MysqlBlobType(Integer length, byte[] defaultValue, Boolean allowNull, BaseByteGenerator generator) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull);
        this.length = length;
    }

    /**
     * Constructor
     *
     * @param length Data type length
     * @param allowNull Whether to allow null values
     * @param defaultValue default value for byte type
     */
    public MysqlBlobType(Integer length, byte[] defaultValue, Boolean allowNull) {
        super(ObModeType.OB_MYSQL, defaultValue, allowNull);
        this.length = length;
    }

    @Override
    public DataTypeFactory<MysqlBlobType, CharDataTypeConfig, BaseByteGenerator> getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_BLOB");
    }

    @Override
    protected Integer maxValueForType() {
        return this.length;
    }

    @Override
    public String convertToSqlString(byte[] value) {
        if (value == null) {
            return "NULL";
        }
        return String.format("'%s'", new String(value));
    }

    @Override
    public String toString() {
        return "BLOB";
    }
}
