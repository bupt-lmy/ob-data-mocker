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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.core.Dispatcher;
import com.oceanbase.tools.datamocker.core.task.AbstractCallBack;
import com.oceanbase.tools.datamocker.core.task.TableTask;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.core.task.TableTaskInfo;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.MDC;

/**
 * Abstract scheduler, through the realization of the scheduler to achieve task thread scheduling
 *
 * @author yh263208
 * @date 2021-01-18 00:37
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public abstract class AbstractScheduler {

    private final long startTimestamp;
    private final MockExecutorService executorService;

    public AbstractScheduler() {
        ThreadPoolExecutor executor = pool();
        Validate.isTrue(executor.getCorePoolSize() == executor.getMaximumPoolSize(),
                "core pool size has to be equal to max pool size");
        Validate.isTrue(executor.getCorePoolSize() >= 5, "core pool size of thread pool can not be smaller than 5");
        this.startTimestamp = System.currentTimeMillis();
        this.executorService = new MockExecutorService(executor);
    }

    /**
     * Task execution method, the abstract scheduler uses this method for the actual execution of the
     * task
     *
     * @param dispatcher Dispatcher object
     * @return Total number of tasks performed
     */
    public MockContext execute(Dispatcher<TableTaskInfo> dispatcher) {
        log.info("Thread pool's initialization has been done. coreSize={},maxSize={}",
                this.executorService.getCorePoolSize(), this.executorService.getMaximumPoolSize());
        MockContext context = new MockContext(this.executorService, dispatcher);
        AbstractScheduler thisScheduler = this;
        int width = dispatcher.getWidth();
        // 标识数组，数组长度和tasks的任务队列数量相同，每一位分别用于标示对应任务队列中是否还有任务等待执行
        boolean[] flags = new boolean[width];
        for (int i = 0; i < width; i++) {
            flags[i] = true;
        }
        Callable<Integer> scheduleTask = () -> {
            MDC.put("mocktask.workspace", context.getLogDir());
            int totalCount = 0;
            int total = 0;
            long maxTimeout = 0L;
            Long timeoutSum = 0L;
            for (int i = 0; i < dispatcher.getWidth(); i++) {
                for (int j = 0; j < dispatcher.getTaskSize(i); j++) {
                    TableTaskInfo tableTask = dispatcher.getObj(i, j);
                    Long timeout = tableTask.getMetaData().getTimeoutMillis();
                    timeoutSum += timeout;
                    if (maxTimeout < timeout) {
                        maxTimeout = timeout;
                    }
                }
            }
            long failCount = 0;
            long maxFailCount = maxTimeout / 5000L + 36;
            while (!Thread.currentThread().isInterrupted() && interval() < timeoutSum) {
                for (int i = 0; i < width; i++) {
                    if (!flags[i]) {
                        continue;
                    }
                    TableTaskInfo tableTaskInfo = dispatcher.getObj(i, 0);
                    if (tableTaskInfo == null) {
                        total++;
                        flags[i] = false;
                        continue;
                    }
                    Set<Set<String>> columnGroups = scheduleColumnTask(tableTaskInfo.columnGroups(),
                            executorService.getActiveCount(), executorService.getCorePoolSize(),
                            executorService.getMaximumPoolSize());
                    if (columnGroups == null) {
                        Thread.sleep(5000);
                        log.warn("Insufficient thread resources, will retry, schema={}, tableName={}",
                                tableTaskInfo.getMetaData().getTableSchema(),
                                tableTaskInfo.getMetaData().getTableName());
                        if ((failCount++) > maxFailCount) {
                            log.warn("Task scheduling operation timeout, will exit, timeout={} min",
                                    maxTimeout / 60000 + 3);
                            return totalCount;
                        }
                        continue;
                    }
                    validateSet(columnGroups);
                    failCount = 0;
                    if (!validateThreadResource(columnGroups, executorService.getActiveCount(),
                            executorService.getMaximumPoolSize())) {
                        int required = columnGroups.size();
                        log.warn("The thread resource requirements given by the custom scheduling algorithm exceed the "
                                + "currently available thread resources, requiredThreadCount={}, availableThreadCount={}",
                                required, this.executorService.getMaximumPoolSize() - executorService.getActiveCount());
                        return totalCount;
                    }
                    dispatcher.pop(i);
                    flags[i] = false;
                    // 初始化TaskBean，主要是定义TaskBean的回调函数
                    TableTask tableTask = new TableTask(tableTaskInfo, columnGroups, i);
                    tableTask.getContext().setStatus(MockTaskStatus.PENDING);
                    tableTask.init(executorService, new AbstractCallBack<TableTaskContext>() {
                        @Override
                        public void doOnSuccess(TableTaskContext param) {
                            flags[param.getTopIndex()] = true;
                            try {
                                thisScheduler.onSuccess(param);
                            } catch (Exception e) {
                                log.warn("Failed to call onSuccess", e);
                            }
                        }

                        @Override
                        public void doOnFailure(TableTaskContext param, Throwable e) {
                            flags[param.getTopIndex()] = true;
                            try {
                                thisScheduler.onFailure(param, e);
                            } catch (Exception e1) {
                                log.warn("Failed to call onFailure", e);
                            }
                        }
                    });
                    if (executorService.isShutdown()) {
                        log.warn("Task has been shutdown, total task executed is {}", totalCount);
                        return totalCount;
                    }
                    totalCount++;
                    TableTaskContext tableTaskContext = executorService.submit(tableTask);
                    context.appendContext(tableTaskContext);
                }
                if (total >= width) {
                    break;
                }
            }
            log.info("Scheduled task has been completed, totalTask={}, duration={}", totalCount,
                    DurationFormatUtils.formatDurationHMS(interval()));
            MDC.clear();
            return totalCount;
        };
        this.executorService.submitCallable(scheduleTask);
        return context;
    }

    /**
     * Verify that the thread resources are sufficient. This method does not throw an exception. If the
     * verification fails, an exception will be thrown directly
     *
     * @param columnGroups Column primitive grouping ID collection
     * @param active Tasks currently active in the thread pool
     * @param max The maximum number of threads in the thread pool
     * @return Return verification result
     */
    private boolean validateThreadResource(Set<Set<String>> columnGroups, int active, int max) {
        int freeResource = max - active;
        if (freeResource < 0) {
            throw new IllegalStateException("Free resource thread pool size is smaller than zero");
        }
        return columnGroups.size() < freeResource;
    }

    /**
     * Verify that the user implements the interface to return the column primitive grouping set is
     * legal, the verification standard is that there can be no intersection between the grouping ID
     * sets
     *
     * @param input Input group set
     * @throws MockerException If the verification fails, an exception is thrown
     */
    private void validateSet(Set<Set<String>> input) {
        List<Set<String>> middle = new ArrayList<>(input);
        for (int i = 0; i < middle.size(); i++) {
            Set<String> copyObj = new HashSet<>(middle.get(i));
            for (int j = i + 1; j < middle.size(); j++) {
                copyObj.retainAll(middle.get(j));
                if (copyObj.size() != 0) {
                    throw new MockerException(MockerError.PARAMETER_ERROR, "Column group set is illegal");
                }
            }
        }
    }

    private long interval() {
        return System.currentTimeMillis() - startTimestamp;
    }

    protected ThreadPoolExecutor pool() {
        int corePoolSize = Math.max(Runtime.getRuntime().availableProcessors(), 5);
        return new ThreadPoolExecutor(corePoolSize, corePoolSize, 0,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new BasicThreadFactory.Builder().namingPattern("ob-data-mocker-thread-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * The thread scheduling abstract method of column generation primitives, through which the
     * scheduling of column primitives is realized
     *
     * @param groups Set of grouping IDs of column primitives
     * @param active The number of active tasks in the current thread pool
     * @param core The core size of the current thread pool
     * @param max The maximum size of the current thread pool
     * @return Return to the group group, each group is allocated a thread resource
     */
    protected abstract Set<Set<String>> scheduleColumnTask(Set<String> groups, int active, int core, int max);

    protected abstract void onSuccess(TableTaskContext context);

    protected abstract void onFailure(TableTaskContext context, Throwable e);

}
