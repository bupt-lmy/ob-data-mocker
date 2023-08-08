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

/**
 * Configuration object for specific column generation tasks
 *
 * @author yh263208
 * @date 2020-12-22 22:07
 * @since OBMOCKER-snapshot-0.1.0
 */
public abstract class AbstractColumnConfig {
    /**
     * Returns the column name of the column
     */
    abstract public String columnName();

    /**
     * Get the data type encapsulation object, in which the encapsulation logic of constraints is added
     *
     * @return Return data type package object
     */
    abstract public AbstractDataType<?, ? extends Comparable<?>> columnType();

    /**
     * Whether it is allowed to be empty, if it is true, it is possible to insert a null value into it
     *
     * @return Return whether the result is empty
     */
    abstract public Boolean allowNull();

    /**
     * The default value of the column
     *
     * @return Return to the default value
     */
    abstract public Object defaultValue();

}
