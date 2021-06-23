package com.oceanbase.tools.datamocker.generator.bytetype;

import com.oceanbase.tools.datamocker.generator.BaseByteGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.FixDateCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * Fixed date type data generator
 *
 * @author yh263208
 * @date 2020-12-16 23:14
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixDateByteGenerator extends BaseByteGenerator {

    private final FixDateCharGenerator customGen;

    public FixDateByteGenerator(CharCaseOption caseOption, long timestamp, String timezone) {
        customGen = new FixDateCharGenerator(caseOption, timestamp, timezone);
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
