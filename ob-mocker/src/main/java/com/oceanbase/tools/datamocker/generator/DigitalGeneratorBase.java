package com.oceanbase.tools.datamocker.generator;

/**
 * Random data generator interface, used to generate random data,
 * provides two interfaces for implementation
 *
 * @author yh263208
 * @date 2020-12-11 18:00
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class DigitalGeneratorBase<T extends Comparable> extends BaseGenerator<T, T> {
    /**
     * Pre-checking step, used to check whether the generator can work normally according to the boundary value
     *
     * @param minValue min value for number type
     * @param maxValue max value for number type
     * @return Return the verification result
     */
    @Override
    abstract public Boolean preCheck(T minValue, T maxValue);

    /**
     * 数据生成方法接口
     *
     * @param minValue The minimum value has slightly different meanings for different types of data generators.
     *                 For digital generation tasks, it reflects the minimum value of the generated numbers.
     * @param maxValue The maximum value has slightly different meanings for different types of data generators.
     *                 For digital generation tasks, it reflects the maximum value of the generated numbers.
     * @return Returns a generated specific value
     */
    @Override
    abstract public T generate(T minValue, T maxValue);

    /**
     * Return the total number of unique data that the data generator can generate
     *
     * @return Return a specific value, or null if the data generator can generate data without limitation
     */
    @Override
    abstract public Long count(T minValue, T maxValue);
}
