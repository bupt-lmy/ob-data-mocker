package com.oceanbase.tools.datamocker.generator.date;

import java.sql.Date;

import com.oceanbase.tools.datamocker.generator.DateGeneratorBase;

/**
 * 固定日期数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 15:24
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixDateGenerator extends DateGeneratorBase<Date> {
    /**
     * 固定日期时间戳
     */
    private final long timestamp;

    public FixDateGenerator(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public Boolean preCheck(Date minValue, Date maxValue) {
        return this.timestamp >= minValue.getTime() && this.timestamp <= maxValue.getTime();
    }

    @Override
    public Date generate(Date minValue, Date maxValue) {
        return new Date(this.timestamp);
    }

    @Override
    public Long count(Date minValue, Date maxValue) {
        return 1L;
    }
}
