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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Getter;
import lombok.Setter;

/**
 * Type configuration, used to represent the type configuration information of the database
 *
 * @author yh263208
 * @date 2020-12-24 20:26
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "name")
@JsonSubTypes(value = {
        @JsonSubTypes.Type(value = DigitDataTypeConfig.class, name = "DIGIT"),
        @JsonSubTypes.Type(value = DateDataTypeConfig.class, name = "DATE"),
        @JsonSubTypes.Type(value = CharDataTypeConfig.class, name = "CHAR")
})
public class DataTypeConfig {

    private Object defaultValue;
    private Boolean allowNull;
    private String columnType;
    /**
     * Data type low value, meaning the same as low value in oracle
     */
    private Object lowValue;
    /**
     * Data type high value, meaning the same as high value in oracle
     */
    private Object highValue;
    /**
     * The initialization parameter of the data generator bound to the column may have multiple values,
     * so it is encapsulated with a List object
     */
    private Map<String, Object> genParams;
    /**
     * The data generator builder object is used to generate a specific data generator for the column
     * object to use based on the above params object
     */
    private String generator;

}
