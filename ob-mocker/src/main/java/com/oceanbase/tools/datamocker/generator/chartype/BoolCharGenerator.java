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

/**
 * Boolean data generator
 *
 * @author yh263208
 * @date 2020-12-16 23:38
 * @since OBMOCKER_snapshot_0.1.0
 */
public class BoolCharGenerator extends BaseCharGenerator {
    /**
     * Fixed boolean type, null if not passed, representing random boolean type
     */
    private final Boolean fixBool;

    public BoolCharGenerator(CharCaseOption caseType, String fixBool) {
        super(caseType);
        if (fixBool == null || fixBool.length() == 0) {
            this.fixBool = null;
        } else {
            this.fixBool = Boolean.valueOf(fixBool);
        }
    }

    @Override
    protected Boolean doPreCheck(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
        int realLength = "FALSE".length();
        if (realLength >= minLength) {
            return realLength <= maxLength;
        }
        return false;
    }

    @Override
    protected String doGenerate(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
        if (this.fixBool != null) {
            return caseOption.convert(this.fixBool.toString());
        }
        if (Math.random() > 0.5) {
            return "TRUE";
        }
        return "FALSE";
    }

    @Override
    protected Long doCount(Integer minLength, Integer maxLength, CharsetType charsetType, CharCaseOption caseOption,
            boolean isUnicode) {
        if (this.fixBool == null) {
            return 2L;
        }
        return 1L;
    }

}
