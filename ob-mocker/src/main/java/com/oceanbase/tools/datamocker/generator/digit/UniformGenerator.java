package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;

/**
 * 服从均匀分布的数据生成器
 *
 * @author yh263208
 * @date 2020-12-10 17:58
 * @since OB_MOCK_snapshot_0.1.0
 */
public class UniformGenerator extends DigitalGeneratorBase<BigDecimal> {
    /**
     * 乘数因子
     */
    private volatile BigDecimal factor = null;
    /**
     * 乘数因子的写锁
     */
    private final ReentrantLock factorWriteLock = new ReentrantLock();

    @Override
    public BigDecimal generate(BigDecimal min, BigDecimal max) {
        BigDecimal currentFactor = getFactor(min, max);
        return new BigDecimal(Double.toString(Math.random())).multiply(currentFactor).add(min);
    }

    /**
     * 获取乘数因子，设计上乘数因子在一个生成对象中是一个固定的值因此设计了这个获取方法来提高获取效率
     * 在一个实例内部，乘数因子是一个固定的值
     *
     * @param maxValue 产生随机数的最大值
     * @param minValue 产生随机数的最小值
     * @return 返回乘数因子
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
