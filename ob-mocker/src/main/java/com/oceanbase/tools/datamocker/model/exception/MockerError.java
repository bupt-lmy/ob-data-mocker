package com.oceanbase.tools.datamocker.model.exception;

/**
 * mock数据异常情况枚举
 *
 * @author yh263208
 * @date 2020-12-10 17:21
 * @since OBMOCKER_snapshot_0.1.0
 */
public enum MockerError {
    /**
     * 未知错误
     */
    UNKNOWN_ERROR("unknown error"),
    /**
     * 参数错误
     */
    PARAMETER_ERROR("input parameter is illegal"),
    /**
     * 输入的值超出范围
     */
    VALUE_OUT_OFRANGE("input value is out of range"),
    /**
     * 非法的返回值
     */
    ILLEGAL_RETURN_VALUE("return value is illegal"),
    /**
     * 非法的OB模式，目前只支持mysql模式以及oracle模式
     */
    INVALID_OB_MODE("ob dialect type in illegal"),
    /**
     * 操作错误，表示一些操作性质的代码，例如创建文件，目录失败
     */
    OPERATION_FAILURE("fail to execute some operations"),
    /**
     * 特性不支持错误枚举
     */
    NOT_SUPPORT_FEATURE("this feature is not support yet"),
    /**
     * 未知列异常，意义为未知的列名
     */
    UNKNOWN_COLUMN_NAME("this column is unknown"),
    /**
     * 未知数据生成器异常
     */
    UNKNOWN_DATA_GENERATOR("target data generator is unknown"),
    /**
     * 未知数据类型异常
     */
    UNKNOWN_DATA_TYPE("target data type is unknown"),
    /**
     * 未知约束异常
     */
    UNKNOWN_CONSTRAINT("target constraint is unkown"),
    /**
     * 执行sql失败
     */
    FAIL_TO_EXECUTE_SQL("fail to execute a sql");
    /**
     * 错误信息
     */
    private final String msg;

    MockerError(String msg) {
        this.msg = msg;
    }

    public String getMessage() {
        return this.msg;
    }
}
