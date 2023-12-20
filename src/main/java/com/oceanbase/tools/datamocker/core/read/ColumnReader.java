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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Column reader, which is used to get a column data from data generator
 *
 * @author yh263208
 * @date 2020-12-31 17:41
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class ColumnReader<T> {

    @Getter
    private final String columnName;
    private final String groupId;
    private final AbstractDataType<T, ? extends Comparable<?>> dataType;

    public ColumnReader(@NonNull AbstractDataType<T, ? extends Comparable<?>> dataType,
            @NonNull String columnName, String groupId) {
        this.dataType = dataType;
        this.columnName = columnName;
        this.groupId = groupId;
    }

    public String groupId() {
        return this.groupId;
    }

    public MockColumnData<T> read() {
        return new MockColumnData<>(columnName, dataType, dataType.acquire());
    }

}
