/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.tools.datamocker.core.task;

import java.util.concurrent.Callable;

import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * Abstract operation task, used to perform some cleaning or initialization operations before the
 * start and end of the mock task
 *
 * @author yh263208
 * @date 2021-01-13 17:25
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public abstract class AbstractMockTask implements Callable<Void> {

    private final long startTimeStamp;
    private final TableTaskMetaData metaData;
    private final TableTaskContext context;
    private AbstractCallBack<TableTaskContext> callBack;
    private volatile boolean cancelled = false;

    public AbstractMockTask(@NonNull TableTaskMetaData metaData, @NonNull TableTaskContext context) {
        this.metaData = metaData;
        this.context = context;
        this.startTimeStamp = System.currentTimeMillis();
    }

    public abstract void execute(TableTaskMetaData metaData, TableTaskContext context) throws Exception;

    @Override
    public Void call() {
        try {
            if (isCancelled()) {
                return null;
            }
            MDC.put("mocktask.workspace", metaData.getLogDir());
            execute(metaData, context);
            if (this.callBack != null) {
                try {
                    this.callBack.onSuccess(context);
                } catch (Throwable e) {
                    boolean shutdownResult = context.terminate();
                    context.setStatus(MockTaskStatus.FAILED);
                    log.warn("Failed to call onSuccess, result={}", shutdownResult, e);
                }
            }
            return null;
        } catch (Throwable e) {
            log.warn("Failed to execute mock data task, message={}", e.getMessage());
            Throwable exception = e;
            while (true) {
                if (exception instanceof InterruptedException) {
                    break;
                }
                exception = exception.getCause();
                if (exception == null) {
                    break;
                }
            }
            if (exception != null) {
                if (MockTaskStatus.CANCELED.equals(context.getStatus())) {
                    /**
                     * data mock task has been interrupted
                     */
                    log.warn("Mock data task is interrupted, duration={}ms,status={}", interval(), context.getStatus());
                } else {
                    context.setStatus(MockTaskStatus.FAILED);
                }
            } else {
                if (!MockTaskStatus.CANCELED.equals(context.getStatus())) {
                    context.setStatus(MockTaskStatus.FAILED);
                }
            }
            context.terminate();
            if (this.callBack != null) {
                MockTaskStatus finalStatus = context.getStatus();
                try {
                    this.callBack.onFailure(context, e);
                } catch (Throwable e1) {
                    log.warn("Failed to call onFailure call back method, status={}", finalStatus, e);
                }
                context.setStatus(finalStatus);
            }
        }
        return null;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return Thread.currentThread().isInterrupted() || this.cancelled;
    }

    protected long interval() {
        return System.currentTimeMillis() - this.startTimeStamp;
    }

    protected long startTime() {
        return this.startTimeStamp;
    }

    public void bind(AbstractCallBack<TableTaskContext> callBack) {
        this.callBack = callBack;
    }

}
