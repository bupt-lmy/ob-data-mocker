package com.oceanbase.tools.datamocker.generator.date;

import java.sql.Date;

import com.oceanbase.tools.datamocker.generator.DateGeneratorBase;

/**
 * Null data generator of date type
 *
 * @author yh263208
 * @date 2021-01-26 14:42
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NullDateGenerator extends DateGeneratorBase<Date> {
    @Override
    public Boolean preCheck(Date startTime, Date endTime) {
        return true;
    }

    @Override
    public Date generate(Date startTime, Date endTime) {
        return null;
    }

    @Override
    public Long count(Date startTime, Date endTime) {
        return 1L;
    }
}
