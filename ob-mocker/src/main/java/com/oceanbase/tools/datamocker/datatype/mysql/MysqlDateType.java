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
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Date type in mysql mode, accurate to the day
 *
 * @author yh263208
 * @date 2020-12-16 11:17
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlDateType extends AbstractDateDataType<Date> {
    /**
     * Date format string in java application
     */
    private static final String JAVA_DATE_FORMAT = "yyyy-MM-dd";

    public MysqlDateType(BaseDateGenerator<Date> generator, Date defaultValue, Boolean allowNull) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull);
        generator.setMinTimeUnit(TimeUnit.DAYS);
    }

    public MysqlDateType(TimeZone timeZone, Date defaultValue, Boolean allowNull) {
        super(ObModeType.OB_MYSQL, timeZone, defaultValue, allowNull);
    }

    public MysqlDateType(Date defaultValue, Boolean allowNull) {
        super(ObModeType.OB_MYSQL, defaultValue, allowNull);
    }

    @Override
    public void bind(BaseGenerator<Date, Date> generator) {
        super.bind(generator);
        ((BaseDateGenerator<Date>) generator).setMinTimeUnit(TimeUnit.DAYS);
    }

    @Override
    protected Long limitForType(Date minDate, Date maxDate) {
        long interval = maxDate.getTime() - minDate.getTime();
        if (interval < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Time interval can not be smaller than zero");
        }
        return interval / 86400000;
    }

    @Override
    public DataTypeFactory<MysqlDateType, DateDataTypeConfig, BaseDateGenerator<Date>> getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_DATE");
    }

    @Override
    protected Date minValueForType() {
        return new Date(-30609820800000L);
    }

    @Override
    protected Date maxValueForType() {
        return new Date(253402271999000L);
    }

    @Override
    public synchronized String convertToSqlString(Date value, TimeZone timeZone) {
        if (value == null) {
            return "NULL";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(JAVA_DATE_FORMAT);
        dateFormat.setTimeZone(timeZone);
        return String.format("'%s'", dateFormat.format(value));
    }
}
