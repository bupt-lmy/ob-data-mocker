package com.oceanbase.tools.datamocker.generator.chartype;

import com.oceanbase.tools.datamocker.generator.BaseCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.util.RegExpTextBuilder;

/**
 * Regular expression string generator, used to generate regular expressions that meet the
 * requirements
 *
 * @author yh263208
 * @date 2021-01-16 20:06
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RegExpGenerator extends BaseCharGenerator {
    /**
     * Regular expression string generation tool class, used to generate regular expression strings
     */
    private final RegExpTextBuilder builder;

    /**
     * Constructor
     *
     * @param regExp Regular expression
     * @param caseOption Capitalization
     */
    public RegExpGenerator(CharCaseOption caseOption, String regExp) {
        super(caseOption);
        this.builder = new RegExpTextBuilder(regExp);
    }

    @Override
    protected Boolean doPreCheck(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
        try {
            this.builder.generate(minLength, maxLength);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    protected String doGenerate(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
        return caseOption.convert(this.builder.generate(minLength, maxLength));
    }

    @Override
    protected Long doCount(Integer minLength, Integer maxLength, CharsetType charsetType, CharCaseOption caseOption,
            boolean isUnicode) {
        return null;
    }

}
