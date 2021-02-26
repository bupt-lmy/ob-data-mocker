package com.oceanbase.tools.datamocker.generator.chartype;

import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * char类型的空数据生成器
 *
 * @author yh263208
 * @date 2021-01-26 14:41
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NullCharGenerator extends CharGeneratorBase {
    public NullCharGenerator() {
        super(CharCaseOption.DEFAULT);
    }

    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        return true;
    }

    @Override
    public String generate(Integer minLength, Integer maxLength) {
        return null;
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        return 1L;
    }
}
