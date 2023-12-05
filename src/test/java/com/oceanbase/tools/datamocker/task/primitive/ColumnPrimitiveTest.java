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
package com.oceanbase.tools.datamocker.task.primitive;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.core.read.ColumnReader;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.generator.digit.NormalGenerator;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Column generation primitive test
 *
 * @author yh263208
 * @date 2020-12-31 20:11
 * @since OBMOCKER_snaoshot_0.1.0
 */
public class ColumnPrimitiveTest extends MockerTestBase {
    @Rule
    public ExpectedException expect = ExpectedException.none();

    @Test
    public void testColumnPrimitive() {
        OracleNumberType number = new OracleNumberType(5, 2, null, false);
        BigDecimal expectAvg = new BigDecimal("12");
        NormalGenerator generator = new NormalGenerator(expectAvg.doubleValue(), 3);
        number.bind(generator);
        Long count = 10000L;
        Long tmp = count;
        ColumnReader<BigDecimal> primitive = new ColumnReader<>(number, "SALARY", null);
        BigDecimal result = BigDecimal.ZERO;
        while ((--count) > 0) {
            MockColumnData<BigDecimal> entry = primitive.read();
            BigDecimal value = entry.getColumnValue();
            result = result.add(value);
        }
        Assert.assertEquals(Long.valueOf(0), count);
        BigDecimal avg = result.divide(BigDecimal.valueOf(Double.parseDouble(tmp.toString())), BigDecimal.ROUND_DOWN);
        Assert.assertTrue(avg.subtract(expectAvg).abs().doubleValue() < 0.5);
    }

}
