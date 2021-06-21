package com.oceanbase.tools.datamocker.generator.chartype;

import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Random number data generator
 *
 * @author yh263208
 * @date 2020-12-16 11:11
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomNumGenerator extends CharGeneratorBase {
    /**
     * Random data start value
     */
    private Long start;
    /**
     * Random data end value
     */
    private Long end;

    public RandomNumGenerator(CharCaseOption caseOption, Long start, Long end) {
        super(caseOption);
        if (start == null || end == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Start or end for random number generator can not be null");
        }
        if (start.compareTo(end) >= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Start for random number generator can not be bigger than end");
        }
        this.start = start;
        this.end = end;
    }

    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        int min = this.start.toString().length();
        int max = this.end.toString().length();
        if (min > maxLength) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Start number is illegal");
        }
        if (max < minLength) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "End number is illegal");
        }
        if (min < minLength) {
            this.start = new Double(Math.pow(10, minLength - 1)).longValue();
        }
        if (max > maxLength) {
            this.end = new Double(Math.pow(10, maxLength) - 1).longValue();
        }
        return true;
    }

    @Override
    public String generate(Integer minLength, Integer maxLength) {
        long interval = end - start;
        long result = new Double(Math.random() * interval + start).longValue();
        if (Long.toString(result).length() < minLength || Long.toString(result).length() > maxLength) {
            throw new MockerException("Number result for random number generator is illegal");
        }
        return Long.toString(result);
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        int min = this.start.toString().length();
        int max = this.end.toString().length();
        if (min < minLength) {
            this.start = new Double(Math.pow(10, minLength - 1)).longValue();
        }
        if (max > maxLength) {
            this.end = new Double(Math.pow(10, maxLength) - 1).longValue();
        }
        return this.end - this.start;
    }
}
