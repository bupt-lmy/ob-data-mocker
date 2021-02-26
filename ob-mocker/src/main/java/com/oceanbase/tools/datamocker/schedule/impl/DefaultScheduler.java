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
 * 默认调度器，调度逻辑是不同组的列原语分配一个线程资源。数据写入原语不同组相互组合，每个组合分配两个线程资源
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
     * 平均分配线程资源，各个列分组分配相同数量的线程资源
     */
    @Override
    protected Set<Set<String>> scheduleColumnTask(Set<String> groups, int active, int core, int max) {
        int allocate = (int) ((max - active) * 0.7) - 1;
        if (allocate <= 0) {
            log.error("fail to schedule task, free thread resource is {}, which is equal to or smaller than zero", allocate);
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
     * 平均分配线程资源到各个数据写出原语
     */
    @Override
    protected Map<Set<String>, Integer> scheduleDataTask(Set<String> groups, int active, int core, int max) {
        int allocate = (int) ((max - active) * 0.7) - 1;
        if (allocate <= 0) {
            log.error("fail to schedule task, free thread resource is {}, which is equal to or smaller than zero", allocate);
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
    protected void callBack(TableTaskContext context) {
    }

    @Override
    public ThreadPoolExecutor pool() {
        return null;
    }
}
