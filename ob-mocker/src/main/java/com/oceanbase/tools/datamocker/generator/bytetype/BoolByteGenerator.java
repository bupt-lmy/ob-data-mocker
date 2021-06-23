package com.oceanbase.tools.datamocker.generator.bytetype;

import com.oceanbase.tools.datamocker.generator.BaseByteGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.BoolCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * Data generator for boolean byte array
 *
 * @author yh263208
 * @date 2020-12-16 23:01
 * @since OBMOCKER_snapshot_0.1.0
 */
public class BoolByteGenerator extends BaseByteGenerator {
    private final BoolCharGenerator customGen;

    public BoolByteGenerator(CharCaseOption caseType, String fixBool) {
        customGen = new BoolCharGenerator(caseType, fixBool);
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
        return customGen.count(minLength, maxLength);
    }
}
