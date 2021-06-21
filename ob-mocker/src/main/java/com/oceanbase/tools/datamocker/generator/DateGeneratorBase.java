package com.oceanbase.tools.datamocker.generator;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Date type data generator
 *
 * @author yh263208
 * @date 2020-12-16 16:27
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class DateGeneratorBase<T extends Comparable> extends BaseGenerator<T, T> {
    /**
     * The smallest precision unit of the data generator, the second of the date type is used by default
     */
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    /**
     * Date precision, mainly for the timestamp type
     */
    private int scale;

    public void setScale(int scale) {
        this.scale = scale;
    }

    protected int scale() {
        return this.scale;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    protected int getnano() {
        return new Random().nextInt(new Double(Math.pow(10, scale)).intValue());
    }

    public TimeUnit timeUnit() {
        return this.timeUnit;
    }

    /**
     * Pre-checking step, used to check whether the generator can work normally according to the boundary value
     *
     * @param startTime min timestamp for a date type
     * @param endTime   max timestamp for a date type
     * @return Return the verification result
     */
    @Override
    abstract public Boolean preCheck(T startTime, T endTime);

    /**
     * Get generated data
     *
     * @param startTime The left boundary value has slightly different meanings for different types of data generators.
     *                  For date-type data generation tasks, it indicates the start time
     * @param endTime The right boundary value has slightly different meanings for different types of data generators.
     *                For date-type data generation tasks, it indicates the end time
     * @return Returns a generated specific value
     */
    @Override
    abstract public T generate(T startTime, T endTime);

    /**
     * Return the total number of unique data that the data generator can generate
     *
     * @return Return a specific value, or null if the data generator can generate data without limitation
     */
    @Override
    abstract public Long count(T startTime, T endTime);
}
