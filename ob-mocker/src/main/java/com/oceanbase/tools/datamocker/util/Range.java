package com.oceanbase.tools.datamocker.util;

import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 表示一个数字区间，是一个左闭右闭区间
 *
 * @author yh263208
 * @date 2020-12-11 20：55
 * @since OBMOCKER_snapshot_0.1.0
 */
public class Range<T extends Comparable> {
    /**
     * 区间的左范围
     */
    private final T min;
    /**
     * 区间的右范围
     */
    private final T max;

    public Range(T min, T max) {
        if (min.compareTo(max) > 0) {
            throw new MockerException("min value can not be bigger than max value");
        }
        this.min = min;
        this.max = max;
    }

    /**
     * 用于判断一个值是否在区间内，这是一个左闭右闭区间
     *
     * @param value 用于判断的值
     * @return 返回布尔型结果
     */
    public boolean contain(T value) {
        return min.compareTo(value) <= 0 && max.compareTo(value) >= 0;
    }

    /**
     * 获取左边界
     *
     * @return 返回左边界值
     */
    public T getMin() {
        return this.min;
    }

    /**
     * 获取右边界值
     *
     * @return 返回右边界值
     */
    public T getMax() {
        return this.max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Range that = (Range) o;
        return this.min.compareTo(that.min) == 0 && this.max.compareTo(that.max) == 0;
    }

    @Override
    public int hashCode() {
        String buffer = this.min.hashCode() + this.max.hashCode() + "";
        return buffer.hashCode();
    }
}