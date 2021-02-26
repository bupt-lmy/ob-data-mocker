package com.oceanbase.tools.datamocker.generator.chartype;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.apache.commons.lang.StringUtils;

/**
 * 步长日期数据和生成器，用于生成指定步长的日期数据
 *
 * @author yh263208
 * @date 2020-12-16 00:37
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepDateCharGenerator extends CharGeneratorBase {
    /**
     * 日期格式
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * 日期格式化
     */
    private final SimpleDateFormat formater = new SimpleDateFormat(DATE_FORMAT);
    ;
    /**
     * 开始的时间戳
     */
    private final long startTime;
    /**
     * 结束的时间戳
     */
    private final long endTime;
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
     * @param caseOption 字符大小写配置，该配置在日期范围内无效
     * @param startTime  开始时间戳
     * @param endTime    结束时间戳
     * @param timeUnit   时间单位
     * @param cycle      是否轮转
     * @param step       时间步长
     */
    public StepDateCharGenerator(CharCaseOption caseOption, long startTime, long endTime, long step, TimeUnit timeUnit, Boolean cycle,
            String timezone) {
        super(caseOption);
        if (startTime >= endTime) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "start time stamp can not be later than end time stamp");
        }
        if (startTime < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "time stamp can not be smaller than zero");
        }
        this.startTime = startTime;
        this.endTime = endTime;
        this.cycle = cycle;
        if (timeUnit != null) {
            this.step = TimeUnit.MILLISECONDS.convert(step, timeUnit);
        } else {
            this.step = step;
        }
        if (StringUtils.isNotBlank(timezone)) {
            TimeZone zone = TimeZone.getTimeZone(timezone);
            formater.setTimeZone(zone);
        }
    }

    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        int realLength = DATE_FORMAT.length();
        if (realLength >= minLength) {
            if (realLength <= maxLength) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String generate(Integer minLength, Integer maxLength) {
        if (step < 0) {
            return formater.format(new Date(minus()));
        }
        return formater.format(new Date(positive()));
    }

    /**
     * 步长为负数时的随机数生成逻辑
     *
     * @return 返回生成的随机日期
     */
    private long minus() {
        if (timestamp == null) {
            timestamp = this.endTime;
            return timestamp;
        }
        timestamp += step;
        if (timestamp < this.startTime) {
            if (cycle) {
                timestamp = this.endTime;
            } else {
                throw new MockerException(MockerError.OPERATION_FAILURE, "can not generate more unique date");
            }
        }
        return timestamp;
    }

    /**
     * 步长为正数时的随机数生成逻辑
     *
     * @return 返回生成的随机日期
     */
    private long positive() {
        if (timestamp == null) {
            timestamp = this.startTime;
            return timestamp;
        }
        timestamp += step;
        if (timestamp > this.endTime) {
            if (cycle) {
                timestamp = this.startTime;
            } else {
                throw new MockerException(MockerError.OPERATION_FAILURE, "can not generate more unique date");
            }
        }
        return timestamp;
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        long interval = this.endTime - this.startTime;
        return interval / Math.abs(step);
    }
}
