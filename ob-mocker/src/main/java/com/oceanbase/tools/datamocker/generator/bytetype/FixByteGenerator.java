package com.oceanbase.tools.datamocker.generator.bytetype;

import com.oceanbase.tools.datamocker.generator.ByteGeneratorBase;
import com.oceanbase.tools.datamocker.generator.chartype.FixCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * 定值字节数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 23:09
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixByteGenerator extends ByteGeneratorBase {
    private FixCharGenerator customGen;

    /**
     * 构造方法
     *
     * @param caseType 字符大小写控制配置
     * @param fixText  定值文本
     */
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
