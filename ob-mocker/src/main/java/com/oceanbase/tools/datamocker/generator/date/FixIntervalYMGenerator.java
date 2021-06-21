package com.oceanbase.tools.datamocker.generator.date;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alipay.oceanbase.jdbc.extend.datatype.INTERVALYM;

import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Fixed interval year to month data generator
 *
 * @author yh263208
 * @date 2021-02-05 16:12
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixIntervalYMGenerator extends BaseGenerator<Integer, INTERVALYM> {
    /**
     * Fixed interval string
     */
    private final String fixText;
    /**
     * Regular expression, used to verify whether the interval is written correctly
     */
    private static final Pattern PATTERN = Pattern.compile(
            "interval '(\\d{1,9}(\\-\\d{1,2})?)' (year|month)(\\(\\d{1}\\))? (to (year|month))?", Pattern.CASE_INSENSITIVE);
    /**
     * The string used to insert the interval
     */
    private String value;

    public FixIntervalYMGenerator(String fixText) {
        if (fixText == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Interval value for generator can not be null");
        }
        this.fixText = fixText;
    }

    @Override
    public Boolean preCheck(Integer leftLimit, Integer rightLimit) {
        Matcher matcher = PATTERN.matcher(this.fixText);
        if (matcher.find()) {
            String intervalVal = matcher.group(1);
            if (intervalVal == null || intervalVal.length() == 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Interval value is illegal for INTERVALYM");
            }
            int yearLen = intervalVal.split("\\-")[0].length();
            if (yearLen < leftLimit || yearLen > rightLimit) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        String.format("Interval value \"%s\" is out of bound for limit [%d,%d]", intervalVal, leftLimit, rightLimit));
            }
            this.value = intervalVal;
        } else {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    String.format("Fix text \"%s\" for INTERVAL YEAR TO MONTH is illegal, value pattern is \"%s\"", this.fixText,
                            "INTERVAL 'integer [- integer]' {YEAR | MONTH} [(precision)][TO {YEAR | MONTH}]"));
        }
        return true;
    }

    @Override
    protected INTERVALYM generate(Integer leftLimit, Integer rightLimit) {
        return new INTERVALYM(this.value);
    }

    @Override
    public Long count(Integer leftLimit, Integer rightLimit) {
        return 1L;
    }
}
