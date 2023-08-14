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
package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.generator.BaseDigitalGenerator;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Fixed number data generator
 *
 * @author yh263208
 * @date 2020-12-16 13:31
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixNumGenerator extends BaseDigitalGenerator<BigDecimal> {
    private final BigDecimal fixNum;

    public FixNumGenerator(BigDecimal fixNum) {
        if (fixNum == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Fix number for FixNumGenerator can not be null");
        }
        this.fixNum = fixNum;
    }

    @Override
    public Boolean preCheck(BigDecimal minValue, BigDecimal maxValue) {
        return this.fixNum.compareTo(minValue) >= 0 && this.fixNum.compareTo(maxValue) <= 0;
    }

    @Override
    public BigDecimal generate(BigDecimal minValue, BigDecimal maxValue) {
        return this.fixNum;
    }

    @Override
    public Long count(BigDecimal minValue, BigDecimal maxValue) {
        return 1L;
    }
}
