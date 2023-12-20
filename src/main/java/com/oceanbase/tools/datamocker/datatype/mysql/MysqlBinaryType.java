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
 * The binary type in mysql mode, including binary and varbinary
 *
 * @author yh263208
 * @date 2020-12-16 10:47
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlBinaryType extends AbstractByteDataType {
    /**
     * Binary width, eg. the width of varbinary(128) is 128
     */
    private final Integer width;

    /**
     * Constructor
     *
     * @param generator Character type binding data generator
     * @param width width of data type
     * @param allowNull Whether to allow null values
     */
    public MysqlBinaryType(byte[] defaultValue, Boolean allowNull, Integer width, BaseByteGenerator generator) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull);
        this.width = width;
    }

    /**
     * Constructor
     *
     * @param defaultValue default value for byte type
     * @param allowNull Whether to allow null values
     * @param width width of data type
     */
    public MysqlBinaryType(byte[] defaultValue, Boolean allowNull, Integer width) {
        super(ObModeType.OB_MYSQL, defaultValue, allowNull);
        this.width = width;
    }

    @Override
    public DataTypeFactory<MysqlBinaryType, CharDataTypeConfig, BaseByteGenerator> getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_BINARY");
    }

    @Override
    protected Integer maxValueForType() {
        return this.width;
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
        return String.format("BINARY(%d)", this.width);
    }
}
