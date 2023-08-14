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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.Getter;

/**
 * Context object for mock task
 *
 * @author yh263208
 * @date 2021-06-21 13:39
 * @since OB_MOCKER_snapshot_0.1.0
 */
public class MockContext {
    private final Integer totalTableTaskCount;
    @Getter
    private final String taskName;
    @Getter
    private final String taskId;
    @Getter
    private final List<TableTaskContext> tables;
    private final MockExecutorService service;

    public MockContext(MockExecutorService service, String taskId, String taskName, Integer totalTableTaskCount) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.totalTableTaskCount = totalTableTaskCount;
        tables = new LinkedList<>();
        if (service == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Thread pool for schedule context can not be null");
        }
        this.service = service;
    }

    /**
     * Append a mock data context object
     *
     * @param context Context object
     */
    protected void appendContext(TableTaskContext context) {
        if (context == null) {
            return;
        }
        synchronized (this.tables) {
            this.tables.add(context);
        }
    }

    /**
     * Delete a context object
     *
     * @param taskId Pass in a subtask id
     */
    protected List<TableTaskContext> removeContext(String taskId) {
        if (taskId == null) {
            return Collections.emptyList();
        }
        List<TableTaskContext> returnVal = new LinkedList<>();
        synchronized (this.tables) {
            int length = this.tables.size();
            for (int i = 0; i < length; i++) {
                if (taskId.equals(this.tables.get(i).getTableTaskId())) {
                    returnVal.add(this.tables.remove(i));
                    length--;
                }
            }
        }
        return returnVal;
    }

    /**
     * Close the mock data scheduler object
     *
     * @return Return close result
     */
    public Boolean shutdown() {
        this.service.shutdown();
        boolean returnVal = true;
        for (TableTaskContext context : this.tables) {
            returnVal &= context.shutdown();
        }
        return returnVal;
    }

    /**
     * get task progress(%)
     *
     * @return progress
     */
    public double getProgress() {
        if (this.tables.size() != 0) {
            double returnVal = 0.0;
            for (TableTaskContext context : this.tables) {
                returnVal += context.getProgress();
            }
            BigDecimal decaimal = new BigDecimal((returnVal * 100.0) / totalTableTaskCount);
            return decaimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        return 0.0;
    }
}
