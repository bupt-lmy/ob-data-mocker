package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;
import java.util.Random;

import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 生成正态分布随机数的数据生成器
 *
 * @author yh263208
 * @date 2020-12-11 16:50
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NormalGenerator extends DigitalGeneratorBase<BigDecimal> {
    /**
     * 正态分布的标准差
     */
    private final double variance;
    /**
     * 正态分布的平均值
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
