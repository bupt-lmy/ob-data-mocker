package com.oceanbase.tools.datamocker.generator.chartype;

import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.util.RegExpTextBuilder;

/**
 * 正则表达式字符串生成器，用于生成符合要求的正则表达式
 *
 * @author yh263208
 * @date 2021-01-16 20:06
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RegExpGenerator extends CharGeneratorBase {
    /**
     * 正则表达式字符串生成工具类，用于生成符合正则表达式的字符串
     */
    private RegExpTextBuilder builder;

    /**
     * 构造函数
     *
     * @param regExp     正则表达式
     * @param caseOption 大小写规定
     */
    public RegExpGenerator(CharCaseOption caseOption, String regExp) {
        super(caseOption);
        this.builder = new RegExpTextBuilder(regExp);
    }

    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        try {
            this.builder.generate(minLength, maxLength);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public String generate(Integer minLength, Integer maxLength) {
        return caseOption().convert(this.builder.generate(minLength, maxLength));
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        return null;
    }
}
