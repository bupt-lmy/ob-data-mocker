package com.oceanbase.tools.datamocker.generator.date;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.DateGeneratorBase;

/**
 * 随机时间戳数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 19:55
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomTimestampGenerator extends DateGeneratorBase<Timestamp> {

    @Override
    public Boolean preCheck(Timestamp minValue, Timestamp maxValue) {
        return true;
    }

    @Override
    public Timestamp generate(Timestamp minValue, Timestamp maxValue) {
        long timstamp = (long) (Math.random() * (maxValue.getTime() - minValue.getTime()) + minValue.getTime());
        Timestamp timestamp = new Timestamp(timstamp);
        if (!minValue.equals(maxValue)) {
            timestamp.setNanos(getnano());
        }
        return timestamp;
    }

    @Override
    public Long count(Timestamp minValue, Timestamp maxValue) {
        long interval = maxValue.getTime() - minValue.getTime();
        return timeUnit().convert(interval, TimeUnit.MILLISECONDS);
    }
}
