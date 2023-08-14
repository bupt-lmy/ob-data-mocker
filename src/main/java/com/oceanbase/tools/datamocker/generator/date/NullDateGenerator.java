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
package com.oceanbase.tools.datamocker.generator.date;

import java.sql.Date;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.BaseDateGenerator;

/**
 * Null data generator of date type
 *
 * @author yh263208
 * @date 2021-01-26 14:42
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NullDateGenerator extends BaseDateGenerator<Date> {
    @Override
    protected Boolean doPreCheck(Date startTime, Date endTime, int scale, TimeUnit timeUnit) {
        return true;
    }

    @Override
    protected Date doGenerate(Date startTime, Date endTime, int scale, TimeUnit timeUnit) {
        return null;
    }

    @Override
    protected Long doCount(Date startTime, Date endTime, int scale, TimeUnit timeUnit) {
        return 1L;
    }

}
