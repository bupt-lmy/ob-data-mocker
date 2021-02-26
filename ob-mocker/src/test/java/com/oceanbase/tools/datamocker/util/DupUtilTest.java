package com.oceanbase.tools.datamocker.util;

import java.util.UUID;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * 工具类的测试类，用于测试重复值检测工具类的测试类
 *
 * @author yh263208
 * @date 2021-01-10 23:13
 * @since OBMOCKER_0.1.0_snapshot
 */
public class DupUtilTest extends MockerTestBase {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testDupUtilUsingSet() {
        String data = "Hello,OBMOCKER";
        DuplicatedJudger util = new DuplicatedJudger(3);
        Assert.assertFalse(util.contains(data));
        util.add(data);
        Assert.assertTrue(util.contains(data));
        util.clear();
        Assert.assertFalse(util.contains(data));
    }

    @Test
    public void testDupUtilUsingBitMap() {
        String data = "Hello,OBMOCKER";
        DuplicatedJudger util = new DuplicatedJudger(10001);
        Assert.assertFalse(util.contains(data));
        util.add(data);
        Assert.assertTrue(util.contains(data));
        util.clear();
        Assert.assertFalse(util.contains(data));
    }

    @Test
    public void testDupUtilWithNegativeCount() {
        thrown.expect(MockerException.class);
        thrown.expectMessage("count for DuplicatedJudger can not be equal to or smaleer than zero");
        DuplicatedJudger util = new DuplicatedJudger(-1);
    }

    @Test
    public void testDupUtilWithOutOfBound() {
        int count = 10001;
        DuplicatedJudger dupUtil = new DuplicatedJudger(count);
        for (int i = 0; i < count + 1; i++) {
            if (i == count) {
                thrown.expectMessage(String.format("the max count for DuplicatedJudger is %d, can not add more", count));
                thrown.expect(MockerException.class);
            }
            dupUtil.add(UUID.randomUUID().toString());
        }
    }

}
