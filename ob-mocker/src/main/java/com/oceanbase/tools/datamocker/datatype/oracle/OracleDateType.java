package com.oceanbase.tools.datamocker.datatype.oracle;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.datatype.AbstractDateDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.generator.DateGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * oracle模式中的date日期类型，精度为秒
 *
 * @author yh263208
 * @date 2020-12-16 14:58
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleDateType extends AbstractDateDataType<Date> {
    /**
     * oracle模式下数据库中的日期格式化字符串
     */
    private static final String ORACLE_DATE_FORMAT = "YYYY-MM-DD HH24:MI:SS";
    /**
     * java应用程序中的日期格式化字符串
     */
    private static final String JAVA_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * 日期格式化器
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(JAVA_DATE_FORMAT);

    public OracleDateType(DateGeneratorBase<Date> generator, Date defaultValue, Boolean allowNull) {
        super(generator, ObModeType.OB_ORACLE, defaultValue, allowNull);
        generator.setTimeUnit(TimeUnit.SECONDS);
    }

    public OracleDateType(TimeZone timeZone, Date defaultValue, Boolean allowNull) {
        super(ObModeType.OB_ORACLE, timeZone, defaultValue, allowNull);
    }

    public OracleDateType(Date defaultValue, Boolean allowNull) {
        super(ObModeType.OB_ORACLE, defaultValue, allowNull);
    }

    /**
     * 绑定数据生成器方法
     *
     * @param generator 数据生成器
     */
    @Override
    public void bind(BaseGenerator<Date, Date> generator) {
        super.bind(generator);
        ((DateGeneratorBase) generator).setTimeUnit(TimeUnit.SECONDS);
    }

    @Override
    protected Long limitForType(Date minDate, Date maxDate) {
        long interval = maxDate.getTime() - minDate.getTime();
        if (interval < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "time interval can not be smaller than zero");
        }
        return interval / 1000;
    }

    @Override
    public DataTypeFactory getFactory() {
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
        dateFormat.setTimeZone(timeZone());
        return String.format("to_date('%s', '%s')", dateFormat.format(value), ORACLE_DATE_FORMAT);
    }
}
