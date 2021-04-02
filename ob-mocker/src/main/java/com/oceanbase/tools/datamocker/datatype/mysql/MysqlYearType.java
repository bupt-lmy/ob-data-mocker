package com.oceanbase.tools.datamocker.datatype.mysql;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.datatype.AbstractDateDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.generator.DateGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * mysql模式下的year类型
 *
 * @author yh263208
 * @date 2021-01-21 14:04
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlYearType extends AbstractDateDataType {
    /**
     * 时间戳类型的精度，该精度范围在0-6范围内
     */
    private final int scale;
    /**
     * java应用程序中的日期格式化字符串
     */
    private static final String JAVA_DATE_FORMAT = "yyyy";
    /**
     * 日期格式化器
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(JAVA_DATE_FORMAT);

    public MysqlYearType(DateGeneratorBase generator, int scale, Date defaultValue, Boolean allowNull) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull);
        generator.setScale(scale);
        generator.setTimeUnit(TimeUnit.DAYS);
        this.scale = scale;
    }

    /**
     * 绑定数据生成器方法
     *
     * @param generator 数据生成器
     */
    @Override
    public void bind(BaseGenerator generator) {
        super.bind(generator);
        ((DateGeneratorBase) generator).setScale(scale);
        ((DateGeneratorBase) generator).setTimeUnit(TimeUnit.DAYS);
    }

    @Override
    protected Long limitForType(Comparable minDate, Comparable maxDate) {
        long interval = ((Date) maxDate).getTime() - ((Date) minDate).getTime();
        return interval / 31536000000L - 1;
    }

    @Override
    public DataTypeFactory getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_YEAR");
    }

    @Override
    protected Comparable minValueForType() {
        return new Date(-2177481600000L);
    }

    @Override
    protected Comparable maxValueForType() {
        return new Date(5838019200000L);
    }

    @Override
    protected Object preTreat(Object value) {
        if (value == null) {
            return null;
        }
        return dateFormat.format((Date) value);
    }

    @Override
    public String toString() {
        return String.format("year(%d)", this.scale);
    }

    @Override
    public synchronized String toString(Object value) {
        if (value == null) {
            return "NULL";
        }
        return value.toString();
    }
}
