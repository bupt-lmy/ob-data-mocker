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
 * 随机字符串生成器测试类
 *
 * @author yh263208
 * @date 2020-12-16 16:37
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomGeneratorTest extends MockerTestBase {
    /**
     * 正向逻辑，测试随机字符串生成器生成的字符串是否在长度范围内符合均匀分布
     */
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
