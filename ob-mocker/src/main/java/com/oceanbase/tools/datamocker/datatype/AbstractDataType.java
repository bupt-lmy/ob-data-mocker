package com.oceanbase.tools.datamocker.datatype;

import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 抽象数据类型类，用于封装一些基础的数据类型逻辑
 *
 * @author yh263208
 * @date 2020-12-10 15:42
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class AbstractDataType<T, V extends Comparable> {
    /**
     * 默认值
     */
    private final T defaultValue;
    /**
     * 是否允许空值
     */
    private Boolean allowNull;
    /**
     * 预检查结果缓存
     */
    private Boolean preCheck = null;
    /**
     * 该数据类型在数据库中的对应类型所能达到的最小值，该值可以人为指定，若不指定则为该数据类型对应能表示的最小值
     */
    protected V lowValue = null;
    /**
     * 该数据类型在数据库中的对应类型所能达到的最大值，该值可以人为指定，若不指定则为该数据类型对应能表示的最大值
     */
    protected V highValue = null;
    /**
     * 默认绑定一个数据生成器
     */
    protected BaseGenerator<V, T> generator;
    /**
     * 该数据类型对应的OB模式
     */
    private final ObModeType dialectType;

    /**
     * 抽象基类的构造函数，在这里需要传入这个数据类型绑定的随机数据生成器，并且指明该数据类型对应的OB模式以及该模式下的数据库类型
     *
     * @param dialectType OB模式
     */
    protected AbstractDataType(ObModeType dialectType, T defaultValue, Boolean allowNull) {
        this.dialectType = dialectType;
        this.allowNull = allowNull;
        this.defaultValue = defaultValue;
    }

    /**
     * 抽象基类的构造函数，在这里需要传入这个数据类型绑定的随机数据生成器，并且指明该数据类型对应的OB模式以及该模式下的数据库类型
     *
     * @param dialectType OB模式
     */
    protected AbstractDataType(BaseGenerator<V, T> generator, ObModeType dialectType, T defaultValue, Boolean allowNull) {
        this.dialectType = dialectType;
        this.allowNull = allowNull;
        this.defaultValue = defaultValue;
        this.bind(generator);
    }

    /**
     * 获取类型的工厂实例
     *
     * @return 返回工厂实例
     */
    abstract public DataTypeFactory getFactory();

    /**
     * 该类型在数据库中对应类型所能表示的数值上限
     *
     * @return 返回最大值
     */
    abstract protected V minValueForType();

    /**
     * 该类型在数据库中对应类型所能表示的数值下限
     *
     * @return 返回最小值
     */
    abstract protected V maxValueForType();

    /**
     * 该类型在指定数据生成器约束下最多能生成的不重复数据量，这个值由两个指标决定，第一个指标是数据生成器本身能生成的不重复数据量
     * 另一个指标就是该数据类型在精度约束下能产生的最多的不重复数据量，二者取小的
     *
     * @return 返回具体的数值
     */
    abstract public Long distinctLimit();

    /**
     * 数据生成器生成值预处理方法
     *
     * @param value 传入用于预处理的值
     * @return 返回处理后的值
     */
    abstract protected T preTreat(T value);

    /**
     * 转字符串方法，用于将一个泛型类型转化为一个字符串类型
     *
     * @param value 泛型类型
     * @return 返回转化的字符串类型
     */
    abstract public String toString(T value);

    /**
     * 生成数据摘要，用于将一个很大的数据转化为一个数据摘要减少数据存储成本
     *
     * @param value 数据的内容
     * @return 返回摘要
     */
    abstract public T toDigest(T value);

    /**
     * 类型转换方法，用于数据兼容，将一个类型的数据转换成数据类型默认的对应类型
     *
     * @param value 输入值
     * @return 转换值
     */
    public T convert(Object value) {
        return (T) value;
    }

    /**
     * 数据生成器对象绑定方法，之所以是一个public类型的方法是因为数据生成器可以绑定一个新的
     *
     * @param generator 数据生成器
     */
    public void bind(BaseGenerator<V, T> generator) {
        this.generator = generator;
        this.generator.setAllowNull(allowNull());
        this.generator.setDefaultValue(defaultValue());
        this.preCheck = null;
    }

    /**
     * 返回是否允许空值
     *
     * @return 返回结果
     */
    public Boolean allowNull() {
        return this.allowNull;
    }

    /**
     * 获取默认值
     *
     * @return 返回默认值
     */
    public T defaultValue() {
        return this.defaultValue;
    }

    /**
     * 获取类型的方言模式
     *
     * @return 返回方言模式
     */
    public ObModeType getDialectType() {
        return dialectType;
    }

    /**
     * 检查边界值是否合法
     *
     * @param value 要设定的边界值
     */
    protected void validateValue(V value) {
        if (value == null) {
            return;
        }
        V lowValue = minValueForType();
        V highValue = maxValueForType();
        if (lowValue == null || highValue == null) {
            throw new MockerException(MockerError.ILLEGAL_RETURN_VALUE,
                    "Lowest or highest value can not be null for data type " + toString());
        }
        if (value.compareTo(lowValue) < 0 || value.compareTo(highValue) > 0) {
            throw new MockerException(MockerError.VALUE_OUT_OFRANGE,
                    String.format("Max or min value %s for data type %s is out of range [%s,%s]",
                            value.toString(), toString(), lowValue.toString(), highValue.toString()));
        }
    }

    /**
     * 设定数据类型的低值
     *
     * @param value 低值
     */
    public void setLowValue(V value) {
        validateValue(value);
        if (value.compareTo(highValue()) > 0) {
            throw new MockerException(MockerError.VALUE_OUT_OFRANGE,
                    String.format("Min value can not be bigger than max value \"%s\" for data type %s", highValue().toString(),
                            toString()));
        }
        this.lowValue = value;
    }

    /**
     * 设定数据类型的高值
     *
     * @param value 高值
     */
    public void setHighValue(V value) {
        validateValue(value);
        if (value.compareTo(lowValue()) < 0) {
            throw new MockerException(MockerError.VALUE_OUT_OFRANGE,
                    String.format("Max value can not be smaller than min value \"%s\" for data type %s", lowValue().toString(),
                            toString()));
        }
        this.highValue = value;
    }

    /**
     * 数据生成方法，调用该方法生成一个符合分布的数据
     *
     * @return 返回生成的数据
     */
    public T acquire() {
        if (generator == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Generator can not be null");
        }
        if (this.preCheck == null) {
            this.preCheck = generator.preCheck(lowValue(), highValue());
        }
        if (this.preCheck == null || !this.preCheck) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    String.format("Data check of column \"%s\" for generator is not passed", this));
        }
        return preTreat(generator.next(lowValue(), highValue()));
    }

    /**
     * 获取该数据类型在约束下的最小值
     *
     * @return 返回最小值
     */
    public V lowValue() {
        if (this.lowValue == null) {
            this.lowValue = minValueForType();
        }
        return this.lowValue;
    }

    /**
     * 返回该数据类型在约束下的最大值
     *
     * @return 返回最大值
     */
    public V highValue() {
        if (this.highValue == null) {
            this.highValue = maxValueForType();
        }
        return this.highValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractDataType<T, V> that = (AbstractDataType<T, V>) o;
        return this.dialectType == that.dialectType && getFactory() == that.getFactory();
    }

    @Override
    public int hashCode() {
        StringBuffer buffer = new StringBuffer(getFactory().toString());
        buffer.append(this.dialectType.name());
        return buffer.toString().hashCode();
    }

}
