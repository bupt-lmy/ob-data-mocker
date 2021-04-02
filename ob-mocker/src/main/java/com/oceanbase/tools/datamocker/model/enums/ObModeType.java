package com.oceanbase.tools.datamocker.model.enums;

/**
 * OB模式枚举，目前支持OB的mysql模式以及OB的oracle模式
 *
 * @author yh263208
 * @date 2020-12-11 14:34
 * @since ODCMOCKER_snapshot_0.1.0
 */
public enum ObModeType {
    /**
     * OB的mysql模式
     */
    OB_MYSQL,
    /**
     * OB的oracle模式
     */
    OB_ORACLE,
    /**
     * 未知的ob模式
     */
    UNKNOWN;
}
