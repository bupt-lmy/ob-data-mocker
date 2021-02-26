package com.oceanbase.tools.datamocker.model.exception;

/**
 * mock数据异常对象封装
 *
 * @author yh263208
 * @date 2020-12-10 16:53
 * @since OBMOCK_snapshot_0.1.0
 */
public class MockerException extends RuntimeException {
    /**
     * 错误码
     */
    private String errorCode;
    /**
     * 原始的错误
     */
    private Throwable originalThrowable;

    /**
     * 异常构造函数
     *
     * @param errorEnum 错误类型枚举
     */
    public MockerException(MockerError errorEnum) {
        super(errorEnum.getMessage());
        this.errorCode = errorEnum.name();
    }

    /**
     * 异常构造函数
     *
     * @param errorEnum 错误类型枚举
     * @param errorMsg  错误信息
     */
    public MockerException(MockerError errorEnum, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorEnum.name();
    }

    /**
     * 异常构造函数
     *
     * @param errMsg 错误信息
     */
    public MockerException(String errMsg) {
        super(errMsg);
        this.errorCode = MockerError.UNKNOWN_ERROR.name();
    }

    /**
     * 默认构造函数
     */
    public MockerException() {
        super(MockerError.UNKNOWN_ERROR.getMessage());
        this.errorCode = MockerError.UNKNOWN_ERROR.name();
    }

    /**
     * mock数据异常构造函数
     *
     * @param originalThrowable 原始的异常抛出类
     */
    public MockerException(Throwable originalThrowable) {
        super(originalThrowable);
        this.originalThrowable = originalThrowable;
        this.errorCode = MockerError.UNKNOWN_ERROR.name();
    }

    /**
     * 获取原始的异常对象
     *
     * @return 返回原始的异常对象
     */
    public Throwable getOriginalThrowable() {
        return this.originalThrowable;
    }

    /**
     * 获取错误码
     */
    public String getErrorCode() {
        return this.errorCode;
    }
}
