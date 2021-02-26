package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.Range;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 范围随机数生成器测试对象
 *
 * @author yh263208
 * @date 2020-12-11 22:01
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RangeGeneratorTest {
    /**
     * 权重映射表，用于表征各个数据段上数据产生的权重
     */
    private Map<Range<BigDecimal>, Double> weightMap = new HashMap<>();
    private Range<BigDecimal> range1 = new Range<>(new BigDecimal("10"), new BigDecimal("14.12"));
    private Range<BigDecimal> range2 = new Range<>(new BigDecimal("14.23"), new BigDecimal("16.345"));
    private Range<BigDecimal> range3 = new Range<>(new BigDecimal("17.56"), new BigDecimal("19.45"));
    private Range<BigDecimal> range4 = new Range<>(new BigDecimal("20.33"), new BigDecimal("25.33"));
    private Range<BigDecimal> range5 = new Range<>(new BigDecimal("26.78"), new BigDecimal("27.25"));
    private Range<BigDecimal> range6 = new Range<>(new BigDecimal("30.44"), new BigDecimal("35.55"));
    private Range<BigDecimal> range7 = new Range<>(new BigDecimal("36.78"), new BigDecimal("50"));

    @Before
    public void initWeightMap() {
        weightMap.put(range1, 0.13);
        weightMap.put(range2, 0.23);
        weightMap.put(range3, 0.08);
        weightMap.put(range4, 0.16);
        weightMap.put(range5, 0.11);
        weightMap.put(range6, 0.16);
        weightMap.put(range7, 0.13);
    }

    /**
     * 正向逻辑，测试指定权重数段上的随机数生成逻辑
     */
    @Test
    public void testRangeGenerator() {
        RangeGenerator generator = new RangeGenerator(this.weightMap);
        BigDecimal min = new BigDecimal("5");
        BigDecimal max = new BigDecimal("50");
        Map<Range<BigDecimal>, Integer> countMap = new HashMap<>();
        Set<Range<BigDecimal>> keySet = this.weightMap.keySet();
        Iterator<Range<BigDecimal>> iter = keySet.iterator();
        List<Range<BigDecimal>> list = new ArrayList<>();
        while (iter.hasNext()) {
            Range<BigDecimal> key = iter.next();
            list.add(key);
        }
        int totalCount = 10000;
        for (int i = 0; i < totalCount; i++) {
            BigDecimal value = generator.generate(min, max);
            boolean flag = false;
            for (Range<BigDecimal> item : list) {
                flag = item.contain(value);
                if (flag) {
                    Integer count = countMap.getOrDefault(item, 0);
                    countMap.put(item, count + 1);
                    break;
                }
            }
            Assert.assertEquals(true, flag);
        }
        keySet = weightMap.keySet();
        iter = keySet.iterator();
        while (iter.hasNext()) {
            Range key = iter.next();
            Double factor = weightMap.get(key);
            Double realFactor = countMap.get(key).doubleValue() / totalCount;
            Double interval = Math.abs(factor - realFactor);
            Assert.assertEquals(true, interval < 0.05);
        }
    }

    /**
     * 逆向逻辑，测试各个数段上比率和不等于100%的时候
     */
    @Test(expected = MockerException.class)
    public void testRangeGenertorWithErrorRatio() {
        weightMap.put(range1, 0.1);
        RangeGenerator generator = new RangeGenerator(this.weightMap);
    }

    /**
     * 逆向逻辑，测试有个数段上范围超过最大值时
     */
    @Test(expected = MockerException.class)
    public void testRangeGenertorWithOutOfBound() {
        weightMap.put(range3, 0.04);
        weightMap.put(new Range<>(new BigDecimal("10"), new BigDecimal("60")), 0.04);
        RangeGenerator generator = new RangeGenerator(this.weightMap);
        BigDecimal min = new BigDecimal("5");
        BigDecimal max = new BigDecimal("50");
        generator.preCheck(min, max);
        int totalCount = 10000;
        for (int i = 0; i < totalCount; i++) {
            generator.generate(min, max);
        }
    }

    /**
     * 逆向逻辑，测试有个数段上范围低于最小值时
     */
    @Test(expected = MockerException.class)
    public void testRangeGenertorWithOutOfMinBound() {
        weightMap.put(range3, 0.04);
        weightMap.put(new Range<>(new BigDecimal("2"), new BigDecimal("12")), 0.04);
        RangeGenerator generator = new RangeGenerator(this.weightMap);
        BigDecimal min = new BigDecimal("5");
        BigDecimal max = new BigDecimal("50");
        generator.preCheck(min, max);
        int totalCount = 10000;
        for (int i = 0; i < totalCount; i++) {
            generator.generate(min, max);
        }
    }
}
