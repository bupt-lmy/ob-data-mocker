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
package com.oceanbase.tools.datamocker.core.read;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.Validate;

/**
 * Column reader, which is used to get a column data from data generator
 *
 * @author yh263208
 * @date 2020-12-31 17:41
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class ColumnReader<T> extends AbstractMockReader<T> {
    /**
     * Data type for a column
     */
    private final AbstractDataType<T, ? extends Comparable<?>> dataType;
    /**
     * Column name
     */
    @Getter
    private final String columnName;
    /**
     * Group ID
     */
    private final String groupId;

    public ColumnReader(AbstractDataType<T, ? extends Comparable<?>> dataType, String columnName, String groupId) {
        Validate.notNull(dataType, "DataType can not be null for ColumnReader");
        Validate.notNull(columnName, "ColumnName can not be null for ColumnReader");
        this.dataType = dataType;
        this.columnName = columnName;
        this.groupId = groupId;
    }

    @Override
    public MockColumnData<T> read() {
        return new MockColumnData<>(columnName, dataType, dataType.acquire());
    }

    @Override
    public String groupId() {
        return this.groupId;
    }
}
