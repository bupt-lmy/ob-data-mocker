package com.oceanbase.tools.datamocker.generator.date;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.DateGeneratorBase;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 顺序时间戳数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 16:44
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepTimestampGenerator extends DateGeneratorBase<Timestamp> {
    /**
     * 日期步长
     */
    private final long step;
    /**
     * 是否循环
     */
    private final Boolean cycle;
    /**
     * 当前生成的数
     */
    private Long timestamp = null;

    /**
     * 构造方法
     *
     * @param timeUnit 时间单位
     * @param cycle    是否轮转
     * @param step     时间步长
     */
    public StepTimestampGenerator(long step, TimeUnit timeUnit, Boolean cycle) {
        this.cycle = cycle;
        if (timeUnit != null) {
            this.step = TimeUnit.MILLISECONDS.convert(step, timeUnit);
        } else {
            this.step = step;
        }
    }

    @Override
    public Boolean preCheck(Timestamp startTime, Timestamp endTime) {
        return true;
    }

    @Override
    public Timestamp generate(Timestamp startTime, Timestamp endTime) {
        Timestamp timestamp;
        if (step < 0) {
            timestamp = new Timestamp(minus(startTime.getTime(), endTime.getTime()));
        } else {
            timestamp = new Timestamp(positive(startTime.getTime(), endTime.getTime()));
        }
        timestamp.setNanos(0);
        return timestamp;
    }

    /**
     * 步长为负数时的随机数生成逻辑
     *
     * @return 返回生成的随机日期
     */
    private long minus(long startTime, long endTime) {
        if (timestamp == null) {
            timestamp = endTime;
            return timestamp;
        }
        timestamp += step;
        if (timestamp < startTime) {
            if (cycle) {
                timestamp = endTime;
            } else {
                throw new MockerException(MockerError.OPERATION_FAILURE, "Can not generate more unique date");
            }
        }
        return timestamp;
    }

    /**
     * 步长为正数时的随机数生成逻辑
     *
     * @return 返回生成的随机日期
     */
    private long positive(long startTime, long endTime) {
        if (timestamp == null) {
            timestamp = startTime;
            return timestamp;
        }
        timestamp += step;
        if (timestamp > endTime) {
            if (cycle) {
                timestamp = startTime;
            } else {
                throw new MockerException(MockerError.OPERATION_FAILURE, "can not generate more unique date");
            }
        }
        return timestamp;
    }

    @Override
    public Long count(Timestamp startTime, Timestamp endTime) {
        long interval = endTime.getTime() - startTime.getTime();
        return interval / Math.abs(step);
    }
}
