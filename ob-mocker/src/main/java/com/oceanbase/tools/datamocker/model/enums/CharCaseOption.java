package com.oceanbase.tools.datamocker.model.enums;

import org.apache.commons.lang.StringUtils;

/**
 * Character case setting enumeration
 *
 * @author yh263208
 * @date 2020-12-16 22:52
 * @since OBMOCKER_snapshot_0.1.0
 */
public enum CharCaseOption {
    /**
     * Convert all to uppercase
     */
    ALL_UPPER_CASE {
        @Override
        public String convert(String value) {
            if (StringUtils.isNotBlank(value)) {
                return value.toUpperCase();
            }
            return null;
        }
    },
    /**
     * All lowercase
     */
    ALL_LOWER_CASE {
        @Override
        public String convert(String value) {
            if (StringUtils.isNotBlank(value)) {
                return value.toLowerCase();
            }
            return null;
        }
    },
    /**
     * Defaults
     */
    DEFAULT {
        @Override
        public String convert(String value) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
            return null;
        }
    };

    abstract public String convert(String value);
}
