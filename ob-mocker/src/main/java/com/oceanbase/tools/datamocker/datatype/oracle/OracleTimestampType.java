package com.oceanbase.tools.datamocker.datatype.oracle;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.datatype.AbstractDateDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.generator.DateGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * oracle模式下的时间戳类型
 *
 * @author yh263208
 * @date 2020-12-16 17:50
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleTimestampType extends AbstractDateDataType<Timestamp> {
    /**
     * oracle模式下数据库中的日期格式化字符串
     */
    private final String oracleDateFormate;
    /**
     * java应用程序中的日期格式化字符串
     */
    private static final String JAVA_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * 日期格式化器
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(JAVA_DATE_FORMAT);
    /**
     * 时间戳类型的精度，该精度范围在0-9范围内
     */
    private final int scale;

    public OracleTimestampType(DateGeneratorBase<Timestamp> generator, int scale, Timestamp defaultValue, Boolean allowNull) {
        super(generator, ObModeType.OB_ORACLE, defaultValue, allowNull);
        if (scale < 0 || scale > 9) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "scale for timestamp can not smaller than zero or bigger than nine");
        }
        this.scale = scale;
        generator.setScale(scale);
        if (scale > 3) {
            generator.setTimeUnit(TimeUnit.MILLISECONDS);
        } else {
            generator.setTimeUnit(TimeUnit.SECONDS);
        }
        if (scale == 0) {
            oracleDateFormate = "YYYY-MM-DD HH24:MI:SS.FF";
        } else {
            oracleDateFormate = String.format("YYYY-MM-DD HH24:MI:SS.FF%d", scale);
        }
    }

    public OracleTimestampType(DateGeneratorBase<Timestamp> generator, Timestamp defaultValue, Boolean allowNull) {
        super(generator, ObModeType.OB_ORACLE, defaultValue, allowNull);
        this.scale = 3;
        generator.setScale(this.scale);
        if (scale > 3) {
            generator.setTimeUnit(TimeUnit.MILLISECONDS);
        } else {
            generator.setTimeUnit(TimeUnit.SECONDS);
        }
        oracleDateFormate = String.format("YYYY-MM-DD HH24:MI:SS.FF%d", scale);
    }

    /**
     * 绑定数据生成器方法
     *
     * @param generator 数据生成器
     */
    @Override
    public void bind(BaseGenerator<Timestamp, Timestamp> generator) {
        super.bind(generator);
        ((DateGeneratorBase) generator).setScale(scale);
        if (scale > 3) {
            ((DateGeneratorBase) generator).setTimeUnit(TimeUnit.MILLISECONDS);
        } else {
            ((DateGeneratorBase) generator).setTimeUnit(TimeUnit.SECONDS);
        }
    }

    @Override
    protected Long limitForType(Timestamp minDate, Timestamp maxDate) {
        long interval = maxDate.getTime() - minDate.getTime();
        if (interval < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "time interval can not be smaller than zero");
        }
        if (scale > 3) {
            return interval;
        }
        return interval / 1000;
    }

    @Override
    public DataTypeFactory getFactory() {
        return DataTypeFactory.getInstance("OB_ORACLE_TIMESTAMP");
    }

    @Override
    public String toString() {
        return String.format("TIMESTAMP(%d)", this.scale);
    }

    @Override
    protected Timestamp minValueForType() {
        return new Timestamp(-62135798400000L);
    }

    @Override
    protected Timestamp maxValueForType() {
        return new Timestamp(253402271999000L);
    }

    @Override
    public synchronized String toString(Timestamp value) {
        if (value == null) {
            return "NULL";
        }
        dateFormat.setTimeZone(timeZone());
        return String.format("to_timestamp('%s.%d', '%s')", dateFormat.format(value), value.getNanos(), oracleDateFormate);
    }
}
