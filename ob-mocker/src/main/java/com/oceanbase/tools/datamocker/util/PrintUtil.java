package com.oceanbase.tools.datamocker.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;

/**
 * Util object for print operation, used to generate formated string value for show
 *
 * @author yh263208
 * @date 2021-06-26 00:22
 * @since OB_MOCKER_snapshot_0.1.2
 */
public class PrintUtil {
    /**
     * Sorted list of timeunis
     */
    private static final List<TimeUnit> SORTED_TIMEUNIT_LIST = Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS,
            TimeUnit.MINUTES, TimeUnit.SECONDS, TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS, TimeUnit.NANOSECONDS);

    /**
     * Get duration string value
     *
     * @param timestamp input timestamp
     * @param timeUnit timeunit for input timestamp
     * @param maxTimeUnit max time unit, eg. if maxTimeUnit=HOURS, the readable string will be
     *        <code>xxx hrs xxx mins xxx seconds</code>
     * @param minTimeUnit min time unit, eg. if minTimeUnit=MIN, the readable string will be
     *        <code>xxx hrs xxx mins</code>
     * @return readable string value
     */
    public static String convertToReadableTimeString(long timestamp, TimeUnit timeUnit, TimeUnit maxTimeUnit,
            TimeUnit minTimeUnit) {
        Validate.isTrue(timestamp >= 0, "Time can not be negative for PrintUtil#convertToReadableTimeString");
        Validate.notNull(timeUnit, "TimeUnit can not be null for PrintUtil#convertToReadableTimeString");
        Validate.notNull(maxTimeUnit, "MaxTimeUnit can not be null for PrintUtil#convertToReadableTimeString");
        Validate.notNull(minTimeUnit, "MinTimeUnit can not be null for PrintUtil#convertToReadableTimeString");
        Validate.isTrue(maxTimeUnit.compareTo(minTimeUnit) >= 0, "MinTimeUnit can not be bigger than MaxTimeUnit");
        int beginIndex = -1;
        int endIndex = -1;
        for (int i = 0; i < SORTED_TIMEUNIT_LIST.size(); i++) {
            TimeUnit unitItem = SORTED_TIMEUNIT_LIST.get(i);
            if (unitItem.compareTo(maxTimeUnit) <= 0 && unitItem.convert(timestamp, timeUnit) != 0) {
                beginIndex = i;
                if (unitItem.compareTo(minTimeUnit) >= 0) {
                    endIndex = SORTED_TIMEUNIT_LIST.indexOf(minTimeUnit);
                } else {
                    endIndex = beginIndex;
                }
                break;
            }
        }
        if (beginIndex == -1) {
            return "0 " + toReadableTimeUnit(minTimeUnit);
        }
        Map<TimeUnit, Long> returnMap = new HashMap<>();
        long remainingTime = timestamp;
        for (int i = beginIndex; i <= endIndex; i++) {
            TimeUnit currentUnit = SORTED_TIMEUNIT_LIST.get(i);
            long valueTime = currentUnit.convert(remainingTime, timeUnit);
            if (valueTime == 0) {
                break;
            }
            returnMap.putIfAbsent(currentUnit, valueTime);
            remainingTime -= timeUnit.convert(valueTime, currentUnit);
        }
        return returnMap.entrySet().stream().sorted((o1, o2) -> o2.getKey().compareTo(o1.getKey()))
                .map(timeUnitLongEntry -> timeUnitLongEntry.getValue() + " "
                        + toReadableTimeUnit(timeUnitLongEntry.getKey()))
                .collect(Collectors.joining(" "));
    }

    /**
     * Get readable time unit string
     *
     * @param timeUnit timeunit
     * @return readable timeunit string
     */
    private static String toReadableTimeUnit(TimeUnit timeUnit) {
        if (TimeUnit.DAYS.equals(timeUnit)) {
            return "days";
        } else if (TimeUnit.HOURS.equals(timeUnit)) {
            return "hrs";
        } else if (TimeUnit.MINUTES.equals(timeUnit)) {
            return "mins";
        } else if (TimeUnit.SECONDS.equals(timeUnit)) {
            return "secs";
        } else if (TimeUnit.MILLISECONDS.equals(timeUnit)) {
            return "millSecs";
        } else if (TimeUnit.MICROSECONDS.equals(timeUnit)) {
            return "microSecs";
        } else {
            return "nanoSecs";
        }
    }
}
