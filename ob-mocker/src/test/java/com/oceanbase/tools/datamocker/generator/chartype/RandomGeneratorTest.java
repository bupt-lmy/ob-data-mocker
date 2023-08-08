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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import org.junit.Assert;
import org.junit.Test;

/**
 * Random string generator test class
 *
 * @author yh263208
 * @date 2020-12-16 16:37
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomGeneratorTest extends MockerTestBase {

    @Test
    public void testRandomGenerator() {
        RandomGenerator generator = new RandomGenerator(CharCaseOption.DEFAULT);
        generator.preCheck(12, 15);
        Map<Integer, Integer> result = new HashMap<>();
        int totalCount = 10000;
        for (int i = 0; i < totalCount; i++) {
            String tmp = generator.generate(12, 15);
            int length = tmp.getBytes().length;
            Assert.assertTrue(length >= 12);
            Assert.assertTrue(length <= 15);
            Integer count = result.getOrDefault(length, 0);
            result.put(length, count + 1);
        }
        Set<Entry<Integer, Integer>> entrySet = result.entrySet();
        int validateCount = totalCount / 4;
        for (Map.Entry<Integer, Integer> entry : entrySet) {
            Assert.assertTrue(entry.getValue() - validateCount <= 300);
        }
    }
}
