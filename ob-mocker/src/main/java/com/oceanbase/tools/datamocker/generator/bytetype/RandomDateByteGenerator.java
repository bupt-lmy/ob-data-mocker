package com.oceanbase.tools.datamocker.generator.bytetype;

import com.oceanbase.tools.datamocker.generator.ByteGeneratorBase;
import com.oceanbase.tools.datamocker.generator.chartype.RandomDateCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * 随机日期字节数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 23:32
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomDateByteGenerator extends ByteGeneratorBase {

    private final RandomDateCharGenerator customGen;

    public RandomDateByteGenerator(CharCaseOption caseOption, long startTime, long endTime, String timezone) {
        customGen = new RandomDateCharGenerator(caseOption, startTime, endTime, timezone);

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
