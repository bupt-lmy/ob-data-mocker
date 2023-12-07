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
package com.oceanbase.tools.datamocker.model.mock;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang.Validate;

/**
 * A simple JavaBean used to encapsulate the data generated on each column of the simulated data
 *
 * @author yh263208
 * @date 2021-02-22 11:44
 * @since OBMOCKER_snapshit_0.1.0
 */
public class MockColumnData<T> {

    @Getter
    private final String columnName;
    private final Pair<AbstractDataType<T, ? extends Comparable<?>>, T> column;

    public MockColumnData(String columnName, @NonNull AbstractDataType<T, ? extends Comparable<?>> dataType,
            T columnValue) {
        Validate.notEmpty(columnName, "ColumnName can not be blank");
        this.columnName = columnName;
        this.column = new Pair<>(dataType, columnValue);
    }

    public T getColumnValue() {
        return this.column.getValue();
    }

    public Object getJdbcColumnValue() {
        return this.column.getKey().convertFromJavaObjectToJdbcObject(getColumnValue());
    }

    public String getColumnValueString() {
        return this.column.getKey().convertToSqlString(getColumnValue());
    }

    public AbstractDataType<T, ? extends Comparable<?>> getColumnDataType() {
        return this.column.getKey();
    }

    public T toDigest() {
        AbstractDataType<T, ? extends Comparable<?>> dataType = getColumnDataType();
        return dataType.toDigest(dataType.convertFromJdbcObjectToJavaObject(getColumnValue()));
    }

    public String toDigestString() {
        return getColumnDataType().convertToSqlString(toDigest());
    }

}
