package com.oceanbase.tools.datamocker.generator;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 基类数据生成对象接口，用于定义数据生成方式
 *
 * @author yh263208
 * @date 2020-12-11 21:16
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class BaseGenerator<T extends Comparable, V> {
    /**
     * 默认值
     */
    private V defaultValue;
    /**
     * 是否允许空值
     */
    private Boolean allowNull = Boolean.FALSE;

    /**
     * 设置默认值
     *
     * @param defaultValue 默认值
     */
    public void setDefaultValue(V defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * 设置是否允许为空
     *
     * @param allowNull 是否为空
     */
    public void setAllowNull(Boolean allowNull) {
        this.allowNull = allowNull;
    }

    /**
     * 返回是否允许空值
     */
    protected Boolean allowNull() {
        return this.allowNull;
    }

    /**
     * 获取默认值
     *
     * @return 返回默认值
     */
    protected V defaultValue() {
        return this.defaultValue;
    }

    /**
     * 预检查步骤，用于根据边界值校验该生成器是否可以正常工作
     *
     * @param leftLimit  左边界值
     * @param rightLimit 右边界值
     * @return 返回校验结果
     */
    public abstract Boolean preCheck(T leftLimit, T rightLimit);

    /**
     * 获取生成数据
     *
     * @param leftLimit  左边界值，对于不同类型的数据生成器含义略有不同，对于数字型的生成任务反映的是生成数字的最小值，
     *                   如果是字符型的生成任务反映的是字符的字节最小值
     * @param rightLimit 右边界值，对于不同类型的数据生成器含义略有不同，对于数字型的生成任务反映的是生成数字的最小值，
     *                   如果是字符型的生成任务反映的是字符的字节最小值
     * @return 返回一个生成的具体值
     */
    abstract protected V generate(T leftLimit, T rightLimit);

    /**
     * 获取生成数据方法
     *
     * @param leftLimit  左边界值，对于不同类型的数据生成器含义略有不同，对于数字型的生成任务反映的是生成数字的最小值，
     *                   如果是字符型的生成任务反映的是字符的字节最小值
     * @param rightLimit 右边界值，对于不同类型的数据生成器含义略有不同，对于数字型的生成任务反映的是生成数字的最小值，
     *                   如果是字符型的生成任务反映的是字符的字节最小值
     * @return 返回一个生成的具体值
     */
    public V next(T leftLimit, T rightLimit) {
        int loopCount = 0;
        while (true) {
            V returnVal = generate(leftLimit, rightLimit);
            if (returnVal == null) {
                if (allowNull()) {
                    return null;
                } else if (defaultValue() != null) {
                    return defaultValue();
                }
                if ((loopCount++) >= 100) {
                    throw new MockerException(MockerError.OPERATION_FAILURE, "data generator get too much null data");
                }
            } else {
                return returnVal;
            }
        }
    }

    /**
     * 返回数据生成器一共能够生成的不重复的数据个数
     *
     * @param leftLimit  左边界值
     * @param rightLimit 右边界值
     * @return 返回具体的数值，如果数据生成器可以无限制生成数据则返回null
     */
    abstract public Long count(T leftLimit, T rightLimit);
}
