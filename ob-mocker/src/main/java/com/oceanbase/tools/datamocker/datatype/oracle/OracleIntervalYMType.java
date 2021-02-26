package com.oceanbase.tools.datamocker.datatype.oracle;

import com.alipay.oceanbase.jdbc.extend.datatype.INTERVALYM;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.model.enums.DialectType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * oracle模式下INTERVAL YEAR TO MONTH数据类型，该数据类型只提供最基本的兼容，因此使用String作为基本的Java类型
 *
 * @author yh263208
 * @date 2021-02-04 12:01
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleIntervalYMType extends AbstractDataType<INTERVALYM, Integer> {
    /**
     * 类型精度，对于interval year to month来说最多到9，默认为2
     */
    private final Integer scale;

    /**
     * interval year to month数据类型兼容java类型的构造函数
     *
     * @param generator    数据生成器
     * @param defaultValue 默认值
     * @param allowNull    是否允许为空
     * @param scale        精度
     * @throws MockerException 若精度值非法则抛出异常
     */
    public OracleIntervalYMType(BaseGenerator<Integer, INTERVALYM> generator, Integer scale, INTERVALYM defaultValue,
            Boolean allowNull) {
        super(generator, DialectType.OB_ORACLE, defaultValue, allowNull);
        if (scale == null) {
            this.scale = 2;
        } else {
            if (scale > 9 || scale < 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        "scale for inter year to month can not be larger than 9 or smaller than 0");
            }
            this.scale = scale;
        }
    }

    @Override
    public DataTypeFactory getFactory() {
        return DataTypeFactory.getInstance("OB_ORACLE_INTERVAL_YEAR_TO_MONTH");
    }

    @Override
    protected Integer minValueForType() {
        return 1;
    }

    @Override
    protected Integer maxValueForType() {
        return this.scale;
    }

    @Override
    public Long distinctLimit() {
        if (generator == null || generator.count(lowValue(), highValue()) == null) {
            return Long.MAX_VALUE;
        }
        return generator.count(lowValue(), highValue());
    }

    @Override
    protected INTERVALYM preTreat(INTERVALYM value) {
        if (value == null) {
            return null;
        }
        byte yearLen = value.getBytes()[value.getBytes().length - 1];
        if (yearLen > this.scale) {
            throw new MockerException(MockerError.VALUE_OUT_OFRANGE,
                    String.format("scale for interval year(%d) to month is out of bound, [%d>%d]", this.scale, yearLen, this.scale));
        }
        return value;
    }

    @Override
    public String toString(INTERVALYM value) {
        if (value == null) {
            return "NULL";
        }
        return String.format("interval %s year to month", value.toString());
    }

    @Override
    public String toString() {
        return String.format("INTERVAL YEAR(%d) TO MONTH", this.scale);
    }

    @Override
    public INTERVALYM toDigest(INTERVALYM value) {
        return value;
    }
}
