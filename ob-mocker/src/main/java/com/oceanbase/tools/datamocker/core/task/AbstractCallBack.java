package com.oceanbase.tools.datamocker.core.task;

/**
 * Callback method, used to execute some call back logic
 *
 * @author yh263208
 * @date 2021-04-14 11:30
 * @since OBMOCKER_snapshot_0.1.1
 */
public abstract class AbstractCallBack<T> {
    /**
     * Has the onSuccess method been executed
     */
    private boolean onSuccessFlag;
    /**
     * Has the onFailure method been executed
     */
    private boolean onFailureFlag;

    public AbstractCallBack() {
        onSuccessFlag = false;
        onFailureFlag = false;
    }

    /**
     * OnSuccess method, which is executed when operation is success
     *
     * @param param custom parameter
     * @throws Throwable exception is allow to be thrown when onSuccess method executed
     */
    public void onSuccess(T param) throws Throwable {
        if (!onFailureFlag && !onSuccessFlag) {
            onSuccessFlag = true;
            doOnSuccess(param);
        }
    }

    protected abstract void doOnSuccess(T param) throws Throwable;

    /**
     * OnFailure method, which is executed when operation is failed
     *
     * @param param custom parameter
     * @param e     input exception
     * @throws Throwable exception is allow to be thrown when onFailure method executed
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
