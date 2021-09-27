package com.oceanbase.tools.datamocker.datatype.mysql;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.datatype.AbstractDateDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseDateGenerator;
import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.model.config.model.DateDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * The year type in mysql mode
 *
 * @author yh263208
 * @date 2021-01-21 14:04
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlYearType extends AbstractDateDataType<Date> {
    /**
     * The precision of the year type, the precision range is in the range of 0-4
     */
    private final int scale;
    /**
     * Date format string in java application
     */
    private static final String JAVA_DATE_FORMAT = "yyyy";

    public MysqlYearType(BaseDateGenerator<Date> generator, int scale, Date defaultValue, Boolean allowNull) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull);
        generator.setScale(scale);
        generator.setMinTimeUnit(TimeUnit.DAYS);
        this.scale = scale;
    }

    @Override
    public void bind(BaseGenerator<Date, Date> generator) {
        super.bind(generator);
        ((BaseDateGenerator<Date>) generator).setScale(scale);
        ((BaseDateGenerator<Date>) generator).setMinTimeUnit(TimeUnit.DAYS);
    }

    @Override
    protected Long limitForType(Date minDate, Date maxDate) {
        long interval = maxDate.getTime() - minDate.getTime();
        return interval / 31536000000L - 1;
    }

    @Override
    public DataTypeFactory<MysqlYearType, DateDataTypeConfig, BaseDateGenerator<Date>> getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_YEAR");
    }

    @Override
    protected Date minValueForType() {
        return new Date(-2177481600000L);
    }

    @Override
    protected Date maxValueForType() {
        return new Date(5838019200000L);
    }

    @Override
    public Object convertFromJavaObjectToJdbcObject(Date javaObject) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(JAVA_DATE_FORMAT);
        return dateFormat.format(javaObject);
    }

    @Override
    public String toString() {
        return String.format("year(%d)", this.scale);
    }

    @Override
    public synchronized String convertToSqlString(Date value, TimeZone timeZone) {
        if (value == null) {
            return "NULL";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(JAVA_DATE_FORMAT);
        return dateFormat.format(value);
    }

}
