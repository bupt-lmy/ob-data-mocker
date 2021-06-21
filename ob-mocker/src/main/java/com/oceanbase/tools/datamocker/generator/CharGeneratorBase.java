package com.oceanbase.tools.datamocker.generator;

import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;

/**
 * String type data generator, used to generate string type random data
 *
 * @author yh263208
 * @date 2020-12-11 20:26
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class CharGeneratorBase extends BaseGenerator<Integer, String> {
    /**
     * The case setting of the character data generator, the default is case-independent
     */
    private final CharCaseOption caseOption;
    /**
     * Is it a Unicode string
     */
    private Boolean isUnicode = Boolean.FALSE;
    private CharsetType charsetType = CharsetType.UTF_8;

    public CharGeneratorBase(CharCaseOption caseOption) {
        this.caseOption = caseOption;
    }

    public void setCharset(CharsetType charset) {
        this.charsetType = charset;
    }

    public void setUnicode(Boolean isUnicode) {
        this.isUnicode = isUnicode;
    }

    protected CharsetType charset() {
        return this.charsetType;
    }

    protected Boolean unicode() {
        return this.isUnicode;
    }

    protected CharCaseOption caseOption() {
        return this.caseOption;
    }

    /**
     * Pre-checking step, used to check whether the generator can work normally according to the
     * boundary value
     *
     * @param minLength min length for string value
     * @param maxLength max length for string value
     * @return Return the verification result
     */
    @Override
    abstract public Boolean preCheck(Integer minLength, Integer maxLength);

    /**
     * Data generation method interface
     *
     * @param minLength The minimum value, the character generation task reflects the minimum byte value
     *        of the character
     * @param maxLength The maximum value, the character generation task reflects the minimum byte value
     *        of the character
     * @return Returns a generated specific value
     */
    @Override
    abstract public String generate(Integer minLength, Integer maxLength);

    /**
     * Return the total number of unique data that the data generator can generate
     *
     * @return Return a specific value, or null if the data generator can generate data without
     *         limitation
     */
    @Override
    abstract public Long count(Integer minLength, Integer maxLength);
}
