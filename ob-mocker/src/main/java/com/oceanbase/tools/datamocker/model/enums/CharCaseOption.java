package com.oceanbase.tools.datamocker.model.enums;

import org.apache.commons.lang.StringUtils;

/**
 * 字符大小写设定枚举
 *
 * @author yh263208
 * @date 2020-12-16 22:52
 * @since OBMOCKER_snapshot_0.1.0
 */
public enum CharCaseOption {
    /**
     * 全部转为大写
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
     * 全部转为小写
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
     * 默认值
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
