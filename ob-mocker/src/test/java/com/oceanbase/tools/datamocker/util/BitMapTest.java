package com.oceanbase.tools.datamocker.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test class for bitmap objects
 *
 * @author yh263208
 * @date 2021-01-09 20:56
 * @since OBMOCKER_0.1.0_snapshot
 */
public class BitMapTest extends MockerTestBase {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private String[] getRandomString(int size) {
        String[] strs = new String[size];
        for (int i = 0; i < size; i++) {
            strs[i] = UUID.randomUUID().toString();
        }
        return strs;
    }

    @Test
    public void testBitMap() {
        int capacity = 100000;
        String[] data = getRandomString(capacity);
        Set<String> set = new HashSet<>();
        BitMap bitMap = new BitMap(capacity);
        for (String item : data) {
            Assert.assertFalse(set.contains(item));
            set.add(item);
        }
        int conflictCount = 0;
        for (String item : data) {
            if (bitMap.contains(item)) {
                conflictCount++;
            }
            Assert.assertTrue(bitMap.add(item));
        }
        Assert.assertTrue(conflictCount / (capacity * 1.0) < 0.1);
    }

    @Test
    public void testDupDataForBitMap() {
        String data1 = "hello, world";
        String data2 = "hello,world";
        BitMap bitMap = new BitMap(2);
        Assert.assertTrue(bitMap.add(data1));
        Assert.assertFalse(bitMap.add(null));
        Assert.assertTrue(bitMap.contains(data1));
        Assert.assertFalse(bitMap.contains(data2));
        Assert.assertFalse(bitMap.contains(null));
        Assert.assertTrue(bitMap.clear(data1));
        Assert.assertFalse(bitMap.clear(null));
        Assert.assertFalse(bitMap.contains(data1));
    }

    @Test
    public void testSizeAndCapacityForBitMap() {
        int count = 1234;
        BitMap bitMap = new BitMap(count);
        int pow = new Double(String.valueOf(Math.log(count) / Math.log(2))).intValue() + 1;
        int factorPow = new Double(String.valueOf(Math.log(4) / Math.log(2))).intValue();
        int capacity = 1 << (pow + factorPow);
        Assert.assertEquals(capacity, bitMap.capacity());
        Assert.assertEquals(capacity / 8, bitMap.size());
    }

    @Test
    public void testBitMapWithIllegalCount() {
        thrown.expectMessage("Capacity of the bitmap can not be equal to or smaller than zero");
        thrown.expect(MockerException.class);
        BitMap bitMap = new BitMap(0);
    }
}
