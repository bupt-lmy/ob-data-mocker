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
package com.oceanbase.tools.datamocker.schedule;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import com.oceanbase.tools.datamocker.core.task.TableTask;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import lombok.NonNull;

/**
 * The mock data thread pool executes the serivce object, used to encapsulate the call and execution
 * of the thread pool
 *
 * @author yh263208
 * @date 2021-01-18 11:24
 * @since OBMOCKER_0.1.0_snapshot
 */
public class MockExecutorService {

    private final ThreadPoolExecutor executor;

    public MockExecutorService(@NonNull ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    private <V> RunnableFuture<V> newTaskFor(Callable<V> task) {
        return new FutureTask<>(task);
    }

    public synchronized TableTaskContext submit(@NonNull TableTask tableTask) {
        tableTask.getContext().setStatus(MockTaskStatus.RUNNING);
        submitCallable(tableTask.getBeforeTask(), tableTask.getContext());
        return tableTask.getContext();
    }

    public synchronized <V> void submitCallable(@NonNull Callable<V> task, @NonNull TableTaskContext context) {
        if (!context.isShutdown() && !isShutdown()) {
            Future<?> future = executor.submit(newTaskFor(task));
            context.appendHandle(future);
        }
    }

    public synchronized <V> void submitCallable(@NonNull Callable<V> task) {
        executor.submit(newTaskFor(task));
    }

    public void shutdown() {
        this.executor.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return this.executor.shutdownNow();
    }

    public boolean isShutdown() {
        return this.executor.isShutdown();
    }

    public int getActiveCount() {
        return this.executor.getActiveCount();
    }

    public int getCorePoolSize() {
        return this.executor.getCorePoolSize();
    }

    public int getMaximumPoolSize() {
        return this.executor.getMaximumPoolSize();
    }

}
