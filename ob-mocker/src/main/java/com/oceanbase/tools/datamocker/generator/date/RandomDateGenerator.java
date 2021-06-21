package com.oceanbase.tools.datamocker.generator.date;

import java.sql.Date;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.DateGeneratorBase;

/**
 * Random date data generator
 *
 * @author yh263208
 * @date 2020-12-16 16:09
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomDateGenerator extends DateGeneratorBase<Date> {

    @Override
    public Boolean preCheck(Date minValue, Date maxValue) {
        return true;
    }

    @Override
    public Date generate(Date minValue, Date maxValue) {
        long timstamp = (long) (Math.random() * (maxValue.getTime() - minValue.getTime()) + minValue.getTime());
        return new Date(timstamp);
    }

    @Override
    public Long count(Date minValue, Date maxValue) {
        long interval = maxValue.getTime() - minValue.getTime();
        return timeUnit().convert(interval, TimeUnit.MILLISECONDS);
    }
}
