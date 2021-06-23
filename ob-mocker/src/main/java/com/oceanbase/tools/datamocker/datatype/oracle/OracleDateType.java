package com.oceanbase.tools.datamocker.datatype.oracle;

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
 * The date type in oracle mode, the precision is seconds
 *
 * @author yh263208
 * @date 2020-12-16 14:58
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleDateType extends AbstractDateDataType<Date> {
    /**
     * Date format string in the database in oracle mode
     */
    private static final String ORACLE_DATE_FORMAT = "YYYY-MM-DD HH24:MI:SS";
    /**
     * Date format string in java application
     */
    private static final String JAVA_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public OracleDateType(BaseDateGenerator<Date> generator, Date defaultValue, Boolean allowNull) {
        super(generator, ObModeType.OB_ORACLE, defaultValue, allowNull);
        generator.setTimeUnit(TimeUnit.SECONDS);
    }

    public OracleDateType(TimeZone timeZone, Date defaultValue, Boolean allowNull) {
        super(ObModeType.OB_ORACLE, timeZone, defaultValue, allowNull);
    }

    public OracleDateType(Date defaultValue, Boolean allowNull) {
        super(ObModeType.OB_ORACLE, defaultValue, allowNull);
    }

    @Override
    public void bind(BaseGenerator<Date, Date> generator) {
        super.bind(generator);
        ((BaseDateGenerator<Date>) generator).setTimeUnit(TimeUnit.SECONDS);
    }

    @Override
    protected Long limitForType(Date minDate, Date maxDate) {
        long interval = maxDate.getTime() - minDate.getTime();
        if (interval < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Time interval can not be smaller than zero");
        }
        return interval / 1000;
    }

    @Override
    public DataTypeFactory<OracleDateType, DateDataTypeConfig, BaseDateGenerator<Date>> getFactory() {
        return DataTypeFactory.getInstance("OB_ORACLE_DATE");
    }

    @Override
    protected Date minValueForType() {
        return new Date(-62135798400000L);
    }

    @Override
    protected Date maxValueForType() {
        return new Date(253402271999000L);
    }

    @Override
    public synchronized String toString(Date value) {
        if (value == null) {
            return "NULL";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(JAVA_DATE_FORMAT);
        dateFormat.setTimeZone(timeZone());
        return String.format("to_date('%s', '%s')", dateFormat.format(value), ORACLE_DATE_FORMAT);
    }
}
