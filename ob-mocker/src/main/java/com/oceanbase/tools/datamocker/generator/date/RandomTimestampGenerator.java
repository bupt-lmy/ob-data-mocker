package com.oceanbase.tools.datamocker.generator.date;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.BaseDateGenerator;

/**
 * Random timestamp data generator
 *
 * @author yh263208
 * @date 2020-12-16 19:55
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomTimestampGenerator extends BaseDateGenerator<Timestamp> {

    @Override
    protected Boolean doPreCheck(Timestamp startTime, Timestamp endTime, int scale, TimeUnit minTimeUnit) {
        return true;
    }

    @Override
    protected Timestamp doGenerate(Timestamp startTime, Timestamp endTime, int scale, TimeUnit minTimeUnit) {
        long timestamp = (long) (Math.random() * (endTime.getTime() - startTime.getTime()) + startTime.getTime());
        Timestamp returnTimestamp = new Timestamp(timestamp);
        if (!startTime.equals(endTime)) {
            returnTimestamp.setNanos(getNanoSeconds(scale));
        }
        return returnTimestamp;
    }

    @Override
    protected Long doCount(Timestamp startTime, Timestamp endTime, int scale, TimeUnit minTimeUnit) {
        long interval = endTime.getTime() - startTime.getTime();
        return minTimeUnit.convert(interval, TimeUnit.MILLISECONDS);
    }

}
