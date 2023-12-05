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

import lombok.Getter;
import lombok.Setter;

/**
 * Character data type configuration object
 *
 * @author yh263208
 * @date 2020-12-24 20:59
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class CharDataTypeConfig extends DataTypeConfig {
    private String charset;
    /**
     * Data length, column length calculated according to Byte
     */
    private Integer width;
    /**
     * Whether to use characters to count column width
     */
    private boolean isUnicode = false;
}
