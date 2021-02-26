package com.oceanbase.tools.datamocker.generator.chartype;

import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 随机数字数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 11:11
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomNumGenerator extends CharGeneratorBase {
    /**
     * 随机数据开始值
     */
    private Long start;
    /**
     * 随机数据结束值
     */
    private Long end;

    public RandomNumGenerator(CharCaseOption caseOption, Long start, Long end) {
        super(caseOption);
        if (start == null || end == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "start or end for random number generator can not be null");
        }
        if (start.compareTo(end) >= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "start for random number generator can not be bigger than end");
        }
        this.start = start;
        this.end = end;
    }

    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        int min = this.start.toString().length();
        int max = this.end.toString().length();
        if (min > maxLength) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "start number is illegal");
        }
        if (max < minLength) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "end number is illegal");
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
        Long interval = end - start;
        Long result = new Double(Math.random() * interval + start).longValue();
        if (result.toString().length() < minLength || result.toString().length() > maxLength) {
            throw new MockerException("number result for random number generator is illegal");
        }
        return result.toString();
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
