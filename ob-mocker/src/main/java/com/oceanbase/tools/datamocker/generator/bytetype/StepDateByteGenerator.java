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
package com.oceanbase.tools.datamocker.generator.bytetype;

import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.BaseByteGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.StepDateCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * Step Date Byte Array Data Generator
 *
 * @author yh263208
 * @date 2020-12-16 23:44
 * @since OBMOCKER_snapshot_0.1.0
 */
public class StepDateByteGenerator extends BaseByteGenerator {
    private final StepDateCharGenerator customGen;

    public StepDateByteGenerator(CharCaseOption caseOption, long startTime, long endTime, long step, TimeUnit timeUnit,
            Boolean cycle,
            String timezone) {
        this.customGen = new StepDateCharGenerator(caseOption, startTime, endTime, step, timeUnit, cycle, timezone);
    }

    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        return customGen.preCheck(minLength, maxLength);
    }

    @Override
    public byte[] generate(Integer minLength, Integer maxLength) {
        return customGen.generate(minLength, maxLength).getBytes();
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        return customGen.count(minLength, maxLength);
    }
}
