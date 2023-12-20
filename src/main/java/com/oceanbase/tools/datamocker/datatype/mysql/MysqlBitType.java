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
import com.oceanbase.tools.datamocker.model.config.DigitDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import org.apache.commons.lang3.Validate;

/**
 * {@link MysqlBitType}
 *
 * @author yh263208
 * @date 2023-12-08 11:39
 * @since ob-data-mocker_0.2.0
 */
public class MysqlBitType extends AbstractDigitDataType<BigDecimal> {
    /**
     * length of bit, not length of byte, 8 bits = 1 byte max bit length is 64
     */
    private final int bitLength;

    public MysqlBitType(int bitLength, BaseDigitalGenerator<BigDecimal> generator,
            BigDecimal defaultValue, Boolean allowNull) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull, false);
        Validate.inclusiveBetween(1, 64, bitLength, "Bit length is out of range [1-64]");
        this.bitLength = bitLength;
    }

    @Override
    public BigDecimal convertFromJdbcObjectToJavaObject(Object value) {
        return value == null ? null : new BigDecimal(value.toString());
    }

    @Override
    public DataTypeFactory<MysqlIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_BIT");
    }

    @Override
    public String toString() {
        return "bit";
    }

    @Override
    public String convertToSqlString(BigDecimal value) {
        return value == null ? "NULL" : value.toPlainString();
    }

    @Override
    protected BigDecimal minValueForType() {
        return new BigDecimal("0");
    }

    @Override
    protected BigDecimal maxValueForType() {
        return new BigDecimal("2").pow(bitLength).subtract(new BigDecimal("1"));
    }

    @Override
    protected BigDecimal preProcessingBeforeOutput(BigDecimal value) {
        return value == null ? null : value.setScale(0, BigDecimal.ROUND_DOWN);
    }

    @Override
    protected Long limitForType(BigDecimal lowValue, BigDecimal highValue) {
        return Long.parseLong(highValue.subtract(lowValue).toPlainString()) + 1;
    }

}
