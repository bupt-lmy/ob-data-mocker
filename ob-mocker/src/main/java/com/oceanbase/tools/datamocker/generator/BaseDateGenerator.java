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
package com.oceanbase.tools.datamocker.generator;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import lombok.Setter;

/**
 * Date type data generator
 *
 * @author yh263208
 * @date 2020-12-16 16:27
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class BaseDateGenerator<T extends Comparable<? super T>> extends BaseGenerator<T, T> {
    /**
     * The smallest precision unit of the data generator, the second of the date type is used by default
     */
    @Setter
    private TimeUnit minTimeUnit = TimeUnit.SECONDS;
    /**
     * Date precision, mainly for the timestamp type
     */
    @Setter
    private int scale;

    /**
     * Pre-checking step, used to check whether the generator can work normally according to the
     * boundary value
     *
     * @param startTime min timestamp for a date type
     * @param endTime max timestamp for a date type
     * @param scale scale of type
     * @param minTimeUnit time unit of time
     * @return Return the verification result
     */
    abstract protected Boolean doPreCheck(T startTime, T endTime, int scale, TimeUnit minTimeUnit);

    @Override
    public Boolean preCheck(T startTime, T endTime) {
        return doPreCheck(startTime, endTime, scale, minTimeUnit);
    }

    /**
     * Get generated data
     *
     * @param startTime The left boundary value has slightly different meanings for different types of
     *        data generators. For date-type data generation tasks, it indicates the start time
     * @param endTime The right boundary value has slightly different meanings for different types of
     *        data generators. For date-type data generation tasks, it indicates the end time
     * @param scale scale of type
     * @param minTimeUnit time unit of time
     * @return Returns a generated specific value
     */
    abstract protected T doGenerate(T startTime, T endTime, int scale, TimeUnit minTimeUnit);

    @Override
    public T generate(T startTime, T endTime) {
        return doGenerate(startTime, endTime, scale, minTimeUnit);
    }

    /**
     * Return the total number of unique data that the data generator can generate
     *
     * @param startTime The left boundary value has slightly different meanings for different types of
     *        data generators. For date-type data generation tasks, it indicates the start time
     * @param endTime The right boundary value has slightly different meanings for different types of
     *        data generators. For date-type data generation tasks, it indicates the end time
     * @param scale scale of type
     * @param minTimeUnit time unit of time
     * @return Return a specific value, or null if the data generator can generate data without
     *         limitation
     */
    abstract protected Long doCount(T startTime, T endTime, int scale, TimeUnit minTimeUnit);

    @Override
    public Long count(T startTime, T endTime) {
        return doCount(startTime, endTime, scale, minTimeUnit);
    }

    /**
     * Get the nanosceond value
     *
     * @return nano seconds
     */
    protected int getNanoSeconds(int scale) {
        return new Random().nextInt(new Double(Math.pow(10, scale)).intValue());
    }

}
