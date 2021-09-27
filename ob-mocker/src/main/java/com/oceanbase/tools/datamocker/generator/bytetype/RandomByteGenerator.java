package com.oceanbase.tools.datamocker.generator.bytetype;

import com.oceanbase.tools.datamocker.generator.BaseByteGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.RandomGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * Random byte data generator
 *
 * @author yh263208
 * @date 2020-12-16 23:37
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomByteGenerator extends BaseByteGenerator {
    private final RandomGenerator customGen;

    public RandomByteGenerator(CharCaseOption caseType) {
        this.customGen = new RandomGenerator(caseType);
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
