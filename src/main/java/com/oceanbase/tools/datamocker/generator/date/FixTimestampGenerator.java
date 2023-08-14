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

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.BaseDateGenerator;

/**
 * Data generator with fixed timestamp
 *
 * @author yh263208
 * @date 2020-12-16 19:19
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixTimestampGenerator extends BaseDateGenerator<Timestamp> {
    /**
     * Fixed date and time stamp
     */
    private final long timestamp;

    public FixTimestampGenerator(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    protected Boolean doPreCheck(Timestamp startTime, Timestamp endTime, int scale, TimeUnit minTimeUnit) {
        return this.timestamp >= startTime.getTime() && this.timestamp <= endTime.getTime();
    }

    @Override
    protected Timestamp doGenerate(Timestamp startTime, Timestamp endTime, int scale, TimeUnit minTimeUnit) {
        Timestamp timestamp = new Timestamp(this.timestamp);
        timestamp.setNanos(0);
        return timestamp;
    }

    @Override
    protected Long doCount(Timestamp startTime, Timestamp endTime, int scale, TimeUnit minTimeUnit) {
        return 1L;
    }

}
