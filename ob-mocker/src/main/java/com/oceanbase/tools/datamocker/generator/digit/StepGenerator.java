package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Step data generator, used to generate random data with specified step length
 *
 * @author yh263208
 * @date 2020-12-11 17:16
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepGenerator extends DigitalGeneratorBase<BigDecimal> {
    private final double step;
    /**
     * Number currently generated
     */
    private BigDecimal currentDigit = null;
    /**
     * Whether to generate in a loop
     */
    private final boolean round;

    public StepGenerator(double step, boolean round) {
        if (step == 0) {
            throw new MockerException("Step can not be zero");
        }
        this.step = step;
        this.round = round;
    }

    public StepGenerator(double step) {
        if (step == 0) {
            throw new MockerException("Step can not be zero");
        }
        this.step = step;
        this.round = false;
    }

    @Override
    public BigDecimal generate(BigDecimal minValue, BigDecimal maxValue) {
        if (step < 0) {
            return minusStep(minValue, maxValue);
        }
        return positive(minValue, maxValue);
    }

    /**
     * Random number generation logic when the step size is negative
     *
     * @param minValue Minimum value generated
     * @param maxValue Maximum value generated
     * @return Returns the generated random value
     */
    private BigDecimal minusStep(BigDecimal minValue, BigDecimal maxValue) {
        if (currentDigit == null) {
            currentDigit = maxValue;
            return currentDigit;
        }
        currentDigit = currentDigit.add(new BigDecimal(Double.toString(step)));
        if (currentDigit.compareTo(minValue) < 0) {
            if (round) {
                currentDigit = maxValue;
            } else {
                throw new MockerException("Can not generate more unique number");
            }
        }
        return currentDigit;
    }

    /**
     * Random number generation logic when the step size is positive
     *
     * @param minValue Minimum value generated
     * @param maxValue Maximum value generated
     * @return Returns the generated random value
     */
    private BigDecimal positive(BigDecimal minValue, BigDecimal maxValue) {
        if (currentDigit == null) {
            currentDigit = minValue;
            return currentDigit;
        }
        currentDigit = currentDigit.add(new BigDecimal(Double.toString(step)));
        if (currentDigit.compareTo(maxValue) > 0) {
            if (round) {
                currentDigit = minValue;
            } else {
                throw new MockerException("Can not generate more unique number");
            }
        }
        return currentDigit;
    }

    @Override
    public Long count(BigDecimal minValue, BigDecimal maxValue) {
        BigDecimal interval = maxValue.subtract(minValue);
        String longValue = interval.divide(
                new BigDecimal(Double.toString(step)).abs(), BigDecimal.ROUND_DOWN)
                .setScale(0, BigDecimal.ROUND_DOWN).toPlainString();
        if (new BigDecimal(longValue).compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) {
            return Long.MAX_VALUE;
        }
        return Long.valueOf(longValue);
    }

    @Override
    public Boolean preCheck(BigDecimal minValue, BigDecimal maxValue) {
        return true;
    }
}
