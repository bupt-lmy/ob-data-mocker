package com.oceanbase.tools.datamocker.generator.chartype;

import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 步长数字数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 13:08
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepNumGenerator extends CharGeneratorBase {
    /**
     * 随机数据开始值
     */
    private Long start;
    /**
     * 随机数据结束值
     */
    private Long end;
    /**
     * 步长
     */
    private final Long step;
    /**
     * 是否循环
     */
    private final Boolean cycle;
    /**
     * 当前数值
     */
    private Long current = null;

    public StepNumGenerator(CharCaseOption caseOption, Long start, Long end, Long step, Boolean cycle) {
        super(caseOption);
        if (start == null || end == null || step == null || cycle == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Start or end or step or cycle for step number generator can not be null");
        }
        if (start.compareTo(end) >= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Start for step number generator can not be bigger than end");
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
     * 步长为负数时的随机数生成逻辑
     *
     * @return 返回生成的随机日期
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
     * 步长为正数时的随机数生成逻辑
     *
     * @return 返回生成的随机日期
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
