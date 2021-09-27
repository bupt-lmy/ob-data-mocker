package com.oceanbase.tools.datamocker.generator.chartype;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.oceanbase.tools.datamocker.generator.BaseCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.apache.commons.lang.StringUtils;

/**
 * Random date data generator
 *
 * @author yh263208
 * @date 2020-12-16 00:08
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomDateCharGenerator extends BaseCharGenerator {
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * Start timestamp
     */
    private final long startTime;
    /**
     * End timestamp
     */
    private final long endTime;

    public RandomDateCharGenerator(CharCaseOption caseOption, long startTime, long endTime, String timezone) {
        super(caseOption);
        if (startTime >= endTime) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Start time stamp can not be later than end time stamp");
        }
        if (startTime < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Time stamp can not be smaller than zero");
        }
        this.startTime = startTime;
        this.endTime = endTime;
        if (StringUtils.isNotBlank(timezone)) {
            TimeZone zone = TimeZone.getTimeZone(timezone);
            SimpleDateFormat formater = new SimpleDateFormat(DATE_FORMAT);
            formater.setTimeZone(zone);
        }
    }

    @Override
    protected Boolean doPreCheck(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
        int realLength = DATE_FORMAT.length();
        if (realLength >= minLength) {
            return realLength <= maxLength;
        }
        return false;
    }

    @Override
    protected String doGenerate(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
        long timstamp = (long) (Math.random() * (endTime - startTime) + startTime);
        SimpleDateFormat formater = new SimpleDateFormat(DATE_FORMAT);
        return formater.format(new Date(timstamp));
    }

    @Override
    protected Long doCount(Integer minLength, Integer maxLength, CharsetType charsetType, CharCaseOption caseOption,
            boolean isUnicode) {
        long interval = this.endTime - this.startTime;
        return interval / 1000;
    }

}
