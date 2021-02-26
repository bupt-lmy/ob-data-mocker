package com.oceanbase.tools.datamocker.generator.date;

import com.alipay.oceanbase.jdbc.extend.datatype.INTERVALYM;

import com.oceanbase.tools.datamocker.generator.BaseGenerator;

/**
 * interval year to month类型的空数据生成器
 *
 * @author yh263208
 * @date 2021-01-26 14:42
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NullIntervalYMGenerator extends BaseGenerator<Integer, INTERVALYM> {

    @Override
    public Boolean preCheck(Integer leftLimit, Integer rightLimit) {
        return true;
    }

    @Override
    protected INTERVALYM generate(Integer leftLimit, Integer rightLimit) {
        return null;
    }

    @Override
    public Long count(Integer leftLimit, Integer rightLimit) {
        return 1L;
    }
}
