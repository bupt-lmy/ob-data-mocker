package com.oceanbase.tools.datamocker.generator.bytetype;

import com.oceanbase.tools.datamocker.generator.BaseByteGenerator;

/**
 * Null data generator
 *
 * @author yh263208
 * @date 2021-01-26 14:40
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NullByteGenerator extends BaseByteGenerator {

    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        return true;
    }

    @Override
    public byte[] generate(Integer minLength, Integer maxLength) {
        return null;
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        return 1L;
    }
}
