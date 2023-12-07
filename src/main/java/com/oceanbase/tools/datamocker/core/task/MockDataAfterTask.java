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
package com.oceanbase.tools.datamocker.core.task;

import lombok.extern.slf4j.Slf4j;

/**
 * The finishing task after the execution of mock data business logic is mainly to count the data
 * volume of the current table
 *
 * @author yh263208
 * @date 2021-01-14 10:54
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class MockDataAfterTask extends AbstractMockTask {

    public MockDataAfterTask(TableTaskMetaData metaData, TableTaskContext context) {
        super(metaData, context);
    }

    @Override
    public void execute(TableTaskMetaData metaData, TableTaskContext context) {
        log.info("Mock after task is running...");
        log.info("Mock after task is succeed");
    }

}
