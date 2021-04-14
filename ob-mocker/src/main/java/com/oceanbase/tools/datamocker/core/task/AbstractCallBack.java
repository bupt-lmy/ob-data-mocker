package com.oceanbase.tools.datamocker.core.task;

/**
 * 抽象回调函数类，用于执行回调函数
 *
 * @author yh263208
 * @date 2021-04-14 11:30
 * @since OBMOCKER_snapshot_0.1.1
 */
public abstract class AbstractCallBack<T> {
    /**
     * 是否已经执行了onSuccess方法
     */
    private boolean onSuccessFlag;
    /**
     * 是否已经执行了onFailure方法
     */
    private boolean onFailureFlag;

    public AbstractCallBack() {
        onSuccessFlag = false;
        onFailureFlag = false;
    }

    /**
     * 成功时的回调函数
     *
     * @param param 参数
     * @throws Throwable 成功的回调函数允许抛出异常
     */
    public void onSuccess(T param) throws Throwable {
        if (!onFailureFlag && !onSuccessFlag) {
            onSuccessFlag = true;
            doOnSuccess(param);
        }
    }

    protected abstract void doOnSuccess(T param) throws Throwable;

    /**
     * 失败时的回调函数
     *
     * @param param 回调函数的参数
     */
    public void onFailure(T param, Throwable e) throws Throwable {
        if (onSuccessFlag || onFailureFlag) {
            throw e;
        } else {
            onFailureFlag = true;
            doOnFailure(param, e);
        }
    }

    protected abstract void doOnFailure(T param, Throwable e) throws Throwable;

}
