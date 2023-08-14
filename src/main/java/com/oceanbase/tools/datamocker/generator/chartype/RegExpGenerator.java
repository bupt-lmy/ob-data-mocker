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
import com.oceanbase.tools.datamocker.util.RegExpTextBuilder;

/**
 * Regular expression string generator, used to generate regular expressions that meet the
 * requirements
 *
 * @author yh263208
 * @date 2021-01-16 20:06
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RegExpGenerator extends BaseCharGenerator {
    /**
     * Regular expression string generation tool class, used to generate regular expression strings
     */
    private final RegExpTextBuilder builder;

    /**
     * Constructor
     *
     * @param regExp Regular expression
     * @param caseOption Capitalization
     */
    public RegExpGenerator(CharCaseOption caseOption, String regExp) {
        super(caseOption);
        this.builder = new RegExpTextBuilder(regExp);
    }

    @Override
    protected Boolean doPreCheck(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
        try {
            this.builder.generate(minLength, maxLength);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    protected String doGenerate(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
        return caseOption.convert(this.builder.generate(minLength, maxLength));
    }

    @Override
    protected Long doCount(Integer minLength, Integer maxLength, CharsetType charsetType, CharCaseOption caseOption,
            boolean isUnicode) {
        return null;
    }

}
