package com.oceanbase.tools.datamocker.generator.date;

import java.sql.Date;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.BaseDateGenerator;
import org.apache.commons.lang.Validate;

/**
 * Fixed date data generator
 *
 * @author yh263208
 * @date 2020-12-16 15:24
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixDateGenerator extends BaseDateGenerator<Date> {
    /**
     * Fixed date and time stamp
     */
    private final long timestamp;

    public FixDateGenerator(long timestamp) {
        Validate.isTrue(timestamp > 0, "Timestamp can not be negative for FixDateGenerator");
        this.timestamp = timestamp;
    }

    @Override
    protected Boolean doPreCheck(Date startTime, Date endTime, int scale, TimeUnit minTimeUnit) {
        return this.timestamp >= startTime.getTime() && this.timestamp <= endTime.getTime();
    }

    @Override
    protected Date doGenerate(Date startTime, Date endTime, int scale, TimeUnit minTimeUnit) {
        return new Date(this.timestamp);
    }

    @Override
    protected Long doCount(Date startTime, Date endTime, int scale, TimeUnit minTimeUnit) {
        return 1L;
    }

}
