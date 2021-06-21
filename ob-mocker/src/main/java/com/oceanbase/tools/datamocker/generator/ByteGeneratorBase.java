package com.oceanbase.tools.datamocker.generator;

/**
 * Byte array object type data generator
 *
 * @author yh263208
 * @date 2020-12-16 22:39
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class ByteGeneratorBase extends BaseGenerator<Integer, byte[]> {
    /**
     * Pre-checking step, used to check whether the generator can work normally according to the boundary value
     *
     * @param minLength Minimum
     * @param maxLength Max length for string value
     * @return Return the verification result
     */
    @Override
    abstract public Boolean preCheck(Integer minLength, Integer maxLength);

    /**
     * Data generation method interface
     *
     * @param minLength The minimum value, the character generation task reflects the minimum byte value of the character
     * @param maxLength The maximum value, the character generation task reflects the minimum byte value of the character
     * @return Returns a generated specific value
     */
    @Override
    abstract public byte[] generate(Integer minLength, Integer maxLength);

    /**
     * Return the total number of unique data that the data generator can generate
     *
     * @return Return a specific value, or null if the data generator can generate data without limitation
     */
    @Override
    abstract public Long count(Integer minLength, Integer maxLength);
}
