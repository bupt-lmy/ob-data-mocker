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
package com.oceanbase.tools.datamocker.generator;

import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import lombok.Setter;

/**
 * String type data generator, used to generate string type random data
 *
 * @author yh263208
 * @date 2020-12-11 20:26
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class BaseCharGenerator extends BaseGenerator<Integer, String> {
    /**
     * The case setting of the character data generator, the default is case-independent
     */
    private final CharCaseOption caseOption;
    /**
     * Is it a Unicode string
     */
    @Setter
    private boolean isUnicode = false;
    @Setter
    private CharsetType charsetType = CharsetType.UTF_8;

    public BaseCharGenerator(CharCaseOption caseOption) {
        this.caseOption = caseOption;
    }

    /**
     * Pre-checking step, used to check whether the generator can work normally according to the
     * boundary value
     *
     * @param minLength min length for string value
     * @param maxLength max length for string value
     * @param charsetType char set type
     * @param caseOption case option
     * @param isUnicode isUnicode
     * @return Return the verification result
     */
    abstract protected Boolean doPreCheck(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption, boolean isUnicode);

    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        return doPreCheck(minLength, maxLength, charsetType, caseOption, isUnicode);
    }

    /**
     * Data generation method interface
     *
     * @param minLength The minimum value, the character generation task reflects the minimum byte value
     *        of the character
     * @param maxLength The maximum value, the character generation task reflects the minimum byte value
     *        of the character
     * @param charsetType char set type
     * @param caseOption case option
     * @param isUnicode isUnicode
     * @return Returns a generated specific value
     */
    abstract protected String doGenerate(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption, boolean isUnicode);

    @Override
    public String generate(Integer minLength, Integer maxLength) {
        String returnVal = doGenerate(minLength, maxLength, charsetType, caseOption, isUnicode);
        if (CharCaseOption.ALL_LOWER_CASE.equals(caseOption)) {
            return returnVal.toLowerCase();
        } else if (CharCaseOption.ALL_UPPER_CASE.equals(caseOption)) {
            return returnVal.toUpperCase();
        }
        return returnVal;
    }

    /**
     * Return the total number of unique data that the data generator can generate
     *
     * @param minLength The minimum value, the character generation task reflects the minimum byte value
     *        of the character
     * @param maxLength The maximum value, the character generation task reflects the minimum byte value
     *        of the character
     * @param charsetType char set type
     * @param caseOption case option
     * @param isUnicode isUnicode
     * @return Return a specific value, or null if the data generator can generate data without
     *         limitation
     */
    abstract protected Long doCount(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption, boolean isUnicode);

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        return doCount(minLength, maxLength, charsetType, caseOption, isUnicode);
    }
}
