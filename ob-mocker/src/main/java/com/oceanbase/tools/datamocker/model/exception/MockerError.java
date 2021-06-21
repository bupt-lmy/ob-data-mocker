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
