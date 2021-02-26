package com.oceanbase.tools.datamocker.generator.chartype;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.apache.commons.lang.StringUtils;

/**
 * 随机日期数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 00:08
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomDateCharGenerator extends CharGeneratorBase {
    /**
     * 日期格式
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * 日期格式化
     */
    private final SimpleDateFormat formater = new SimpleDateFormat(DATE_FORMAT);
    /**
     * 开始的时间戳
     */
    private final long startTime;
    /**
     * 结束的时间戳
     */
    private final long endTime;

    public RandomDateCharGenerator(CharCaseOption caseOption, long startTime, long endTime, String timezone) {
        super(caseOption);
        if (startTime >= endTime) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "start time stamp can not be later than end time stamp");
        }
        if (startTime < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "time stamp can not be smaller than zero");
        }
        this.startTime = startTime;
        this.endTime = endTime;
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
        long timstamp = (long) (Math.random() * (endTime - startTime) + startTime);
        return formater.format(new Date(timstamp));
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        long interval = this.endTime - this.startTime;
        return interval / 1000;
    }
}