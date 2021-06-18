package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 步长数据生成器，用于生成指定步长的随机数据
 *
 * @author yh263208
 * @date 2020-12-11 17:16
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepGenerator extends DigitalGeneratorBase<BigDecimal> {
    /**
     * 步长
     */
    private final double step;
    /**
     * 当前生成的数
     */
    private BigDecimal currentDigit = null;
    /**
     * s是否循环生成
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
     * 步长为负数时的随机数生成逻辑
     *
     * @param minValue 生成的最小值
     * @param maxValue 生成的最大值
     * @return 返回生成的随机数值
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
     * 步长为正数时的随机数生成逻辑
     *
     * @param minValue 生成的最小值
     * @param maxValue 生成的最大值
     * @return 返回生成的随机数值
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
