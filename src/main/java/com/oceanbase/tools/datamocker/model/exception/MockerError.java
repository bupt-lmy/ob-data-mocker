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
package com.oceanbase.tools.datamocker.model.exception;

/**
 * Mock data exception enumeration
 *
 * @author yh263208
 * @date 2020-12-10 17:21
 * @since OBMOCKER_snapshot_0.1.0
 */
public enum MockerError {
    /**
     * unknown mistake
     */
    UNKNOWN_ERROR("unknown error"),
    /**
     * Parameter error
     */
    PARAMETER_ERROR("input parameter is illegal"),
    /**
     * The entered value is out of range
     */
    VALUE_OUT_OFRANGE("input value is out of range"),
    /**
     * Illegal return value
     */
    ILLEGAL_RETURN_VALUE("return value is illegal"),
    /**
     * Illegal OB mode, currently only supports mysql mode and oracle mode
     */
    INVALID_OB_MODE("ob dialect type in illegal"),
    /**
     * Operation error, indicating some operational codes, such as file creation, directory failure
     */
    OPERATION_FAILURE("fail to execute some operations"),
    /**
     * Feature does not support error enumeration
     */
    NOT_SUPPORT_FEATURE("this feature is not support yet"),
    /**
     * Unknown column exception, meaning unknown column name
     */
    UNKNOWN_COLUMN_NAME("this column is unknown"),
    /**
     * Unknown data generator exception
     */
    UNKNOWN_DATA_GENERATOR("target data generator is unknown"),
    /**
     * Unknown data type exception
     */
    UNKNOWN_DATA_TYPE("target data type is unknown"),
    /**
     * Unknown constraint exception
     */
    UNKNOWN_CONSTRAINT("target constraint is unkown"),
    /**
     * SQL execution failed
     */
    FAIL_TO_EXECUTE_SQL("fail to execute a sql");
    /**
     * Error message
     */
    private final String msg;

    MockerError(String msg) {
        this.msg = msg;
    }

    public String getMessage() {
        return this.msg;
    }
}
