package com.oceanbase.tools.datamocker.generator;

import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;

/**
 * 字符串类型的数据生成器，用于生成字符串类型的随机数据
 *
 * @author yh263208
 * @date 2020-12-11 20:26
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class CharGeneratorBase extends BaseGenerator<Integer, String> {
    /**
     * 字符数据生成器的大小写设定，默认为大小写无关
     */
    private CharCaseOption caseOption;
    /**
     * 是否为Unicode字符串
     */
    private Boolean isUnicode = Boolean.FALSE;
    /**
     * 字符串编码格式
     */
    private CharsetType charsetType = CharsetType.UTF_8;

    public CharGeneratorBase(CharCaseOption caseOption) {
        this.caseOption = caseOption;
    }

    /**
     * 设置字符编码格式
     *
     * @param charset 字符编码格式
     */
    public void setCharset(CharsetType charset) {
        this.charsetType = charset;
    }

    /**
     * 设置是否为unicode字符串
     *
     * @param isUnicode 是否为unicode字符串设置
     */
    public void setUnicode(Boolean isUnicode) {
        this.isUnicode = isUnicode;
    }

    /**
     * 获取字符串的编码格式
     */
    protected CharsetType charset() {
        return this.charsetType;
    }

    /**
     * 返回是否为unicode字符串
     *
     * @return 返回结果
     */
    protected Boolean unicode() {
        return this.isUnicode;
    }

    /**
     * 返回数据生成器的大小写设定
     */
    protected CharCaseOption caseOption() {
        return this.caseOption;
    }

    /**
     * 预检查步骤，用于根据边界值校验该生成器是否可以正常工作
     *
     * @param minLength 最小值，
     * @param maxLength 最大值
     * @return 返回校验结果
     */
    @Override
    abstract public Boolean preCheck(Integer minLength, Integer maxLength);

    /**
     * 数据生成方法接口
     *
     * @param minLength 最小值，对于不同类型的数据生成器含义略有不同，对于数字型的生成任务反映的是生成数字的最小值，
     *                  如果是字符型的生成任务反映的是字符的字节最小值
     * @param maxLength 最大值，对于不同类型的数据生成器含义略有不同，对于数字型的生成任务反映的是生成数字的最小值，
     *                  如果是字符型的生成任务反映的是字符的字节最小值
     * @return 返回一个生成的具体值
     */
    @Override
    abstract public String generate(Integer minLength, Integer maxLength);

    /**
     * 返回数据生成器一共能够生成的不重复的数据个数
     *
     * @return 返回具体的数值，如果数据生成器可以无限制生成数据则返回null
     */
    @Override
    abstract public Long count(Integer minLength, Integer maxLength);
}
