package com.oceanbase.tools.datamocker.datatype.mysql;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.datatype.AbstractDateDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.generator.DateGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.DialectType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * mysql模式下的date类型，精确到天
 *
 * @author yh263208
 * @date 2020-12-16 11:17
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlDateType extends AbstractDateDataType<Date> {
    /**
     * java应用程序中的日期格式化字符串
     */
    private static final String JAVA_DATE_FORMAT = "yyyy-MM-dd";
    /**
     * 日期格式化器
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(JAVA_DATE_FORMAT);

    public MysqlDateType(DateGeneratorBase<Date> generator, Date defaultValue, Boolean allowNull) {
        super(generator, DialectType.OB_MYSQL, defaultValue, allowNull);
        generator.setTimeUnit(TimeUnit.DAYS);
    }

    public MysqlDateType(TimeZone timeZone, Date defaultValue, Boolean allowNull) {
        super(DialectType.OB_MYSQL, timeZone, defaultValue, allowNull);
    }

    public MysqlDateType(Date defaultValue, Boolean allowNull) {
        super(DialectType.OB_MYSQL, defaultValue, allowNull);
    }

    /**
     * 绑定数据生成器方法
     *
     * @param generator 数据生成器
     */
    @Override
    public void bind(BaseGenerator<Date, Date> generator) {
        super.bind(generator);
        ((DateGeneratorBase) generator).setTimeUnit(TimeUnit.DAYS);
    }

    @Override
    protected Long limitForType(Date minDate, Date maxDate) {
        long interval = maxDate.getTime() - minDate.getTime();
        if (interval < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "time interval can not be smaller than zero");
        }
        return interval / 86400000;
    }

    @Override
    public DataTypeFactory getFactory() {
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
    public synchronized String toString(Date value) {
        if (value == null) {
            return "NULL";
        }
        dateFormat.setTimeZone(timeZone());
        return String.format("'%s'", dateFormat.format(value));
    }
}
