package com.oceanbase.tools.datamocker.generator.bytetype;

import com.oceanbase.tools.datamocker.generator.BaseByteGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.StepNumGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * Sequential digital byte data generator
 *
 * @author yh263208
 * @date 2020-12-16 23:42
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepNumByteGenerator extends BaseByteGenerator {

    private final StepNumGenerator customGen;

    public StepNumByteGenerator(CharCaseOption caseOption, Long start, Long end, Long step, Boolean cycle) {
        this.customGen = new StepNumGenerator(caseOption, start, end, step, cycle);
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
