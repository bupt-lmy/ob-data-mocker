package com.oceanbase.tools.datamocker.model.enums;

/**
 * 字符串类型的字符集枚举
 *
 * @author yh263208
 * @date 2020-12-06 21:39
 * @since OBMOCKER_snapshot_0.1.0
 */
public enum CharsetType {
    /**
     * utf-8字符编码格式
     */
    UTF_8 {
        @Override
        public String getCharSet() {
            return "UTF-8";
        }
    },
    /**
     * utf-8字符编码格式
     */
    AL32UTF8 {
        @Override
        public String getCharSet() {
            return "UTF-8";
        }
    },
    /**
     * utf-8字符编码格式
     */
    UTF8MB4 {
        @Override
        public String getCharSet() {
            return "UTF-8";
        }
    },
    /**
     * GBK编码格式
     */
    GBK {
        @Override
        public String getCharSet() {
            return "GBK";
        }
    };

    /**
     * 获取编码格式的字符串名称
     *
     * @return 返回名称
     */
    abstract public String getCharSet();
}
