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
import java.util.LinkedList;
import java.util.List;

import com.oceanbase.tools.datamocker.core.Dispatcher;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import lombok.Getter;
import lombok.NonNull;

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
    private final String logDir;
    @Getter
    private final List<TableTaskContext> tables;
    private final MockExecutorService service;

    public MockContext(@NonNull MockExecutorService service, @NonNull Dispatcher<?> dispatcher) {
        this.logDir = dispatcher.getLogDir();
        this.tables = new LinkedList<>();
        this.service = service;
        this.totalTableTaskCount = dispatcher.getTotalCount();
    }

    protected void appendContext(TableTaskContext context) {
        if (context == null) {
            return;
        }
        synchronized (this.tables) {
            this.tables.add(context);
        }
    }

    public Boolean shutdown() {
        this.service.shutdown();
        boolean returnVal = true;
        for (TableTaskContext context : this.tables) {
            returnVal &= context.shutdown();
        }
        return returnVal;
    }

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
