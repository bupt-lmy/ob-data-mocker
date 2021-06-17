package com.oceanbase.tools.datamocker.datatype;

import java.security.NoSuchAlgorithmException;

import com.oceanbase.tools.datamocker.generator.ByteGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.DigestUtil;

/**
 * 抽象字节数据类型
 *
 * @author yh263208
 * @date 2020-12-16 22:35
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class AbstractByteDataType extends AbstractDataType<byte[], Integer> {

    /**
     * 抽象基类的构造函数，在这里需要传入这个数据类型绑定的随机数据生成器，并且指明该数据类型对应的OB模式以及该模式下的数据库类型
     *
     * @param generator   随机数据生成器
     * @param dialectType OB模式
     * @param allowNull   是否允许空值
     */
    public AbstractByteDataType(ByteGeneratorBase generator, ObModeType dialectType, byte[] defaultValue, Boolean allowNull) {
        super(generator, dialectType, defaultValue, allowNull);
    }

    /**
     * 抽象基类的构造函数，在这里需要传入这个数据类型绑定的随机数据生成器，并且指明该数据类型对应的OB模式以及该模式下的数据库类型
     *
     * @param dialectType OB模式
     * @param allowNull   是否允许空值
     */
    protected AbstractByteDataType(ObModeType dialectType, byte[] defaultValue, Boolean allowNull) {
        super(dialectType, defaultValue, allowNull);
    }

    /**
     * 字符类型的最小字节长度， 默认为0
     *
     * @return 返回长度
     */
    @Override
    protected Integer minValueForType() {
        return 1;
    }

    /**
     * 该类型在指定数据生成器约束下最多能生成的不重复数据量
     *
     * @return 返回具体的数值
     */
    @Override
    public Long distinctLimit() {
        if (generator == null || generator.count(lowValue(), highValue()) == null) {
            return Long.MAX_VALUE;
        }
        return generator.count(lowValue(), highValue());
    }

    /**
     * 预检查方法，用于校验数据生成器生成的数据是否合法
     *
     * @param value 待插入的数据
     * @return 返回校验后的数据
     * @throws MockerException 校验失败则抛出异常
     */
    @Override
    protected byte[] preTreat(byte[] value) {
        if (value == null) {
            return null;
        }
        Integer maxLength = highValue();
        if (value.length > maxLength) {
            throw new MockerException(MockerError.VALUE_OUT_OFRANGE,
                    String.format("The length of then generator's value is bigger than bound [0,%d]", maxLength));
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
    public byte[] toDigest(byte[] value) {
        try {
            return DigestUtil.getToken(value);
        } catch (NoSuchAlgorithmException e) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, e.getMessage());
        }
    }
}
