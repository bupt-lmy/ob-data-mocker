package com.oceanbase.tools.datamocker.generator.date;

import java.sql.Timestamp;

import com.oceanbase.tools.datamocker.generator.DateGeneratorBase;

/**
 * date类型的空数据生成器
 *
 * @author yh263208
 * @date 2021-01-26 14:42
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NullTimestampGenerator extends DateGeneratorBase<Timestamp> {
    @Override
    public Boolean preCheck(Timestamp startTime, Timestamp endTime) {
        return true;
    }

    @Override
    public Timestamp generate(Timestamp startTime, Timestamp endTime) {
        return null;
    }

    @Override
    public Long count(Timestamp startTime, Timestamp endTime) {
        return 1L;
    }
}
