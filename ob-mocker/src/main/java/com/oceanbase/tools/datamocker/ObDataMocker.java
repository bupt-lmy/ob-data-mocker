package com.oceanbase.tools.datamocker;

import com.oceanbase.tools.datamocker.core.Dispatcher;
import com.oceanbase.tools.datamocker.core.task.TableTaskInfo;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.schedule.AbstractScheduler;
import com.oceanbase.tools.datamocker.schedule.MockContext;

/**
 * 模拟数据对象，使用该对象进行进行实际的数据生成
 *
 * @author yh263208
 * @date 2021-02-03 21:02
 * @since OBMOCKER_snapshot_0.1.0
 */
public class ObDataMocker {
    /**
     * mock数据任务对象，用于封装多表任务之间的拓扑关系
     */
    private final Dispatcher<TableTaskInfo> dispatcher;
    /**
     * 调度器，用于向各个表生成任务调度线程资源
     */
    private final AbstractScheduler scheduler;

    /**
     * 保护类型的构造函数，不能让用户通过new的方式手动创建
     *
     * @param dispatcher 封装任务拓扑关系的分发器对象
     * @param scheduler  用于调度任务的调度器对象
     */
    public ObDataMocker(Dispatcher<TableTaskInfo> dispatcher, AbstractScheduler scheduler) {
        this.dispatcher = dispatcher;
        this.scheduler = scheduler;
    }

    /**
     * 开启一个模拟数据任务
     *
     * @return 返回模拟数据上下文
     */
    public MockContext start() {
        if (this.scheduler == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Scheduler for ob mocker can not be null");
        }
        if (this.dispatcher == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Mock task dispatcher can not be null");
        }
        return scheduler.execute(dispatcher);
    }

    /**
     * 获取模拟数据多表任务的并发数
     *
     * @return 返回并发数
     */
    public int size() {
        if (this.dispatcher == null) {
            return -1;
        }
        return this.dispatcher.count();
    }

    /**
     * 获取某个任务队列上任务的长度
     *
     * @param index 索引
     * @return 返回长度
     * @throws Exception 可能是一个非法的索引值
     */
    public int size(int index) throws Exception {
        if (index < 0 || this.dispatcher == null) {
            return -1;
        }
        return this.dispatcher.getTaskSize(index);
    }
}
