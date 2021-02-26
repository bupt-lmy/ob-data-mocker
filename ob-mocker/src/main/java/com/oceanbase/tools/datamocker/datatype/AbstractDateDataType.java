package com.oceanbase.tools.datamocker.datatype;

import java.util.TimeZone;

import com.oceanbase.tools.datamocker.generator.DateGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.DialectType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 抽象日期类型，用于表征数据库中的日期类型
 *
 * @author yh263208
 * @date 2020-12-16 14:39
 * @since OBMOCKER_0.1.0_snapshot
 */
public abstract class AbstractDateDataType<T extends Comparable> extends AbstractDataType<T, T> {
    /**
     * 时区，默认为当前时区
     */
    private TimeZone timeZone = TimeZone.getDefault();

    /**
     * 抽象基类的构造函数，在这里需要传入这个数据类型绑定的随机数据生成器，并且指明该数据类型对应的OB模式以及该模式下的数据库类型
     *
     * @param generator   随机数据生成器
     * @param dialectType OB模式
     */
    public AbstractDateDataType(DateGeneratorBase<T> generator, DialectType dialectType, T defaultValue, Boolean allowNull) {
        super(generator, dialectType, defaultValue, allowNull);
    }

    /**
     * 抽象基类的构造函数，在这里需要传入这个数据类型绑定的随机数据生成器，并且指明该数据类型对应的OB模式以及该模式下的数据库类型
     *
     * @param dialectType OB模式
     */
    protected AbstractDateDataType(DialectType dialectType, T defaultValue, Boolean allowNull) {
        super(dialectType, defaultValue, allowNull);
    }

    /**
     * 抽象基类的构造函数，在这里需要传入这个数据类型绑定的随机数据生成器，并且指明该数据类型对应的OB模式以及该模式下的数据库类型
     *
     * @param dialectType OB模式
     * @param timeZone    时区
     */
    protected AbstractDateDataType(DialectType dialectType, TimeZone timeZone, T defaultValue, Boolean allowNull) {
        super(dialectType, defaultValue, allowNull);
        this.timeZone = timeZone;
    }

    /**
     * 日期对象设定时区
     *
     * @param timeZone 时区
     */
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * 获取当前设定的时区
     */
    protected TimeZone timeZone() {
        return this.timeZone;
    }

    /**
     * 在类型自身约束（例如精度，有效数字位数）下所能产生的最多不重复数字的位数
     *
     * @return 返回最多能产生的数据个数
     */
    abstract protected Long limitForType(T minDate, T maxDate);

    /**
     * 该类型在指定数据生成器约束下最多能生成的不重复数据量，这个值由两个指标决定，第一个指标是数据生成器本身能生成的不重复数据量
     * 另一个指标就是该数据类型在精度约束下能产生的最多的不重复数据量，二者取小的
     *
     * @return 返回具体的数值
     */
    @Override
    public Long distinctLimit() {
        if (generator == null || generator.count(lowValue(), highValue()) == null) {
            return limitForType(lowValue(), highValue());
        }
        return limitForType(lowValue(), highValue()) < generator.count(lowValue(), highValue()) ?
                limitForType(lowValue(), highValue()) : generator.count(lowValue(), highValue());
    }

    /**
     * 预处理方法，用于校验结果是否合法
     *
     * @param value 用于校验的值
     * @return 返回校验后的结果
     * @throws MockerException 校验失败则抛错
     */
    @Override
    protected T preTreat(T value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(lowValue()) < 0) {
            throw new MockerException(MockerError.ILLEGAL_RETURN_VALUE, "date can not be smaller than low value");
        } else if (value.compareTo(highValue()) > 0) {
            throw new MockerException(MockerError.ILLEGAL_RETURN_VALUE, "date can not be bigger than high value");
        }
        return value;
    }

    /**
     * 返回数据摘要，数字类型的数据摘要就是其本身
     *
     * @param value 值
     * @return 返回摘要
     */
    @Override
    public T toDigest(T value) {
        return value;
    }
}
