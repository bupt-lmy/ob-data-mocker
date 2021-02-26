package com.oceanbase.tools.datamocker.core.task;

/**
 * 通用回调函数，用于在任务结束之前进行一次回调操作
 *
 * @author yh263208
 * @date 2021-01-13 17:28
 * @since OBMOCKER_snapshot_0.1.0
 */
public interface CallBackMethod<T> {
    /**
     * 具体的回调方法
     *
     * @param result 回调函数需要带的参数
     */
    void execute(T result);
}
