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
package com.oceanbase.tools.datamocker;

import com.oceanbase.tools.datamocker.core.Dispatcher;
import com.oceanbase.tools.datamocker.core.task.TableTaskInfo;
import com.oceanbase.tools.datamocker.schedule.AbstractScheduler;
import com.oceanbase.tools.datamocker.schedule.MockContext;
import lombok.NonNull;

/**
 * Simulate the data object, use the object for actual data generation
 *
 * @author yh263208
 * @date 2021-02-03 21:02
 * @since OBMOCKER_snapshot_0.1.0
 */
public class ObDataMocker {

    private final AbstractScheduler scheduler;
    private final Dispatcher<TableTaskInfo> dispatcher;

    public ObDataMocker(@NonNull Dispatcher<TableTaskInfo> dispatcher, @NonNull AbstractScheduler scheduler) {
        this.scheduler = scheduler;
        this.dispatcher = dispatcher;
    }

    public MockContext start() {
        return scheduler.execute(dispatcher);
    }

    public int size() {
        if (this.dispatcher == null) {
            return -1;
        }
        return this.dispatcher.getWidth();
    }

    public int size(int index) {
        if (index < 0 || this.dispatcher == null) {
            return -1;
        }
        return this.dispatcher.getTaskSize(index);
    }

}
