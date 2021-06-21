package com.oceanbase.tools.datamocker.generator.chartype;

import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.util.RegExpTextBuilder;

/**
 * Regular expression string generator, used to generate regular expressions that meet the requirements
 *
 * @author yh263208
 * @date 2021-01-16 20:06
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RegExpGenerator extends CharGeneratorBase {
    /**
     * Regular expression string generation tool class, used to generate regular expression strings
     */
    private final RegExpTextBuilder builder;

    /**
     * Constructor
     *
     * @param regExp     Regular expression
     * @param caseOption Capitalization
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
