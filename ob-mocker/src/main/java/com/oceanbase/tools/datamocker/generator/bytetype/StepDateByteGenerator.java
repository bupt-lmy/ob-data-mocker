package com.oceanbase.tools.datamocker.generator.bytetype;

import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.ByteGeneratorBase;
import com.oceanbase.tools.datamocker.generator.chartype.StepDateCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * Step Date Byte Array Data Generator
 *
 * @author yh263208
 * @date 2020-12-16 23:44
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepDateByteGenerator extends ByteGeneratorBase {
    private final StepDateCharGenerator customGen;

    /**
     * 构造方法
     *
     * @param caseOption 字符大小写配置，该配置在日期范围内无效
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @param timeUnit 时间单位
     * @param cycle 是否轮转
     * @param step 时间步长
     */
    public StepDateByteGenerator(CharCaseOption caseOption, long startTime, long endTime, long step, TimeUnit timeUnit,
            Boolean cycle,
            String timezone) {
        this.customGen = new StepDateCharGenerator(caseOption, startTime, endTime, step, timeUnit, cycle, timezone);
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
