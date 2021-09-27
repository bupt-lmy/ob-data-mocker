package com.oceanbase.tools.datamocker.generator.date;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.BaseDateGenerator;

/**
 * Data generator with fixed timestamp
 *
 * @author yh263208
 * @date 2020-12-16 19:19
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixTimestampGenerator extends BaseDateGenerator<Timestamp> {
    /**
     * Fixed date and time stamp
     */
    private final long timestamp;

    public FixTimestampGenerator(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    protected Boolean doPreCheck(Timestamp startTime, Timestamp endTime, int scale, TimeUnit minTimeUnit) {
        return this.timestamp >= startTime.getTime() && this.timestamp <= endTime.getTime();
    }

    @Override
    protected Timestamp doGenerate(Timestamp startTime, Timestamp endTime, int scale, TimeUnit minTimeUnit) {
        Timestamp timestamp = new Timestamp(this.timestamp);
        timestamp.setNanos(0);
        return timestamp;
    }

    @Override
    protected Long doCount(Timestamp startTime, Timestamp endTime, int scale, TimeUnit minTimeUnit) {
        return 1L;
    }

}
