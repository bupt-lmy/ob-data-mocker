package com.oceanbase.tools.datamocker.generator.chartype;

import com.oceanbase.tools.datamocker.generator.BaseCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Step size digital data generator
 *
 * @author yh263208
 * @date 2020-12-16 13:08
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepNumGenerator extends BaseCharGenerator {
    /**
     * Random data start value
     */
    private Long start;
    /**
     * Random data end value
     */
    private Long end;
    private final Long step;
    /**
     * Whether to loop
     */
    private final Boolean cycle;
    /**
     * Current value
     */
    private Long current = null;

    public StepNumGenerator(CharCaseOption caseOption, Long start, Long end, Long step, Boolean cycle) {
        super(caseOption);
        if (start == null || end == null || step == null || cycle == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Start or end or step or cycle for step number generator can not be null");
        }
        if (start.compareTo(end) >= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Start for step number generator can not be bigger than end");
        }
        this.start = start;
        this.end = end;
        this.step = step;
        this.cycle = cycle;
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
        if (step < 0) {
            return Long.toString(minus());
        }
        return Long.toString(positive());
    }

    /**
     * Random number generation logic when the step size is negative
     *
     * @return Returns the generated random date
     */
    private long minus() {
        if (current == null) {
            current = this.end;
            return current;
        }
        current += step;
        if (current < this.start) {
            if (cycle) {
                current = this.end;
            } else {
                throw new MockerException(MockerError.OPERATION_FAILURE, "Can not generate more unique date");
            }
        }
        return current;
    }

    /**
     * Random number generation logic when the step size is positive
     *
     * @return Returns the generated random date
     */
    private long positive() {
        if (current == null) {
            current = this.start;
            return current;
        }
        current += step;
        if (current > this.end) {
            if (cycle) {
                current = this.start;
            } else {
                throw new MockerException(MockerError.OPERATION_FAILURE, "Can not generate more unique date");
            }
        }
        return current;
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
