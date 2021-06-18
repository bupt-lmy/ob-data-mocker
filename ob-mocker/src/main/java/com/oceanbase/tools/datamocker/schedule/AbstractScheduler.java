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
import com.oceanbase.tools.datamocker.core.write.output.MockerDataSource;
import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.Validate;
import org.slf4j.MDC;

/**
 * 抽象调度器，通过实现该调度器实现任务的线程调度
 *
 * @author yh263208
 * @date 2021-01-18 00:37
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public abstract class AbstractScheduler {
    /**
     * 线程池的初始大小
     */
    private static final int CORE_POOL_SIZE;
    /**
     * 线程池的最大大小
     */
    private static final int MAX_POOL_SIZE;
    /**
     * 线程池的对象封装
     */
    private MockExecutorService service;
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
        Validate.isTrue(executor.getCorePoolSize() == executor.getMaximumPoolSize(), "core pool size has to be equal to max pool size");
        Validate.isTrue(executor.getCorePoolSize() >= 5, "core pool size of thread pool can not be smaller than 5");
        service = new MockExecutorService(executor);
        startTimestamp = System.currentTimeMillis();
    }

    /**
     * 任务执行方法，抽象调度器使用该方法进行任务的实际执行
     *
     * @param dispatcher 分发器对象
     * @return 一共执行的任务数量
     */
    public MockContext execute(Dispatcher<TableTaskInfo> dispatcher) {
        log.info("Thread pool's initialization has been done. coreSize={},maxSize={}", CORE_POOL_SIZE, MAX_POOL_SIZE);
        MockContext context = new MockContext(this.service, dispatcher.taskId(), dispatcher.name(), dispatcher.totalCount());
        AbstractScheduler thisScheduler = this;
        int concurrentCount = dispatcher.count();
        //标识数组，数组长度和tasks的任务队列数量相同，每一位分别用于标示对应任务队列中是否还有任务等待执行
        boolean[] flags = new boolean[concurrentCount];
        for (int i = 0; i < concurrentCount; i++) {
            flags[i] = true;
        }
        Callable<Integer> scheduleTask = () -> {
            MDC.put("mocktask.workspace", context.getTaskId());
            int totalCount = 0;
            int total = 0;
            Long maxTimeout = 0L;
            Long timeoutSum = 0L;
            for (int i = 0; i < dispatcher.count(); i++) {
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
                            Map<Set<String>, Integer> dataGroups = scheduleDataTask(task.dataWriteGroups(), service.getActiveCount(),
                                    service.getCorePoolSize(), service.getMaximumPoolSize());
                            if (dataGroups == null) {
                                Thread.sleep(5000);
                                log.warn("Insufficient thread resources, will retry, schema={}, tableName={}",
                                        task.getMetaData().getTableSchema(), task.getMetaData().getTableName());
                                if ((failCount++) > maxFailCount) {
                                    log.warn("Task scheduling operation timed out, the scheduling thread will exit, timeout={} min",
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
                                    service.getActiveCount() + currentActive, service.getCorePoolSize(), service.getMaximumPoolSize());
                            if (columnGroups == null) {
                                Thread.sleep(5000);
                                log.warn("Insufficient thread resources, will retry, schema={}, tableName={}",
                                        task.getMetaData().getTableSchema(), task.getMetaData().getTableName());
                                if ((failCount++) > maxFailCount) {
                                    log.warn("Task scheduling operation timed out, the scheduling thread will exit, timeout={} min",
                                            maxTimeout / 60000 + 3);
                                    clearResource(dispatcher);
                                    return totalCount;
                                }
                                continue;
                            }
                            validateSet(columnGroups);
                            failCount = 0;
                            if (!validateThreadResource(columnGroups, dataGroups, service.getActiveCount(), service.getMaximumPoolSize())) {
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
                            //初始化TaskBean，主要是定义TaskBean的回调函数
                            TableTask mockTaskBean = new TableTask(task, columnGroups, dataGroups, dispatcher.name(), i);
                            mockTaskBean.getContext().setStatus(MockTaskStatus.PENDING);
                            mockTaskBean.init(service, new AbstractCallBack<TableTaskContext>() {
                                @Override
                                public void doOnSuccess(TableTaskContext param) {
                                    ((MockerDataSource) param.getDataSource()).clear();
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
                                    ((MockerDataSource) param.getDataSource()).clear();
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
                                ((MockerDataSource) task.getDataSource()).clear();
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
            log.info("Scheduled task execution completed, totalTaskExecuted={}, duration={}", totalCount, getDuration());
            MDC.clear();
            return totalCount;
        };
        this.service.submitCallable(scheduleTask);
        return context;
    }

    /**
     * 调度器线程退出前需要清理分发器对象内部没有执行完的任务的资源
     *
     * @param dispatcher 分发器对象
     * @throws Exception 释放资源可能发生异常
     */
    private void clearResource(Dispatcher<TableTaskInfo> dispatcher) throws Exception {
        for (int i = 0; i < dispatcher.count(); i++) {
            for (int j = 0; j < dispatcher.getTaskSize(i); j++) {
                TableTaskInfo bean = dispatcher.getObj(i, j);
                ((MockerDataSource) bean.getDataSource()).clear();
                for (MockerFile manager : bean.getFileManagers()) {
                    manager.close();
                }
            }
        }
    }

    /**
     * 验证线程资源是否足够，该方法不抛出异常，如果验证不通过则直接抛出异常
     *
     * @param columnGroups 列原语分组ID集合
     * @param dataGroups   数据生成原语分组ID集合
     * @param active       目前线程池中活跃的任务
     * @param max          线程池的最大线程数
     * @return 返回验证结果
     */
    private boolean validateThreadResource(Set<Set<String>> columnGroups, Map<Set<String>, Integer> dataGroups, int active, int max) {
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
     * 验证用户实现接口返回列原语分组集合是否合法，验证标准是各个分组ID集合之间不能有交集
     *
     * @param input 输入分组集合
     * @throws MockerException 若验证失败则抛出异常
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
     * 返回调度任务的历时
     *
     * @return 返回调度任务执行的时间
     */
    private long interval() {
        return System.currentTimeMillis() - startTimestamp;
    }

    /**
     * Get duration string value
     *
     * @return duration string value
     * */
    private String getDuration() {
        long minMillis = 60 * 1000L;
        long hourMills = minMillis * 60;
        long duration = interval();
        if (duration < minMillis) {
            return String.format("%.2f s", duration / 1000.0);
        } else if (duration < hourMills) {
            return String.format("%.2f min", duration / 1000.0 / 60);
        } else {
            return String.format("%.2f hrs", duration / 1000.0 / 60 / 60);
        }
    }

    /**
     * 列生成原语的线程调度抽象方法，通过该方法实现列原语的调度
     *
     * @param groups 列原语的分组ID集合
     * @param active 当前线程池的活跃任务数量
     * @param core   当前线程池的core size
     * @param max    当前线程池的最大大小
     * @return 返回group分组，每个分组分配一个线程资源
     */
    abstract protected Set<Set<String>> scheduleColumnTask(Set<String> groups, int active, int core, int max);

    /**
     * 数据写入原语的抽象调度方法，通过该方法调度数据原语
     *
     * @param groups 数据原语的分组ID
     * @param active 线程池的活跃任务数量
     * @param core   线程池的core size
     * @param max    线程池的最大容量
     * @return 返回每个分组集合所分配的线程数量
     */
    abstract protected Map<Set<String>, Integer> scheduleDataTask(Set<String> groups, int active, int core, int max);

    /**
     * 任务执行完成后调用的回调方法
     *
     * @param context mock任务的执行上下文
     */
    protected abstract void onSuccess(TableTaskContext context);

    /**
     * 任务执行完成后调用的回调方法，任务失败时
     *
     * @param context mock任务的执行上下文
     */
    protected abstract void onFailure(TableTaskContext context, Throwable e);

    /**
     * 获取线程池对象，如果想使用默认的就可以直接返回null
     *
     * @return 返回线程池对象
     */
    public abstract ThreadPoolExecutor pool();
}
