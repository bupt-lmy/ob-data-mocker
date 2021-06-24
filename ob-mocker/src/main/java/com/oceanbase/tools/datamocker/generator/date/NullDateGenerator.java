package com.oceanbase.tools.datamocker.generator.date;

import java.sql.Date;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.BaseDateGenerator;

/**
 * Null data generator of date type
 *
 * @author yh263208
 * @date 2021-01-26 14:42
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NullDateGenerator extends BaseDateGenerator<Date> {
    @Override
    protected Boolean doPreCheck(Date startTime, Date endTime, int scale, TimeUnit timeUnit) {
        return true;
    }

    @Override
    protected Date doGenerate(Date startTime, Date endTime, int scale, TimeUnit timeUnit) {
        return null;
    }

    @Override
    protected Long doCount(Date startTime, Date endTime, int scale, TimeUnit timeUnit) {
        return 1L;
    }

}
