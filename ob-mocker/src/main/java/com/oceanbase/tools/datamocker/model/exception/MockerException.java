package com.oceanbase.tools.datamocker.model.exception;

import lombok.Getter;

/**
 * Mock data exception object encapsulation
 *
 * @author yh263208
 * @date 2020-12-10 16:53
 * @since OBMOCK_snapshot_0.1.0
 */
public class MockerException extends RuntimeException {
    /**
     * error code
     */
    @Getter
    private final String errorCode;

    /**
     * Exception constructor
     *
     * @param errorEnum Error type enumeration
     */
    public MockerException(MockerError errorEnum) {
        super(errorEnum.getMessage());
        this.errorCode = errorEnum.name();
    }

    /**
     * Exception constructor
     *
     * @param errorEnum Error type enumeration
     * @param errorMsg  Error message
     */
    public MockerException(MockerError errorEnum, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorEnum.name();
    }

    /**
     * Exception constructor
     *
     * @param errMsg Error message
     */
    public MockerException(String errMsg) {
        super(errMsg);
        this.errorCode = MockerError.UNKNOWN_ERROR.name();
    }

    /**
     * Default Exception constructor
     */
    public MockerException() {
        super(MockerError.UNKNOWN_ERROR.getMessage());
        this.errorCode = MockerError.UNKNOWN_ERROR.name();
    }

    /**
     * Exception constructor
     *
     * @param originalThrowable The original exception throwing class
     */
    public MockerException(Throwable originalThrowable) {
        super(originalThrowable.getMessage());
        this.initCause(originalThrowable);
        this.errorCode = MockerError.UNKNOWN_ERROR.name();
    }

}
