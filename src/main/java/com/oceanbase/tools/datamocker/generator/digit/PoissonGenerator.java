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
 * Poisson Distribution Random Number Generator
 *
 * @author yh263208
 * @date 2020-12-09 13:51
 * @since ODCMOCKER_snapshot_0.1.0
 */
public class PoissonGenerator extends BaseDigitalGenerator<BigDecimal> {
    /**
     * Mean of Poisson distribution
     */
    private final double lambda;

    /**
     * Constructor
     *
     * @param lambda Pass in an average
     */
    public PoissonGenerator(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public Boolean preCheck(BigDecimal minValue, BigDecimal maxValue) {
        BigDecimal lambdaValue = new BigDecimal(Double.toString(lambda));
        if (lambdaValue.compareTo(minValue) < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Lambda is smaller than min value");
        } else if (lambdaValue.compareTo(maxValue) >= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Lambda is bigger than max value");
        } else if (minValue.compareTo(BigDecimal.ZERO) != 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Min value is not equal to zero");
        }
        return true;
    }

    @Override
    public BigDecimal generate(BigDecimal minValue, BigDecimal maxValue) {
        BigDecimal i = minValue;
        BigDecimal a = new BigDecimal(Double.toString(Math.exp(-lambda)));
        BigDecimal b = new BigDecimal(Double.toString(1.0));
        do {
            b = b.multiply(new BigDecimal(Double.toString(Math.random())));
            i = i.add(BigDecimal.ONE);
        } while (b.compareTo(a) >= 0 && i.compareTo(maxValue) <= 0);
        return i.subtract(BigDecimal.ONE);
    }

    @Override
    public Long count(BigDecimal minValue, BigDecimal maxValue) {
        return null;
    }

}
