package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;
import java.util.Random;

import com.oceanbase.tools.datamocker.generator.BaseDigitalGenerator;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * A data generator that generates normally distributed random numbers
 *
 * @author yh263208
 * @date 2020-12-11 16:50
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NormalGenerator extends BaseDigitalGenerator<BigDecimal> {
    /**
     * Standard deviation of normal distribution
     */
    private final double variance;
    /**
     * The mean of the normal distribution
     */
    private final double average;

    public NormalGenerator(double average, double variance) {
        this.variance = variance;
        this.average = average;
    }

    public NormalGenerator() {
        this.average = 0D;
        this.variance = 1D;
    }

    @Override
    public Boolean preCheck(BigDecimal minValue, BigDecimal maxValue) {
        BigDecimal avg = new BigDecimal(Double.toString(average));
        if (avg.compareTo(minValue) < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Avg value is illegal for min value");
        } else if (avg.compareTo(maxValue) >= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Avg value is illegal for max value");
        }
        return true;
    }

    @Override
    public BigDecimal generate(BigDecimal minValue, BigDecimal maxValue) {
        Random random = new Random();
        BigDecimal value = new BigDecimal(Double.toString(Math.sqrt(variance) * random.nextGaussian() + average));
        if (value.compareTo(minValue) < 0) {
            return minValue;
        } else if (value.compareTo(maxValue) > 0) {
            return maxValue;
        }
        return value;
    }

    @Override
    public Long count(BigDecimal minValue, BigDecimal maxValue) {
        return null;
    }
}
