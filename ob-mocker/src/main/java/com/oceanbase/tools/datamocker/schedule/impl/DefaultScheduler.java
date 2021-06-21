package com.oceanbase.tools.datamocker.schedule.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.schedule.AbstractScheduler;
import lombok.extern.slf4j.Slf4j;

/**
 * The default scheduler, the scheduling logic is that different groups of column primitives allocate a thread resource.
 * Different groups of data write primitives are combined with each other, and each combination is allocated two thread resources
 *
 * @author yh263208
 * @date 2021-01-18 22:14
 * @since OBMOCKER_0.1.0_snapshot
 */
@Slf4j
public class DefaultScheduler extends AbstractScheduler {

    private final int maxConnectionSize;

    public DefaultScheduler(int maxConnectionSize) {
        this.maxConnectionSize = maxConnectionSize;
    }

    /**
     * Thread resources are allocated equally, and the same number of thread resources are allocated to each column grouping
     */
    @Override
    protected Set<Set<String>> scheduleColumnTask(Set<String> groups, int active, int core, int max) {
        int allocate = (int) ((max - active) * 0.7) - 1;
        if (allocate <= 0) {
            log.error("The scheduling task failed because the idle thread resources are less than or equal to zero, freeThreadCount={}",
                    allocate);
            return null;
        }
        int size = groups.size();
        Set<Set<String>> returnVal = new HashSet<>();
        if (allocate > size) {
            for (String item : groups) {
                Set<String> itemSet = new HashSet<>();
                itemSet.add(item);
                returnVal.add(itemSet);
            }
        } else {
            List<String> array = new ArrayList<>(groups);
            int quotient = size / allocate;
            for (int i = 0; i < allocate; i++) {
                Set<String> middleSet = new HashSet<>();
                for (int j = 0; j < quotient + 1; j++) {
                    int actualIndex = j * allocate + i;
                    if (actualIndex >= size) {
                        break;
                    } else {
                        middleSet.add(array.get(actualIndex));
                    }
                }
                returnVal.add(middleSet);
            }
        }
        return returnVal;
    }

    /**
     * Equally allocate thread resources to each data to write primitives
     */
    @Override
    protected Map<Set<String>, Integer> scheduleDataTask(Set<String> groups, int active, int core, int max) {
        int allocate = (int) ((max - active) * 0.7) - 1;
        if (allocate <= 0) {
            log.error("The scheduling task failed because the idle thread resources are less than or equal to zero, freeThreadCount={}",
                    allocate);
            return null;
        }
        if (allocate > this.maxConnectionSize - 2) {
            allocate = this.maxConnectionSize - 2;
        }
        Map<Set<String>, Integer> returnVal = new HashMap<>();
        returnVal.put(groups, allocate);
        return returnVal;
    }

    @Override
    protected void onSuccess(TableTaskContext context) {

    }

    @Override
    protected void onFailure(TableTaskContext context, Throwable e) {

    }

    @Override
    public ThreadPoolExecutor pool() {
        return null;
    }
}
