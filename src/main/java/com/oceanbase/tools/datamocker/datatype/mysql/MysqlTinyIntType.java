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

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.datatype.AbstractDigitDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseDigitalGenerator;
import com.oceanbase.tools.datamocker.model.config.model.DigitDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * tinyint type in mysql
 *
 * @author yh263208
 * @date 2021-01-28 18:04
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlTinyIntType extends AbstractDigitDataType<BigDecimal> {
    /**
     * Constructor of tinyint type
     *
     * @param generator data generator
     * @param defaultValue default value for data type
     * @param allowNull Whether it is allowed to be empty
     * @param signed Is it a signed number
     */
    public MysqlTinyIntType(BaseDigitalGenerator<BigDecimal> generator, BigDecimal defaultValue, Boolean allowNull,
            Boolean signed) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull, signed);
    }

    @Override
    protected Long limitForType(BigDecimal lowValue, BigDecimal highValue) {
        BigDecimal interval = highValue.subtract(lowValue);
        return Long.parseLong(interval.toPlainString()) + 1;
    }

    /**
     * Conversion method, because mysql reuses the data generator in oracle mode, t he data must be
     * calculated using BigDecimal for data type conversion.
     *
     * @param value original value
     * @return converted value
     */
    @Override
    public BigDecimal convertFromJdbcObjectToJavaObject(Object value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString());
    }

    @Override
    public DataTypeFactory<MysqlTinyIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> getFactory() {
        if (signed()) {
            return DataTypeFactory.getInstance("OB_MYSQL_TINYINT");
        }
        return DataTypeFactory.getInstance("OB_MYSQL_TINYINT_UNSIGNED");
    }

    @Override
    protected BigDecimal minValueForType() {
        if (signed()) {
            return new BigDecimal("-128");
        }
        return new BigDecimal("0");
    }

    @Override
    protected BigDecimal maxValueForType() {
        if (signed()) {
            return new BigDecimal("127");
        }
        return new BigDecimal("255");
    }

    @Override
    protected BigDecimal preProcessingBeforeOutput(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(0, BigDecimal.ROUND_DOWN);
    }

    @Override
    public String toString() {
        if (signed()) {
            return "tinyint";
        }
        return "tinyint unsigned";
    }

    @Override
    public String convertToSqlString(BigDecimal value) {
        if (value == null) {
            return "NULL";
        }
        return value.toPlainString();
    }
}
