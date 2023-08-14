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
package com.oceanbase.tools.datamocker.model.config.impl;

import java.util.List;

import com.oceanbase.tools.datamocker.model.config.AbstractTaskConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import lombok.Getter;
import lombok.Setter;

/**
 * Overall task configuration object, used to configure overall task parameters
 *
 * @author yh263208
 * @date 2020-12-27 20:59
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class DefaultTaskConfig extends AbstractTaskConfig {
    /**
     * Table generation task collection
     */
    private List<DefaultTableConfig> tables;
    /**
     * The type of dialect that generates the task
     */
    private ObModeType dialectType;
    /**
     * Database configuration object
     */
    private DataBaseConfig dbConfig;
    /**
     * Task name, the default is null, if the user does not pass in a task name, one will be
     * automatically generated here
     */
    private String taskName = null;
    /**
     * The minimum number of connections in the connection pool
     */
    private int minConnectionSize;
    /**
     * Maximum number of connections in the connection pool
     */
    private int maxConnectionSize;
    /**
     * Connection pool connection number amplification step
     */
    private int connectionIncreasementStep;

    @Override
    public ObModeType obDialectType() {
        return dialectType;
    }

    @Override
    public DataBaseConfig dbConfig() {
        return dbConfig;
    }

    @Override
    public List<DefaultTableConfig> tasks() {
        return tables;
    }

    @Override
    public String taskName() {
        return taskName;
    }

    @Override
    protected int connectionIncreaseStepCount() {
        return this.connectionIncreasementStep;
    }

    @Override
    protected int connectionInitCount() {
        return this.minConnectionSize;
    }

    @Override
    protected int connectionMaxCount() {
        return this.maxConnectionSize;
    }
}
