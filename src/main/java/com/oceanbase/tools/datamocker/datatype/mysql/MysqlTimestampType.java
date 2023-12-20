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
package com.oceanbase.tools.datamocker.datatype.mysql;

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
 * Timestamp type in mysql mode
 *
 * @author yh263208
 * @date 2021-01-19 11:39
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MysqlTimestampType extends AbstractDateDataType<Timestamp> {
    /**
     * Date format string in java application
     */
    private static final String JAVA_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * The precision of the timestamp type, the precision range is in the range of 0-6
     */
    private final int scale;

    public MysqlTimestampType(BaseDateGenerator<Timestamp> generator, int scale, Timestamp defaultValue,
            Boolean allowNull) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull);
        if (scale < 0 || scale > 6) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Scale for timestamp can not smaller than zero or bigger than six");
        }
        this.scale = scale;
        generator.setScale(scale);
        if (scale > 3) {
            generator.setMinTimeUnit(TimeUnit.MILLISECONDS);
        } else {
            generator.setMinTimeUnit(TimeUnit.SECONDS);
        }
    }

    public MysqlTimestampType(BaseDateGenerator<Timestamp> generator, Timestamp defaultValue, Boolean allowNull) {
        super(generator, ObModeType.OB_MYSQL, defaultValue, allowNull);
        this.scale = 3;
        generator.setScale(this.scale);
        generator.setMinTimeUnit(TimeUnit.SECONDS);
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
    public DataTypeFactory<MysqlTimestampType, DateDataTypeConfig, BaseDateGenerator<Timestamp>> getFactory() {
        return DataTypeFactory.getInstance("OB_MYSQL_TIMESTAMP");
    }

    @Override
    public String toString() {
        return String.format("timestamp(%d)", this.scale);
    }

    @Override
    protected Timestamp minValueForType() {
        return new Timestamp(-30609820800000L);
    }

    @Override
    protected Timestamp maxValueForType() {
        return new Timestamp(253402214399000L);
    }

    @Override
    public synchronized String convertToSqlString(Timestamp value, TimeZone timeZone) {
        if (value == null) {
            return "NULL";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(JAVA_DATE_FORMAT);
        dateFormat.setTimeZone(timeZone);
        if (this.scale != 0) {
            return String.format("'%s.%d'", dateFormat.format(value), value.getNanos());
        } else {
            return String.format("'%s'", dateFormat.format(value));
        }
    }
}
