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
import java.util.Map;
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
import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.PrintUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.Validate;
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
    /**
     * The initial size of the thread pool
     */
    private static final int CORE_POOL_SIZE;
    /**
     * The maximum size of the thread pool
     */
    private static final int MAX_POOL_SIZE;
    /**
     * Object encapsulation of thread pool
     */
    private final MockExecutorService service;
    private final long startTimestamp;

    static {
        CORE_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 5);
        MAX_POOL_SIZE = CORE_POOL_SIZE;
    }

    public AbstractScheduler() {
        ThreadPoolExecutor executor = pool();
        if (executor == null) {
            executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 0, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
        }
        Validate.isTrue(executor.getCorePoolSize() == executor.getMaximumPoolSize(),
                "core pool size has to be equal to max pool size");
        Validate.isTrue(executor.getCorePoolSize() >= 5, "core pool size of thread pool can not be smaller than 5");
        service = new MockExecutorService(executor);
        startTimestamp = System.currentTimeMillis();
    }

    /**
     * Task execution method, the abstract scheduler uses this method for the actual execution of the
     * task
     *
     * @param dispatcher Dispatcher object
     * @return Total number of tasks performed
     */
    public MockContext execute(Dispatcher<TableTaskInfo> dispatcher) {
        log.info("Thread pool's initialization has been done. coreSize={},maxSize={}", CORE_POOL_SIZE, MAX_POOL_SIZE);
        MockContext context =
                new MockContext(this.service, dispatcher.getTaskId(), dispatcher.getName(), dispatcher.totalCount());
        AbstractScheduler thisScheduler = this;
        int concurrentCount = dispatcher.getConcurrent();
        // 标识数组，数组长度和tasks的任务队列数量相同，每一位分别用于标示对应任务队列中是否还有任务等待执行
        boolean[] flags = new boolean[concurrentCount];
        for (int i = 0; i < concurrentCount; i++) {
            flags[i] = true;
        }
        Callable<Integer> scheduleTask = () -> {
            MDC.put("mocktask.workspace", context.getTaskId());
            int totalCount = 0;
            int total = 0;
            long maxTimeout = 0L;
            Long timeoutSum = 0L;
            for (int i = 0; i < dispatcher.getConcurrent(); i++) {
                for (int j = 0; j < dispatcher.getTaskSize(i); j++) {
                    TableTaskInfo tableTask = dispatcher.getObj(i, j);
                    Long timeout = tableTask.getMetaData().getTimeoutMilliseconds();
                    timeoutSum += timeout;
                    if (maxTimeout < timeout) {
                        maxTimeout = timeout;
                    }
                }
            }
            long failCount = 0;
            long maxFailCount = maxTimeout / 5000L + 36;
            while (!Thread.currentThread().isInterrupted() && interval() < timeoutSum) {
                for (int i = 0; i < concurrentCount; i++) {
                    if (flags[i]) {
                        TableTaskInfo task = dispatcher.getObj(i, 0);
                        if (task != null) {
                            Map<Set<String>, Integer> dataGroups =
                                    scheduleDataTask(task.dataWriteGroups(), service.getActiveCount(),
                                            service.getCorePoolSize(), service.getMaximumPoolSize());
                            if (dataGroups == null) {
                                Thread.sleep(5000);
                                log.warn("Insufficient thread resources, will retry, schema={}, tableName={}",
                                        task.getMetaData().getTableSchema(), task.getMetaData().getTableName());
                                if ((failCount++) > maxFailCount) {
                                    log.warn(
                                            "Task scheduling operation timed out, the scheduling thread will exit, timeout={} min",
                                            maxTimeout / 60000 + 3);
                                    clearResource(dispatcher);
                                    return totalCount;
                                }
                                continue;
                            }
                            int currentActive = 0;
                            for (Map.Entry<Set<String>, Integer> entry : dataGroups.entrySet()) {
                                currentActive += entry.getValue();
                            }
                            Set<Set<String>> columnGroups = scheduleColumnTask(task.columnGroups(),
                                    service.getActiveCount() + currentActive, service.getCorePoolSize(),
                                    service.getMaximumPoolSize());
                            if (columnGroups == null) {
                                Thread.sleep(5000);
                                log.warn("Insufficient thread resources, will retry, schema={}, tableName={}",
                                        task.getMetaData().getTableSchema(), task.getMetaData().getTableName());
                                if ((failCount++) > maxFailCount) {
                                    log.warn(
                                            "Task scheduling operation timed out, the scheduling thread will exit, timeout={} min",
                                            maxTimeout / 60000 + 3);
                                    clearResource(dispatcher);
                                    return totalCount;
                                }
                                continue;
                            }
                            validateSet(columnGroups);
                            failCount = 0;
                            if (!validateThreadResource(columnGroups, dataGroups, service.getActiveCount(),
                                    service.getMaximumPoolSize())) {
                                int required = columnGroups.size();
                                Set<Map.Entry<Set<String>, Integer>> entrySet = dataGroups.entrySet();
                                for (Map.Entry<Set<String>, Integer> entry : entrySet) {
                                    required += entry.getValue();
                                }
                                log.warn(
                                        "The thread resource requirements given by the custom scheduling algorithm exceed the currently "
                                                + "available thread resources, requiredThreadCount={}, availableThreadCount={}",
                                        required, this.service.getMaximumPoolSize() - service.getActiveCount());
                                clearResource(dispatcher);
                                return totalCount;
                            }
                            dispatcher.pop(i);
                            flags[i] = false;
                            // 初始化TaskBean，主要是定义TaskBean的回调函数
                            TableTask mockTaskBean =
                                    new TableTask(task, columnGroups, dataGroups, dispatcher.getName(), i);
                            mockTaskBean.getContext().setStatus(MockTaskStatus.PENDING);
                            mockTaskBean.init(service, new AbstractCallBack<TableTaskContext>() {
                                @Override
                                public void doOnSuccess(TableTaskContext param) {
                                    try {
                                        if (param.getDataSource() instanceof AutoCloseable) {
                                            ((AutoCloseable) param.getDataSource()).close();
                                        }
                                    } catch (Exception e) {
                                        // eat exception
                                    }
                                    for (MockerFile fileManager : param.getFileManagers()) {
                                        fileManager.close();
                                    }
                                    flags[param.getTopIndex()] = true;
                                    try {
                                        thisScheduler.onSuccess(param);
                                    } catch (Throwable e) {
                                        log.error("Some errors happened when scheduler onSuccess executed", e);
                                    }
                                }

                                @Override
                                public void doOnFailure(TableTaskContext param, Throwable e) {
                                    try {
                                        if (param.getDataSource() instanceof AutoCloseable) {
                                            ((AutoCloseable) param.getDataSource()).close();
                                        }
                                    } catch (Exception ex) {
                                        // eat exception
                                    }
                                    for (MockerFile fileManager : param.getFileManagers()) {
                                        fileManager.close();
                                    }
                                    flags[param.getTopIndex()] = true;
                                    try {
                                        thisScheduler.onFailure(param, e);
                                    } catch (Throwable e1) {
                                        log.error("Some errors happened when scheduler onFailure executed", e);
                                    }
                                }
                            });
                            if (service.isShutdown()) {
                                try {
                                    if (task.getDataSource() instanceof AutoCloseable) {
                                        ((AutoCloseable) task.getDataSource()).close();
                                    }
                                } catch (Exception e) {
                                    // eat exception
                                }
                                for (MockerFile manager : task.getFileManagers()) {
                                    manager.close();
                                }
                                clearResource(dispatcher);
                                log.warn("Task has been shutdown, total task executed is {}", totalCount);
                                return totalCount;
                            }
                            totalCount++;
                            TableTaskContext mockContext = service.submit(mockTaskBean);
                            context.appendContext(mockContext);
                        } else {
                            total++;
                            flags[i] = false;
                        }
                    }
                }
                if (total >= concurrentCount) {
                    break;
                }
            }
            clearResource(dispatcher);
            log.info("Scheduled task execution completed, totalTaskExecuted={}, duration={}", totalCount,
                    PrintUtil.convertToReadableTimeString(interval(), TimeUnit.MILLISECONDS, TimeUnit.MINUTES,
                            TimeUnit.SECONDS));
            MDC.clear();
            return totalCount;
        };
        this.service.submitCallable(scheduleTask);
        return context;
    }

    /**
     * Before the scheduler thread exits, it is necessary to clean up the resources of the unfinished
     * tasks inside the dispatcher object
     *
     * @param dispatcher Dispatcher object
     * @throws Exception The release of resources may be abnormal
     */
    private void clearResource(Dispatcher<TableTaskInfo> dispatcher) throws Exception {
        for (int i = 0; i < dispatcher.getConcurrent(); i++) {
            for (int j = 0; j < dispatcher.getTaskSize(i); j++) {
                TableTaskInfo bean = dispatcher.getObj(i, j);
                try {
                    if (bean.getDataSource() instanceof AutoCloseable) {
                        ((AutoCloseable) bean.getDataSource()).close();
                    }
                } catch (Exception e) {
                    // eat exception
                }
                for (MockerFile manager : bean.getFileManagers()) {
                    manager.close();
                }
            }
        }
    }

    /**
     * Verify that the thread resources are sufficient. This method does not throw an exception. If the
     * verification fails, an exception will be thrown directly
     *
     * @param columnGroups Column primitive grouping ID collection
     * @param dataGroups Data generation primitive group ID collection
     * @param active Tasks currently active in the thread pool
     * @param max The maximum number of threads in the thread pool
     * @return Return verification result
     */
    private boolean validateThreadResource(Set<Set<String>> columnGroups, Map<Set<String>, Integer> dataGroups,
            int active, int max) {
        int freeResource = max - active;
        if (freeResource < 0) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, "Free resource thread pool size is smaller than zero");
        }
        int required = columnGroups.size();
        Set<Map.Entry<Set<String>, Integer>> entrySet = dataGroups.entrySet();
        for (Map.Entry<Set<String>, Integer> entry : entrySet) {
            if (entry.getValue() <= 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Thread count can not be smaller than zero");
            }
            required += entry.getValue();
        }
        return required < freeResource;
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

    /**
     * Return the duration of the scheduled task
     *
     * @return Return the execution time of the scheduled task
     */
    private long interval() {
        return System.currentTimeMillis() - startTimestamp;
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
    abstract protected Set<Set<String>> scheduleColumnTask(Set<String> groups, int active, int core, int max);

    /**
     * An abstract scheduling method for data writing primitives, through which data primitives are
     * dispatched
     *
     * @param groups Group ID of the data primitive
     * @param active Number of active tasks in the thread pool
     * @param core The core size of the thread pool
     * @param max Maximum capacity of thread pool
     * @return Returns the number of threads allocated for each grouping set
     */
    abstract protected Map<Set<String>, Integer> scheduleDataTask(Set<String> groups, int active, int core, int max);

    /**
     * Callback method called after task execution is complete
     *
     * @param context Execution context of mock task
     */
    protected abstract void onSuccess(TableTaskContext context);

    /**
     * Callback method to be called after the task execution is completed, when the task fails
     *
     * @param context Execution context of mock task
     */
    protected abstract void onFailure(TableTaskContext context, Throwable e);

    /**
     * Get the thread pool object, if you want to use the default, you can directly return null
     *
     * @return Return thread pool object
     */
    public abstract ThreadPoolExecutor pool();
}
