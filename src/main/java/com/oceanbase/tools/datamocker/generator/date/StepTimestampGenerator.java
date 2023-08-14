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
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Sequential Timestamp Data Generator
 *
 * @author yh263208
 * @date 2020-12-16 16:44
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepTimestampGenerator extends BaseDateGenerator<Timestamp> {
    /**
     * Date step
     */
    private final long step;
    /**
     * Whether to loop
     */
    private final Boolean cycle;
    /**
     * Number currently generated
     */
    private Long timestamp = null;

    /**
     * Constructor
     *
     * @param timeUnit time unit
     * @param cycle Whether to rotate
     * @param step Time Step
     */
    public StepTimestampGenerator(long step, TimeUnit timeUnit, Boolean cycle) {
        this.cycle = cycle;
        if (timeUnit != null) {
            this.step = TimeUnit.MILLISECONDS.convert(step, timeUnit);
        } else {
            this.step = step;
        }
    }

    @Override
    protected Boolean doPreCheck(Timestamp startTime, Timestamp endTime, int scale, TimeUnit minTimeUnit) {
        return true;
    }

    @Override
    protected Timestamp doGenerate(Timestamp startTime, Timestamp endTime, int scale, TimeUnit minTimeUnit) {
        Timestamp timestamp;
        if (step < 0) {
            timestamp = new Timestamp(minus(startTime.getTime(), endTime.getTime()));
        } else {
            timestamp = new Timestamp(positive(startTime.getTime(), endTime.getTime()));
        }
        timestamp.setNanos(0);
        return timestamp;
    }

    @Override
    protected Long doCount(Timestamp startTime, Timestamp endTime, int scale, TimeUnit minTimeUnit) {
        long interval = endTime.getTime() - startTime.getTime();
        return interval / Math.abs(step);
    }

    /**
     * Random number generation logic when the step size is negative
     *
     * @return Returns the generated random date
     */
    private long minus(long startTime, long endTime) {
        if (timestamp == null) {
            timestamp = endTime;
            return timestamp;
        }
        timestamp += step;
        if (timestamp < startTime) {
            if (cycle) {
                timestamp = endTime;
            } else {
                throw new MockerException(MockerError.OPERATION_FAILURE, "Can not generate more unique date");
            }
        }
        return timestamp;
    }

    /**
     * Random number generation logic when the step size is positive
     *
     * @return Returns the generated random date
     */
    private long positive(long startTime, long endTime) {
        if (timestamp == null) {
            timestamp = startTime;
            return timestamp;
        }
        timestamp += step;
        if (timestamp > endTime) {
            if (cycle) {
                timestamp = startTime;
            } else {
                throw new MockerException(MockerError.OPERATION_FAILURE, "Can not generate more unique date");
            }
        }
        return timestamp;
    }

}
