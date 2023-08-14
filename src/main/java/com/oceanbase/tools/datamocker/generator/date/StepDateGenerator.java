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
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Sequential date data generator
 *
 * @author yh263208
 * @date 2020-12-16 16:44
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepDateGenerator extends BaseDateGenerator<Date> {
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
    public StepDateGenerator(long step, TimeUnit timeUnit, Boolean cycle) {
        this.cycle = cycle;
        if (timeUnit != null) {
            this.step = TimeUnit.MILLISECONDS.convert(step, timeUnit);
        } else {
            this.step = step;
        }
    }

    @Override
    protected Boolean doPreCheck(Date startTime, Date endTime, int scale, TimeUnit minTimeUnit) {
        return true;
    }

    @Override
    protected Date doGenerate(Date startTime, Date endTime, int scale, TimeUnit minTimeUnit) {
        if (step < 0) {
            return new Date(minus(startTime.getTime(), endTime.getTime()));
        }
        return new Date(positive(startTime.getTime(), endTime.getTime()));
    }

    @Override
    protected Long doCount(Date startTime, Date endTime, int scale, TimeUnit minTimeUnit) {
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
