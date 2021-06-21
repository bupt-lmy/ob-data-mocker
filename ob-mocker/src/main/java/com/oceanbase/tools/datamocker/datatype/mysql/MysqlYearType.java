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
 * The year type in mysql mode
 *
 * @author yh263208
 * @date 2021-01-21 14:04
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlYearType extends AbstractDateDataType {
    /**
     * The precision of the timestamp type, the precision range is in the range of 0-6
     */
    private final int scale;
    /**
     * Date format string in java application
     */
    private static final String JAVA_DATE_FORMAT = "yyyy";

    public MysqlYearType(DateGeneratorBase generator, int scale, Date defaultValue, Boolean allowNull) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull);
        generator.setScale(scale);
        generator.setTimeUnit(TimeUnit.DAYS);
        this.scale = scale;
    }

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
        SimpleDateFormat dateFormat = new SimpleDateFormat(JAVA_DATE_FORMAT);
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
