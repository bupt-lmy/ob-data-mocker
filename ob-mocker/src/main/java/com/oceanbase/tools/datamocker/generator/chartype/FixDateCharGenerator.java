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
 * Fixed value date data generator
 *
 * @author yh263208
 * @date 2020-12-16 00:30
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixDateCharGenerator extends CharGeneratorBase {
    /**
     * Date format
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * Fixed time stamp
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
            SimpleDateFormat formater = new SimpleDateFormat(DATE_FORMAT);
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
        SimpleDateFormat formater = new SimpleDateFormat(DATE_FORMAT);
        return formater.format(new Date(this.timestamp));
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        return 1L;
    }
}
