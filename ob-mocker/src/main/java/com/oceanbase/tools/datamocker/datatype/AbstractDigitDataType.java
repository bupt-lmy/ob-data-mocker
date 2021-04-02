package com.oceanbase.tools.datamocker.datatype;

import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * 数字类型的抽象基类
 *
 * @author yh263208
 * @date 2020-12-10 15:54
 * @since OBMOCKER_snashot_0.1.0
 */
public abstract class AbstractDigitDataType<T extends Comparable> extends AbstractDataType<T, T> {
    /**
     * 是否为有符号数
     */
    private Boolean isSigned = Boolean.TRUE;

    /**
     * 抽象基类的构造函数，在这里需要传入这个数据类型绑定的随机数据生成器，并且指明该数据类型对应的OB模式以及该模式下的数据库类型
     *
     * @param generator   随机数据生成器
     * @param dialectType OB模式
     */
    public AbstractDigitDataType(DigitalGeneratorBase<T> generator, ObModeType dialectType, T defaultValue, Boolean allowNull) {
        super(generator, dialectType, defaultValue, allowNull);
    }

    /**
     * 抽象基类的构造函数，在这里需要传入这个数据类型绑定的随机数据生成器，并且指明该数据类型对应的OB模式以及该模式下的数据库类型
     *
     * @param generator   随机数据生成器
     * @param dialectType OB模式
     */
    public AbstractDigitDataType(DigitalGeneratorBase<T> generator, ObModeType dialectType, T defaultValue, Boolean allowNull,
            Boolean isSigned) {
        super(generator, dialectType, defaultValue, allowNull);
        this.isSigned = isSigned;
    }

    /**
     * 抽象基类的构造函数，在这里需要传入这个数据类型绑定的随机数据生成器，并且指明该数据类型对应的OB模式以及该模式下的数据库类型
     *
     * @param dialectType OB模式
     */
    protected AbstractDigitDataType(ObModeType dialectType, T defaultValue, Boolean allowNull) {
        super(dialectType, defaultValue, allowNull);
    }

    /**
     * 在类型自身约束（例如精度，有效数字位数）下所能产生的最多不重复数字的位数
     *
     * @param lowValue  边界值
     * @param highValue 边界值
     * @return 返回最多能产生的数据个数
     */
    abstract protected Long limitForType(T lowValue, T highValue);

    /**
     * 返回是否为有符号数
     *
     * @return 返回是否有符号的结果
     */
    public Boolean signed() {
        return this.isSigned;
    }

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
