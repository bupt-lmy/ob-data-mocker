package com.oceanbase.tools.datamocker.generator.chartype;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.BaseCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.apache.commons.lang.StringUtils;

/**
 * Step date data and generator, used to generate date data with specified steps
 *
 * @author yh263208
 * @date 2020-12-16 00:37
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepDateCharGenerator extends BaseCharGenerator {
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * Start timestamp
     */
    private final long startTime;
    /**
     * End timestamp
     */
    private final long endTime;
    /**
     * Date step
     */
    private final long step;
    /**
     * Whether to loop
     */
    private final Boolean cycle;
    /**
     * Current timestamp
     */
    private Long timestamp = null;

    /**
     * Constructor
     *
     * @param caseOption Character case configuration, the configuration is invalid in the date range
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @param timeUnit Time unit
     * @param cycle Whether to rotate
     * @param step Time Step
     */
    public StepDateCharGenerator(CharCaseOption caseOption, long startTime, long endTime, long step, TimeUnit timeUnit,
            Boolean cycle,
            String timezone) {
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
        this.cycle = cycle;
        if (timeUnit != null) {
            this.step = TimeUnit.MILLISECONDS.convert(step, timeUnit);
        } else {
            this.step = step;
        }
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
        SimpleDateFormat formater = new SimpleDateFormat(DATE_FORMAT);
        if (step < 0) {
            return formater.format(new Date(minus()));
        }
        return formater.format(new Date(positive()));
    }

    @Override
    protected Long doCount(Integer minLength, Integer maxLength, CharsetType charsetType, CharCaseOption caseOption,
            boolean isUnicode) {
        long interval = this.endTime - this.startTime;
        return interval / Math.abs(step);
    }

    /**
     * Random number generation logic when the step size is negative
     *
     * @return Returns the generated random date
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
                throw new MockerException(MockerError.OPERATION_FAILURE, "Can not generate more unique date");
            }
        }
        return timestamp;
    }

    /**
     * Random number generation logic when the step size is positive
     *
     * @return Returns the generated random date
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
                throw new MockerException(MockerError.OPERATION_FAILURE, "Can not generate more unique date");
            }
        }
        return timestamp;
    }

}
