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

import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import lombok.extern.slf4j.Slf4j;

/**
 * The default scheduler, the scheduling logic is that different groups of column primitives
 * allocate a thread resource. Different groups of data write primitives are combined with each
 * other, and each combination is allocated two thread resources
 *
 * @author yh263208
 * @date 2021-01-18 22:14
 * @since OBMOCKER_0.1.0_snapshot
 */
@Slf4j
public class DefaultScheduler extends AbstractScheduler {

    @Override
    protected Set<Set<String>> scheduleColumnTask(Set<String> groups, int active, int core, int max) {
        int allocate = (int) ((max - active) * 0.7) - 1;
        if (allocate <= 0) {
            log.warn("The idle thread resources are less than or equal to zero, freeThreadCount={}", allocate);
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

    @Override
    protected void onSuccess(TableTaskContext context) {

    }

    @Override
    protected void onFailure(TableTaskContext context, Throwable e) {

    }

}
