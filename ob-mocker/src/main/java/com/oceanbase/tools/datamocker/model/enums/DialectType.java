package com.oceanbase.tools.datamocker.model.enums;

/**
 * OB模式枚举，目前支持OB的mysql模式以及OB的oracle模式
 *
 * @author yh263208
 * @date 2020-12-11 14:34
 * @since ODCMOCKER_snapshot_0.1.0
 */
public enum DialectType {
    /**
     * OB的mysql模式
     */
    OB_MYSQL("OB_MYSQL"),
    /**
     * OB的oracle模式
     */
    OB_ORACLE("OB_ORACLE"),
    /**
     * 未知模式
     */
    UNKNOWN("UNKNOWN");

    private String name;

    DialectType(String stringVal) {
        name = stringVal;
    }

    public static DialectType getEnumByString(String code) {
        for (DialectType e : DialectType.values()) {
            if (code.equalsIgnoreCase(e.name)) {
                return e;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
