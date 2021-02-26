package com.oceanbase.tools.datamocker.generator.bytetype;

import com.oceanbase.tools.datamocker.generator.ByteGeneratorBase;
import com.oceanbase.tools.datamocker.generator.chartype.FixDateCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * 固定日期类型数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 23:14
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixDateByteGenerator extends ByteGeneratorBase {
    /**
     * 自定义数据生成器
     */
    private FixDateCharGenerator customGen;

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
