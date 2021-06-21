package com.oceanbase.tools.datamocker;

import com.oceanbase.tools.datamocker.core.Dispatcher;
import com.oceanbase.tools.datamocker.core.task.TableTaskInfo;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.schedule.AbstractScheduler;
import com.oceanbase.tools.datamocker.schedule.MockContext;

/**
 * Simulate the data object, use the object for actual data generation
 *
 * @author yh263208
 * @date 2021-02-03 21:02
 * @since OBMOCKER_snapshot_0.1.0
 */
public class ObDataMocker {
    /**
     * Mock data task object, used to encapsulate the topological relationship between multi-table tasks
     */
    private final Dispatcher<TableTaskInfo> dispatcher;
    /**
     * Scheduler, used to generate task scheduling thread resources for each table
     */
    private final AbstractScheduler scheduler;

    /**
     * The constructor of the protected type cannot be manually created by the user through the new
     * method
     *
     * @param dispatcher Dispatcher object that encapsulates the topological relationship of tasks
     * @param scheduler Scheduler object for scheduling tasks
     */
    public ObDataMocker(Dispatcher<TableTaskInfo> dispatcher, AbstractScheduler scheduler) {
        this.dispatcher = dispatcher;
        this.scheduler = scheduler;
    }

    /**
     * Start a simulated data task
     *
     * @return Return to the simulation data context
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
     * Get the concurrency number of the simulation data multi-table task
     *
     * @return Return the number of concurrent
     */
    public int size() {
        if (this.dispatcher == null) {
            return -1;
        }
        return this.dispatcher.count();
    }

    /**
     * Get the length of a task on a task queue
     *
     * @param index index
     * @return Return length
     * @throws Exception May be an illegal index value
     */
    public int size(int index) throws Exception {
        if (index < 0 || this.dispatcher == null) {
            return -1;
        }
        return this.dispatcher.getTaskSize(index);
    }
}
