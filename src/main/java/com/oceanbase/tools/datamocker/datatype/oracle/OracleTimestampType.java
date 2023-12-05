/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.tools.datamocker.datatype.oracle;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.datatype.AbstractDateDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.generator.BaseDateGenerator;
import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.model.config.DateDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Timestamp type in oracle mode
 *
 * @author yh263208
 * @date 2020-12-16 17:50
 * @since OBMOCKER_snapshot_0.1.0
 */
public class OracleTimestampType extends AbstractDateDataType<Timestamp> {
    /**
     * Date format string in the database in oracle mode
     */
    private final String oracleDateFormate;
    /**
     * Date format string in java application
     */
    private static final String JAVA_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * The precision of the timestamp type, the precision range is 0-9
     */
    private final int scale;

    public OracleTimestampType(BaseDateGenerator<Timestamp> generator, int scale, Timestamp defaultValue,
            Boolean allowNull) {
        super(generator, ObModeType.OB_ORACLE, defaultValue, allowNull);
        if (scale < 0 || scale > 9) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Scale for timestamp can not smaller than zero or bigger than nine");
        }
        this.scale = scale;
        generator.setScale(scale);
        if (scale > 3) {
            generator.setMinTimeUnit(TimeUnit.MILLISECONDS);
        } else {
            generator.setMinTimeUnit(TimeUnit.SECONDS);
        }
        if (scale == 0) {
            oracleDateFormate = "YYYY-MM-DD HH24:MI:SS.FF";
        } else {
            oracleDateFormate = String.format("YYYY-MM-DD HH24:MI:SS.FF%d", scale);
        }
    }

    public OracleTimestampType(BaseDateGenerator<Timestamp> generator, Timestamp defaultValue, Boolean allowNull) {
        super(generator, ObModeType.OB_ORACLE, defaultValue, allowNull);
        this.scale = 3;
        generator.setScale(this.scale);
        generator.setMinTimeUnit(TimeUnit.SECONDS);
        oracleDateFormate = String.format("YYYY-MM-DD HH24:MI:SS.FF%d", scale);
    }

    @Override
    public void bind(BaseGenerator<Timestamp, Timestamp> generator) {
        super.bind(generator);
        ((BaseDateGenerator<Timestamp>) generator).setScale(scale);
        if (scale > 3) {
            ((BaseDateGenerator<Timestamp>) generator).setMinTimeUnit(TimeUnit.MILLISECONDS);
        } else {
            ((BaseDateGenerator<Timestamp>) generator).setMinTimeUnit(TimeUnit.SECONDS);
        }
    }

    @Override
    protected Long limitForType(Timestamp minDate, Timestamp maxDate) {
        long interval = maxDate.getTime() - minDate.getTime();
        if (interval < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Time interval can not be smaller than zero");
        }
        if (scale > 3) {
            return interval;
        }
        return interval / 1000;
    }

    @Override
    public DataTypeFactory<OracleTimestampType, DateDataTypeConfig, BaseDateGenerator<Timestamp>> getFactory() {
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
    public synchronized String convertToSqlString(Timestamp value, TimeZone timeZone) {
        if (value == null) {
            return "NULL";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(JAVA_DATE_FORMAT);
        dateFormat.setTimeZone(timeZone);
        return String.format("to_timestamp('%s.%d', '%s')", dateFormat.format(value), value.getNanos(),
                oracleDateFormate);
    }
}
