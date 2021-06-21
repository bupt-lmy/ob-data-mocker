package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;

/**
 * Obey a uniformly distributed data generator
 *
 * @author yh263208
 * @date 2020-12-10 17:58
 * @since OB_MOCK_snapshot_0.1.0
 */
public class UniformGenerator extends DigitalGeneratorBase<BigDecimal> {
    /**
     * Multiplier factor
     */
    private volatile BigDecimal factor = null;
    /**
     * Multiplier factor write lock
     */
    private final ReentrantLock factorWriteLock = new ReentrantLock();

    @Override
    public BigDecimal generate(BigDecimal min, BigDecimal max) {
        BigDecimal currentFactor = getFactor(min, max);
        return new BigDecimal(Double.toString(Math.random())).multiply(currentFactor).add(min);
    }

    /**
     * Get the multiplier factor. In design, the multiplier factor is a fixed value in a generated
     * object. Therefore, this acquisition method is designed to improve the acquisition efficiency. In
     * an instance, the multiplier factor is a fixed value.
     *
     * @param maxValue Generate the maximum value of a random number
     * @param minValue Generate the minimum value of a random number
     * @return Returns the multiplier factor
     */
    private BigDecimal getFactor(BigDecimal minValue, BigDecimal maxValue) {
        if (factor == null) {
            factorWriteLock.lock();
            try {
                if (factor == null) {
                    factor = maxValue.subtract(minValue);
                }
                return factor;
            } finally {
                factorWriteLock.unlock();
            }
        }
        return factor;
    }

    @Override
    public Long count(BigDecimal minValue, BigDecimal maxValue) {
        return null;
    }

    @Override
    public Boolean preCheck(BigDecimal minValue, BigDecimal maxValue) {
        return true;
    }

}
