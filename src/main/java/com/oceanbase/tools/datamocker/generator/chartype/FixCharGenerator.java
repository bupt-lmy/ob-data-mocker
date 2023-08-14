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

import java.io.UnsupportedEncodingException;

import com.oceanbase.tools.datamocker.generator.BaseCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Fixed value text data generator
 *
 * @author yh263208
 * @date 2020-12-16 23:13
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixCharGenerator extends BaseCharGenerator {
    /**
     * Fixed value text
     */
    private final String fixText;

    /**
     * Constructor
     *
     * @param caseType Character case control configuration
     * @param fixText Fixed value text
     */
    public FixCharGenerator(CharCaseOption caseType, String fixText) {
        super(caseType);
        this.fixText = fixText;
    }

    @Override
    protected Boolean doPreCheck(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
        int realLength;
        if (isUnicode) {
            realLength = this.fixText.length();
        } else {
            try {
                realLength = this.fixText.getBytes(charsetType.getCharSet()).length;
            } catch (UnsupportedEncodingException e) {
                throw new MockerException(MockerError.UNKNOWN_ERROR, e.getMessage());
            }
        }
        if (realLength >= minLength) {
            return realLength <= maxLength;
        }
        return false;
    }

    @Override
    protected String doGenerate(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
        return caseOption.convert(this.fixText);
    }

    @Override
    protected Long doCount(Integer minLength, Integer maxLength, CharsetType charsetType, CharCaseOption caseOption,
            boolean isUnicode) {
        return 1L;
    }

}
