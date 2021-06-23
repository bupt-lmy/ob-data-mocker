package com.oceanbase.tools.datamocker.generator.date;

import java.sql.Timestamp;

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
    public Boolean preCheck(Timestamp minValue, Timestamp maxValue) {
        return this.timestamp >= minValue.getTime() && this.timestamp <= maxValue.getTime();
    }

    @Override
    public Timestamp generate(Timestamp minValue, Timestamp maxValue) {
        Timestamp timestamp = new Timestamp(this.timestamp);
        timestamp.setNanos(0);
        return timestamp;
    }

    @Override
    public Long count(Timestamp minValue, Timestamp maxValue) {
        return 1L;
    }

}
