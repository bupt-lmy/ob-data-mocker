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
package com.oceanbase.tools.datamocker.model.config;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import lombok.Getter;
import lombok.Setter;

/**
 * Column task configuration object, used to indicate the configuration information of the column
 * generation task
 *
 * @author yh263208
 * @date 2020-12-27 20:57
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class MockColumnConfig {

    private String columnName;
    private DataTypeConfig typeConfig;
    private Boolean allowNull = true;
    private Object defaultValue;
    private AbstractDataType<?, ? extends Comparable<?>> dataType = null;

    public synchronized AbstractDataType<?, ? extends Comparable<?>> getDataType() {
        if (dataType != null) {
            return dataType;
        }
        DataTypeFactory<? extends AbstractDataType<?, ? extends Comparable<?>>, DataTypeConfig, ? extends BaseGenerator<? extends Comparable<?>, ?>> dataTypeFactory =
                DataTypeFactory.getInstance(typeConfig.getColumnType());
        typeConfig.setAllowNull(getAllowNull());
        typeConfig.setDefaultValue(getDefaultValue());
        this.dataType = dataTypeFactory.make(typeConfig);
        return this.dataType;
    }

}
