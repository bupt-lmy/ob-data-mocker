package com.oceanbase.tools.datamocker.generator.bytetype;

import com.oceanbase.tools.datamocker.generator.BaseByteGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.RandomNumGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * Random number generator
 *
 * @author yh263208
 * @date 2020-12-16 23:38
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomNumByteGenerator extends BaseByteGenerator {

    private final RandomNumGenerator cutomGen;

    public RandomNumByteGenerator(CharCaseOption caseOption, Long start, Long end) {
        this.cutomGen = new RandomNumGenerator(caseOption, start, end);
    }

    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        return cutomGen.preCheck(minLength, maxLength);
    }

    @Override
    public byte[] generate(Integer minLength, Integer maxLength) {
        return cutomGen.generate(minLength, maxLength).getBytes();
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        return cutomGen.count(minLength, maxLength);
    }
}
