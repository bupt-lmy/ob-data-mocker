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
package com.oceanbase.tools.datamocker.datatype.oracle;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.generator.chartype.RandomGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Char type test class in oracle mode
 *
 * @author yh263208
 * @date 2020-12-16 16:06
 * @since OBMOCKER_snapshot_0.1.0
 */
public class CharTest extends MockerTestBase {
    /**
     * Test the forward logic, test whether the returned result meets expectations when the precision
     * and effective digits are given for the char object
     */
    @Test
    public void testMinAndMaxValueForChar() {
        OracleCharType charType = new OracleCharType(128, null, false, CharsetType.UTF_8, false);
        Assert.assertEquals(Integer.valueOf(1), charType.lowValue());
        Assert.assertEquals(Integer.valueOf(128), charType.highValue());
        Integer minValue = 32;
        Integer maxValue = 64;
        charType.setLowValue(minValue);
        charType.setHighValue(maxValue);
        Assert.assertEquals(minValue, charType.lowValue());
        Assert.assertEquals(maxValue, charType.highValue());
    }

    @Test
    public void testRandomGeneratorForChar() {
        RandomGenerator generator = new RandomGenerator(CharCaseOption.DEFAULT);
        OracleCharType charType =
                new OracleCharType(128, null, false, CharsetType.UTF_8, new RandomGenerator(CharCaseOption.DEFAULT),
                        false);
        charType.bind(generator);
        Integer minValue = 32;
        Integer maxValue = 64;
        Assert.assertEquals(Long.MAX_VALUE, generator.count(minValue, maxValue).longValue());
        charType.setLowValue(minValue);
        charType.setHighValue(maxValue);
        Assert.assertEquals(minValue, charType.lowValue());
        Assert.assertEquals(maxValue, charType.highValue());
        for (int i = 0; i < 10000; i++) {
            int length = charType.acquire().getBytes().length;
            Assert.assertTrue(length <= 64);
            Assert.assertTrue(length >= 32);
        }
    }
}
