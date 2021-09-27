package com.oceanbase.tools.datamocker.model.enums;

/**
 * How to deal with duplicate data
 *
 * @author yh263208
 * @date 2020-12-22 21:48
 * @since OBMOCKER-snapshot-0.1.0
 */
public enum DuplicateStrategy {
    /**
     * Ignore the conflict
     */
    IGNORE,
    /**
     * Coverage conflict
     */
    OVERWRITE,
    /**
     * Terminate write
     */
    TERMINATE
}
