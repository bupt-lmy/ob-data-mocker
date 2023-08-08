/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.tools.datamocker.generator.chartype;

import com.oceanbase.tools.datamocker.generator.BaseCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Random number data generator
 *
 * @author yh263208
 * @date 2020-12-16 11:11
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomNumGenerator extends BaseCharGenerator {
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
    protected Boolean doPreCheck(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
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
    protected String doGenerate(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
        long interval = end - start;
        long result = new Double(Math.random() * interval + start).longValue();
        if (Long.toString(result).length() < minLength || Long.toString(result).length() > maxLength) {
            throw new MockerException("Number result for random number generator is illegal");
        }
        return Long.toString(result);
    }

    @Override
    protected Long doCount(Integer minLength, Integer maxLength, CharsetType charsetType, CharCaseOption caseOption,
            boolean isUnicode) {
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
