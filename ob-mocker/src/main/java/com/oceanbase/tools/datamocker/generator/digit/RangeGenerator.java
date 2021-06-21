package com.oceanbase.tools.datamocker.generator.digit;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.Range;

/**
 * Range data generator, used to generate data within a specific range
 *
 * @author yh263208
 * @date 2020-12-11 21:20
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RangeGenerator extends DigitalGeneratorBase<BigDecimal> {
    /**
     * Weight mapping table
     */
    private final Map<Range<BigDecimal>, Double> weightMap;

    /**
     * Constructor, pass in a weight mapping set, note that the weights in the weight mapping set must
     * add up to one, otherwise an error will be reported
     *
     * @param weightMap Weight mapping table
     */
    public RangeGenerator(Map<Range<BigDecimal>, Double> weightMap) {
        this.weightMap = weightMap;
        if (weightMap == null) {
            throw new MockerException("Weight map can not be null");
        }
        Set<Range<BigDecimal>> keySet = weightMap.keySet();
        Iterator<Range<BigDecimal>> iter = keySet.iterator();
        Double result = 0D;
        while (iter.hasNext()) {
            Range key = iter.next();
            result += weightMap.get(key);
        }
        if (result <= 1.0 && Math.abs(result - 1.0) > 0.01) {
            throw new MockerException("Weight values have to be added to one");
        }
    }

    @Override
    public Boolean preCheck(BigDecimal minValue, BigDecimal maxValue) {
        Boolean validate = null;
        Set<Range<BigDecimal>> keySet = weightMap.keySet();
        Iterator<Range<BigDecimal>> iter = keySet.iterator();
        while (iter.hasNext()) {
            Range key = iter.next();
            if (key.getMin().compareTo(minValue) < 0) {
                validate = false;
            } else if (key.getMax().compareTo(maxValue) > 0) {
                validate = false;
            } else {
                if (validate == null || validate) {
                    validate = true;
                }
            }
        }
        if (validate == null) {
            throw new MockerException(MockerError.VALUE_OUT_OFRANGE, "Input range info is illegal");
        } else if (!validate) {
            throw new MockerException(MockerError.VALUE_OUT_OFRANGE, "Input range info is illegal");
        }
        return true;
    }

    @Override
    public BigDecimal generate(BigDecimal minValue, BigDecimal maxValue) {
        Range<BigDecimal> range = getRange();
        BigDecimal min = range.getMin();
        BigDecimal max = range.getMax();
        return new BigDecimal(Double.toString(Math.random())).multiply(max.subtract(min)).add(min);
    }

    /**
     * Get a random number generation range
     *
     * @return Return range
     */
    private Range getRange() {
        double random = Math.random();
        Set<Range<BigDecimal>> keySet = weightMap.keySet();
        Iterator<Range<BigDecimal>> iter = keySet.iterator();
        Double sum = 0.0;
        Range returnValue = null;
        while (iter.hasNext()) {
            Range key = iter.next();
            if (sum > random) {
                break;
            }
            sum += weightMap.get(key);
            returnValue = key;
        }
        return returnValue;
    }

    /**
     * This generation method can generate unlimited unique data, so it returns null
     *
     * @return Returning null means that an unlimited amount of unique data can be generated
     */
    @Override
    public Long count(BigDecimal minValue, BigDecimal maxValue) {
        return null;
    }
}
