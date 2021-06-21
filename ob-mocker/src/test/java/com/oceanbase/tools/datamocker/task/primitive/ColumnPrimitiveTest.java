package com.oceanbase.tools.datamocker.task.primitive;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.core.read.ColumnReader;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.generator.digit.NormalGenerator;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.Pair;
import org.junit.Assert;
import org.junit.Test;

/**
 * Column generation primitive test
 *
 * @author yh263208
 * @date 2020-12-31 20:11
 * @since OBMOCKER_snaoshot_0.1.0
 */
public class ColumnPrimitiveTest extends MockerTestBase {

    @Test
    public void testColumnPrimitive() throws Exception {
        OracleNumberType number = new OracleNumberType(5, 2, null, false);
        BigDecimal expectAvg = new BigDecimal("12");
        NormalGenerator generator = new NormalGenerator(expectAvg.doubleValue(), 3);
        number.bind(generator);
        Long count = 10000L;
        Long tmp = count;
        ColumnReader<BigDecimal> primitive = new ColumnReader<>(number, "SALARY", null);
        BigDecimal result = BigDecimal.ZERO;
        while ((--count) > 0) {
            Pair<String, Pair<AbstractDataType, BigDecimal>> entry = primitive.read();
            Pair<AbstractDataType, BigDecimal> value = entry.getValue();
            result = result.add(value.getValue());
        }
        Assert.assertEquals(Long.valueOf(0), count);
        BigDecimal avg = result.divide(BigDecimal.valueOf(Double.parseDouble(tmp.toString())), BigDecimal.ROUND_DOWN);
        Assert.assertTrue(avg.subtract(expectAvg).abs().doubleValue() < 0.5);
    }

    @Test(expected = MockerException.class)
    public void testColumnPrimitiveWithoutType() {
        ColumnReader primitive = new ColumnReader(null, null, null);
    }

}
