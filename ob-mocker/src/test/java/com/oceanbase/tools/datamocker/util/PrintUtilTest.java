package com.oceanbase.tools.datamocker.util;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test object for PrintUtil
 *
 * @author yh263208
 * @date 2021-06-26 00:28
 * @since OB_MOCKER_snapshot_0.1.2
 */
public class PrintUtilTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testPrintMillSeconds() {
        long timestamp = 3600000;
        String expectedString = "1 hrs";
        Assert.assertEquals(expectedString, PrintUtil.convertToReadableTimeString(timestamp, TimeUnit.MILLISECONDS,
                TimeUnit.HOURS, TimeUnit.SECONDS));
    }

    @Test
    public void testWithDifferentBoundTimeUnit() {
        long timestamp = 123456;
        String expectedString = "1 days 10 hrs 17 mins 36 secs";
        Assert.assertEquals(expectedString, PrintUtil.convertToReadableTimeString(timestamp, TimeUnit.SECONDS,
                TimeUnit.DAYS, TimeUnit.MILLISECONDS));
        Assert.assertEquals(expectedString,
                PrintUtil.convertToReadableTimeString(timestamp, TimeUnit.SECONDS, TimeUnit.DAYS, TimeUnit.SECONDS));
        expectedString = "1 days 10 hrs 17 mins";
        Assert.assertEquals(expectedString,
                PrintUtil.convertToReadableTimeString(timestamp, TimeUnit.SECONDS, TimeUnit.DAYS, TimeUnit.MINUTES));
        expectedString = "1 days 10 hrs";
        Assert.assertEquals(expectedString,
                PrintUtil.convertToReadableTimeString(timestamp, TimeUnit.SECONDS, TimeUnit.DAYS, TimeUnit.HOURS));
        expectedString = "1 days";
        Assert.assertEquals(expectedString,
                PrintUtil.convertToReadableTimeString(timestamp, TimeUnit.SECONDS, TimeUnit.DAYS, TimeUnit.DAYS));
    }

    @Test
    public void testZeroTime() {
        long timestamp = 0;
        String expectedString = "0 secs";
        Assert.assertEquals(expectedString, PrintUtil.convertToReadableTimeString(timestamp, TimeUnit.MILLISECONDS,
                TimeUnit.HOURS, TimeUnit.SECONDS));
    }

    @Test
    public void testIllegalMinAndMaxTimeUnit() {
        thrown.expectMessage("MinTimeUnit can not be bigger than MaxTimeUnit");
        thrown.expect(IllegalArgumentException.class);
        PrintUtil.convertToReadableTimeString(1000, TimeUnit.SECONDS, TimeUnit.SECONDS, TimeUnit.DAYS);
    }

    @Test
    public void testWithNegativeTimestamp() {
        thrown.expectMessage("Time can not be negative for PrintUtil#convertToReadableTimeString");
        thrown.expect(IllegalArgumentException.class);
        PrintUtil.convertToReadableTimeString(-100, TimeUnit.SECONDS, TimeUnit.DAYS, TimeUnit.SECONDS);
    }

}
