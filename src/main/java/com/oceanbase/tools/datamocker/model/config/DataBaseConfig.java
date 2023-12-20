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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Database connection information configuration package object
 *
 * @author yh263208
 * @date 2020-12-24 15:30
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
@ToString(exclude = {"password"})
public class DataBaseConfig {
    private String host;
    private Integer port;
    private String user;
    private String tenant;
    private String cluster;
    private String password;
    private String defaultSchame;
    /**
     * Database connection parameters
     */
    private Map<String, String> connectParam;
}
