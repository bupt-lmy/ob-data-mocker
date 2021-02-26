package com.oceanbase.tools.datamocker.model.enums;

/**
 * 重复数据的处理方式
 *
 * @author yh263208
 * @date 2020-12-22 21:48
 * @since OBMOCKER-snapshot-0.1.0
 */
public enum DuplicateStrategy {
    /**
     * 忽略冲突
     */
    IGNORE,
    /**
     * 覆盖冲突
     */
    OVERWRITE,
    /**
     * 终止写入
     */
    TERMINATE
}
