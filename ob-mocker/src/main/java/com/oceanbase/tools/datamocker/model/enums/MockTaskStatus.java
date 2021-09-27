package com.oceanbase.tools.datamocker.model.enums;

/**
 * Task status enumeration of mock data
 *
 * @author yh263208
 * @date 2021-01-17 22:59
 * @since OBMOCKER_snapshot_0.1.0
 */
public enum MockTaskStatus {
    /**
     * Task is running
     */
    RUNNING,
    /**
     * Task is being scheduled
     */
    PENDING,
    /**
     * The task was executed successfully
     */
    SUCCESS,
    /**
     * Task execution failed
     */
    FAILED,
    /**
     * Task created successfully
     */
    CREATED,
    /**
     * Task is interrupted
     */
    CANCELED;
}
