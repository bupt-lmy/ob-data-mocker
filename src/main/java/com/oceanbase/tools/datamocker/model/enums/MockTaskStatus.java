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
package com.oceanbase.tools.datamocker.model.enums;

/**
 * Task status enumeration of mock data
 *
 * @author yh263208
 * @date 2021-01-17 22:59
 * @since OBMOCKER_snapshot_0.1.0
 */
public enum MockTaskStatus {
    /**
     * Task is running
     */
    RUNNING,
    /**
     * Task is being scheduled
     */
    PENDING,
    /**
     * The task was executed successfully
     */
    SUCCESS,
    /**
     * Task execution failed
     */
    FAILED,
    /**
     * Task created successfully
     */
    CREATED,
    /**
     * Task is interrupted
     */
    CANCELED;
}
