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
 * 定值日期数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 00:30
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixDateCharGenerator extends CharGeneratorBase {
    /**
     * 日期格式
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * 日期格式化
     */
    private final SimpleDateFormat formater = new SimpleDateFormat(DATE_FORMAT);
    /**
     * 定值时间戳
     */
    private final long timestamp;

    public FixDateCharGenerator(CharCaseOption caseOption, long timestamp, String timezone) {
        super(caseOption);
        if (timestamp < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Timestamp can not be smaller than zero");
        }
        this.timestamp = timestamp;
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
        return formater.format(new Date(this.timestamp));
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        return 1L;
    }
}
