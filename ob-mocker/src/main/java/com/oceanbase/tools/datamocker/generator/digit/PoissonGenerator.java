package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 泊松分布随机数生成器
 *
 * @author yh263208
 * @date 2020-12-09 13:51
 * @since ODCMOCKER_snapshot_0.1.0
 */
public class PoissonGenerator extends DigitalGeneratorBase<BigDecimal> {
    /**
     * 泊松分布的均值
     */
    private final double lambda;

    /**
     * 构造函数
     *
     * @param lambda 传入一个平均值
     */
    public PoissonGenerator(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public Boolean preCheck(BigDecimal minValue, BigDecimal maxValue) {
        BigDecimal lambdaValue = new BigDecimal(Double.toString(lambda));
        if (lambdaValue.compareTo(minValue) < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "lambda is smaller than min value");
        } else if (lambdaValue.compareTo(maxValue) >= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "lambda is bigger than max value");
        } else if (minValue.compareTo(BigDecimal.ZERO) != 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "lmin value is not equal to zero");
        }
        return true;
    }

    @Override
    public BigDecimal generate(BigDecimal minValue, BigDecimal maxValue) {
        BigDecimal i = minValue;
        BigDecimal a = new BigDecimal(Double.toString(Math.exp(-lambda)));
        BigDecimal b = new BigDecimal(Double.toString(1.0));
        do {
            b = b.multiply(new BigDecimal(Double.toString(Math.random())));
            i = i.add(BigDecimal.ONE);
        } while (b.compareTo(a) >= 0 && i.compareTo(maxValue) <= 0);
        return i.subtract(BigDecimal.ONE);
    }

    @Override
    public Long count(BigDecimal minValue, BigDecimal maxValue) {
        return null;
    }

}
