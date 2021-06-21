package com.oceanbase.tools.datamocker.model.enums;

/**
 * Character set enumeration of string type
 *
 * @author yh263208
 * @date 2020-12-06 21:39
 * @since OBMOCKER_snapshot_0.1.0
 */
public enum CharsetType {
    /**
     * utf-8 character encoding format
     */
    UTF_8 {
        @Override
        public String getCharSet() {
            return "UTF-8";
        }
    },
    /**
     * utf-8 character encoding format
     */
    AL32UTF8 {
        @Override
        public String getCharSet() {
            return "UTF-8";
        }
    },
    /**
     * utf-8 character encoding format
     */
    UTF8MB4 {
        @Override
        public String getCharSet() {
            return "UTF-8";
        }
    },
    /**
     * gbk character encoding format
     */
    GBK {
        @Override
        public String getCharSet() {
            return "GBK";
        }
    };

    /**
     * Get the string name of the encoding format
     *
     * @return Return name
     */
    abstract public String getCharSet();
}
