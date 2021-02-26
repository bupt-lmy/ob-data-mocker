package com.oceanbase.tools.datamocker.generator;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 日期类型的数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 16:27
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class DateGeneratorBase<T extends Comparable> extends BaseGenerator<T, T> {
    /**
     * 数据生成器的最小精度单位，默认使用date类型的秒
     */
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    /**
     * 日期精度，主要针对于时间戳类型
     */
    private int scale;

    /**
     * 设置精度
     *
     * @param scale 精度
     */
    public void setScale(int scale) {
        this.scale = scale;
    }

    /**
     * 获取精度
     *
     * @return 返回精度
     */
    protected int scale() {
        return this.scale;
    }

    /**
     * 设置日期精度
     *
     * @param timeUnit 精度单位
     */
    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    /**
     * 获取纳秒值
     *
     * @return 返回纳秒值
     */
    protected int getnano() {
        return new Random().nextInt(new Double(Math.pow(10, scale)).intValue());
    }

    /**
     * 获取日期精度
     *
     * @return 返回日期精度
     */
    public TimeUnit timeUnit() {
        return this.timeUnit;
    }

    /**
     * 预检查步骤，用于根据边界值校验该生成器是否可以正常工作
     *
     * @param startTime\ 最小值，
     * @param endTime    最大值
     * @return 返回校验结果
     */
    @Override
    abstract public Boolean preCheck(T startTime, T endTime);

    /**
     * 数据生成方法接口
     *
     * @param startTime 最小值，对于不同类型的数据生成器含义略有不同，对于数字型的生成任务反映的是生成数字的最小值，
     *                  如果是字符型的生成任务反映的是字符的字节最小值
     * @param endTime   最大值，对于不同类型的数据生成器含义略有不同，对于数字型的生成任务反映的是生成数字的最小值，
     *                  如果是字符型的生成任务反映的是字符的字节最小值
     * @return 返回一个生成的具体值
     */
    @Override
    abstract public T generate(T startTime, T endTime);

    /**
     * 返回数据生成器一共能够生成的不重复的数据个数
     *
     * @return 返回具体的数值，如果数据生成器可以无限制生成数据则返回null
     */
    @Override
    abstract public Long count(T startTime, T endTime);
}
