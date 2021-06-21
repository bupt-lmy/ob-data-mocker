package com.oceanbase.tools.datamocker.generator.bytetype;

import com.oceanbase.tools.datamocker.generator.ByteGeneratorBase;
import com.oceanbase.tools.datamocker.generator.chartype.RegExpGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * Regular expression string generator,
 * used to generate regular expressions that meet the requirements
 *
 * @author yh263208
 * @date 2021-01-16 20:06
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RegExpByteGenerator extends ByteGeneratorBase {
    private final RegExpGenerator customGen;

    public RegExpByteGenerator(CharCaseOption caseType, String regExp) {
        this.customGen = new RegExpGenerator(caseType, regExp);
    }

    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        return customGen.preCheck(minLength, maxLength);
    }

    @Override
    public byte[] generate(Integer minLength, Integer maxLength) {
        String returnVal = customGen.generate(minLength, maxLength);
        return returnVal == null ? null : returnVal.getBytes();
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        return customGen.count(minLength, maxLength);
    }
}
