package com.oceanbase.tools.datamocker.generator.date;

import java.sql.Date;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.DateGeneratorBase;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 顺序日期数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 16:44
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepDateGenerator extends DateGeneratorBase<Date> {
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
    public StepDateGenerator(long step, TimeUnit timeUnit, Boolean cycle) {
        this.cycle = cycle;
        if (timeUnit != null) {
            this.step = TimeUnit.MILLISECONDS.convert(step, timeUnit);
        } else {
            this.step = step;
        }
    }

    @Override
    public Boolean preCheck(Date startTime, Date endTime) {
        return true;
    }

    @Override
    public Date generate(Date startTime, Date endTime) {
        if (step < 0) {
            return new Date(minus(startTime.getTime(), endTime.getTime()));
        }
        return new Date(positive(startTime.getTime(), endTime.getTime()));
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
                throw new MockerException(MockerError.OPERATION_FAILURE, "Can not generate more unique date");
            }
        }
        return timestamp;
    }

    @Override
    public Long count(Date startTime, Date endTime) {
        long interval = endTime.getTime() - startTime.getTime();
        return interval / Math.abs(step);
    }
}
