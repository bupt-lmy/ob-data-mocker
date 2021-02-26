package com.oceanbase.tools.datamocker.model.enums;

/**
 * mock数据的任务状态枚举
 *
 * @author yh263208
 * @date 2021-01-17 22:59
 * @since OBMOCKER_snapshot_0.1.0
 */
public enum MockTaskStatus {
    /**
     * 任务正在运行中
     */
    RUNNING,
    /**
     * 任务正在调度中
     */
    PENDING,
    /**
     * 任务执行成功
     */
    SUCCESS,
    /**
     * 任务执行失败
     */
    FAILED,
    /**
     * 任务创建成功
     */
    CREATED,
    /**
     * 任务被中断
     */
    CANCELED;
}
