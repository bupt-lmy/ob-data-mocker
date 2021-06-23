package com.oceanbase.tools.datamocker.generator.bytetype;

import com.oceanbase.tools.datamocker.generator.BaseByteGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.FixCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * Constant value byte data generator
 *
 * @author yh263208
 * @date 2020-12-16 23:09
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixByteGenerator extends BaseByteGenerator {
    private final FixCharGenerator customGen;

    public FixByteGenerator(CharCaseOption caseType, String fixText) {
        customGen = new FixCharGenerator(caseType, fixText);
    }

    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        return customGen.preCheck(minLength, maxLength);
    }

    @Override
    public byte[] generate(Integer minLength, Integer maxLength) {
        return customGen.generate(minLength, maxLength).getBytes();
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        return 1L;
    }
}
