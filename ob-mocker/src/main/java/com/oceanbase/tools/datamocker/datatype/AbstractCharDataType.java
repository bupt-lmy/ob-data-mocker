package com.oceanbase.tools.datamocker.datatype;

import java.io.UnsupportedEncodingException;

import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 抽象字符类型，用于描述数据库中字符串类型的数据类型
 *
 * @author yh263208
 * @date 2020-12-11 20:16
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class AbstractCharDataType extends AbstractDataType<String, Integer> {
    /**
     * s是否存储为unicode字符串
     */
    private final Boolean isUnicode;
    /**
     * 数据库的字符串编码格式
     */
    private final CharsetType charsetType;
    /**
     * 字段的长度
     */
    private final Integer length;

    /**
     * 抽象基类的构造函数，在这里需要传入这个数据类型绑定的随机数据生成器，并且指明该数据类型对应的OB模式以及该模式下的数据库类型
     *
     * @param generator   随机数据生成器
     * @param charsetType 字符编码格式
     * @param dialectType OB模式
     * @param length      类型长度
     * @param allowNull   是否允许空值
     */
    public AbstractCharDataType(CharGeneratorBase generator, ObModeType dialectType, CharsetType charsetType, Integer length,
            String defaultValue, Boolean allowNull, Boolean isUnicode) {
        super(generator, dialectType, defaultValue, allowNull);
        validateParam(charsetType, length);
        generator.setCharset(charsetType);
        generator.setUnicode(isUnicode);
        this.charsetType = charsetType;
        this.length = length;
        this.isUnicode = isUnicode;
    }

    /**
     * 抽象基类的构造函数，在这里需要传入这个数据类型绑定的随机数据生成器，并且指明该数据类型对应的OB模式以及该模式下的数据库类型
     *
     * @param charsetType 字符编码格式
     * @param dialectType OB模式
     * @param length      类型长度
     * @param allowNull   是否允许空值
     */
    protected AbstractCharDataType(ObModeType dialectType, CharsetType charsetType, Integer length, String defaultValue, Boolean allowNull,
            Boolean isUnicode) {
        super(dialectType, defaultValue, allowNull);
        validateParam(charsetType, length);
        this.charsetType = charsetType;
        this.length = length;
        this.isUnicode = isUnicode;
    }

    /**
     * 验证构造方法中的参数是否合法
     *
     * @param charsetType 字符集类型
     * @param length      字符长度
     * @throws MockerException 验证失败抛出异常
     */
    public void validateParam(CharsetType charsetType, Integer length) {
        if (charsetType == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Char set for data type can not be null");
        }
        if (length == null || length <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Datatype length can not be null or smaller than zero");
        }
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
     * 字符类型的默认最大长度，为构造函数传入的长度，不能小于，零
     *
     * @return 返回长度
     */
    @Override
    protected Integer maxValueForType() {
        return this.length;
    }

    /**
     * 数据生成器对象绑定方法，之所以是一个public类型的方法是因为数据生成器可以绑定一个新的
     *
     * @param generator 数据生成器
     */
    @Override
    public void bind(BaseGenerator<Integer, String> generator) {
        super.bind(generator);
        ((CharGeneratorBase) generator).setCharset(charset());
        ((CharGeneratorBase) generator).setUnicode(isUnicode());
    }

    /**
     * 获取字符串类型的字符编码格式
     *
     * @return 返回字符编码格式
     */
    public CharsetType charset() {
        return this.charsetType;
    }

    /**
     * 返回是否为存储Unicode字符串
     *
     * @return 返回结果
     */
    public Boolean isUnicode() {
        return this.isUnicode;
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
    protected String preTreat(String value) {
        if (value == null) {
            return null;
        }
        Integer maxLength = highValue();
        int realLength;
        try {
            if (isUnicode()) {
                realLength = value.length();
            } else {
                realLength = value.getBytes(charset().getCharSet()).length;
            }
        } catch (UnsupportedEncodingException e) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, e.getMessage());
        }
        if (realLength > maxLength) {
            throw new MockerException(MockerError.VALUE_OUT_OFRANGE,
                    String.format("The length of then generator's value is bigger than bound [0,%d]", maxLength));
        }
        return value;
    }

    /**
     * 返回字符串
     *
     * @param value 参数传入的字符串
     * @return 返回的字符串
     */
    @Override
    public String toString(String value) {
        if (value == null) {
            return "NULL";
        }
        return String.format("'%s'", value);
    }

    /**
     * 返回数据摘要，数字类型的数据摘要就是其本身
     *
     * @param value 值
     * @return 返回摘要
     */
    @Override
    public String toDigest(String value) {
        return value;
    }
}
