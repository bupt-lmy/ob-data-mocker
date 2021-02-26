package com.oceanbase.tools.datamocker.datatype.mysql;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.datatype.AbstractDigitDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.DialectType;

/**
 * mysql模式下的smallInt数据类型
 *
 * @author yh263208
 * @date 2021-01-28 20:09
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlSmallIntType extends AbstractDigitDataType<BigDecimal> {
    /**
     * smallInt类型的构造函数
     *
     * @param generator    数据生成器
     * @param defaultValue 默认值
     * @param allowNull    是否允许为空
     * @param signed       是否为有符号数
     */
    public MysqlSmallIntType(DigitalGeneratorBase<BigDecimal> generator, BigDecimal defaultValue, Boolean allowNull, Boolean signed) {
        super(generator, DialectType.OB_MYSQL, defaultValue, allowNull, signed);
    }

    @Override
    protected Long limitForType(BigDecimal lowValue, BigDecimal highValue) {
        BigDecimal interval = highValue.subtract(lowValue);
        return Long.valueOf(interval.toPlainString()) + 1;
    }

    /**
     * 转换方法，由于mysql复用了oracle模式的数据生成器，数据使用BigDecimal进行计算必须使用转化方法进行数据类型转换
     *
     * @param value 原值
     * @return 转换值
     */
    @Override
    public BigDecimal convert(Object value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString());
    }

    @Override
    public DataTypeFactory getFactory() {
        if (signed()) {
            return DataTypeFactory.getInstance("OB_MYSQL_SMALLINT");
        }
        return DataTypeFactory.getInstance("OB_MYSQL_SMALLINT_UNSIGNED");
    }

    @Override
    protected BigDecimal minValueForType() {
        if (signed()) {
            return new BigDecimal("-32768");
        }
        return new BigDecimal("0");
    }

    @Override
    protected BigDecimal maxValueForType() {
        if (signed()) {
            return new BigDecimal("32767");
        }
        return new BigDecimal("65535");
    }

    @Override
    protected BigDecimal preTreat(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(0, BigDecimal.ROUND_DOWN);
    }

    @Override
    public String toString() {
        if (signed()) {
            return "smallint";
        }
        return "smallint unsigned";
    }

    @Override
    public String toString(BigDecimal value) {
        if (value == null) {
            return "NULL";
        }
        return value.toPlainString();
    }
}
