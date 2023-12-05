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
package com.oceanbase.tools.datamocker.datatype;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.oceanbase.jdbc.extend.datatype.INTERVALYM;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlBigIntType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlBinaryType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlBlobType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlCharType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlDateTimeType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlDateType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlDecimalType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlFloatType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlIntType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlMediumIntType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlSmallIntType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlTextType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlTimeType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlTimestampType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlTinyIntType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlVarCharType;
import com.oceanbase.tools.datamocker.datatype.mysql.MysqlYearType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleBlobType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleCharType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleDateType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleIntervalYMType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNvarCharType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleRawType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleTimestampType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleVarCharType;
import com.oceanbase.tools.datamocker.generator.BaseByteGenerator;
import com.oceanbase.tools.datamocker.generator.BaseCharGenerator;
import com.oceanbase.tools.datamocker.generator.BaseDateGenerator;
import com.oceanbase.tools.datamocker.generator.BaseDigitalGenerator;
import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.generator.GeneratorFactory;
import com.oceanbase.tools.datamocker.model.config.CharDataTypeConfig;
import com.oceanbase.tools.datamocker.model.config.DataTypeConfig;
import com.oceanbase.tools.datamocker.model.config.DateDataTypeConfig;
import com.oceanbase.tools.datamocker.model.config.DigitDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.apache.commons.lang.Validate;

/**
 * Abstract data type class, used to encapsulate some basic data type logic
 *
 * @author yh263208
 * @date 2020-12-10 15:42
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class DataTypeFactory<T extends AbstractDataType<?, ? extends Comparable<?>>, V extends DataTypeConfig, K extends BaseGenerator<? extends Comparable<?>, ?>> {
    /**
     * The year type in mysql mode
     */
    private static final DataTypeFactory<MysqlYearType, DateDataTypeConfig, BaseDateGenerator<Date>> OB_MYSQL_YEAR =
            new DataTypeFactory<MysqlYearType, DateDataTypeConfig, BaseDateGenerator<Date>>() {

                @Override
                public String name() {
                    return "OB_MYSQL_YEAR";
                }

                @Override
                protected MysqlYearType newInstance(DateDataTypeConfig config, BaseDateGenerator<Date> generator) {
                    int scale = config.getScale() == null ? 3 : config.getScale();
                    Date defaultValue = null;
                    Object val = config.getDefaultValue();
                    if (val != null) {
                        defaultValue = new Date(Long.parseLong(val.toString()));
                    }
                    MysqlYearType returnValue =
                            new MysqlYearType(generator, scale, defaultValue, config.getAllowNull());
                    if (config.getTimezone() != null) {
                        returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
                    }
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new Date(Long.parseLong(config.getLowValue().toString())));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new Date(Long.parseLong(config.getHighValue().toString())));
                    }
                    return returnValue;
                }
            };
    /**
     * The datetime type in mysql mode
     */
    private static final DataTypeFactory<MysqlDateTimeType, DateDataTypeConfig, BaseDateGenerator<Timestamp>> OB_MYSQL_DATETIME =
            new DataTypeFactory<MysqlDateTimeType, DateDataTypeConfig, BaseDateGenerator<Timestamp>>() {

                @Override
                public String name() {
                    return "OB_MYSQL_DATETIME";
                }

                @Override
                protected MysqlDateTimeType newInstance(DateDataTypeConfig config,
                        BaseDateGenerator<Timestamp> generator) {
                    int scale = config.getScale() == null ? 0 : config.getScale();
                    Timestamp defaultValue = null;
                    Object val = config.getDefaultValue();
                    if (val != null) {
                        defaultValue = new Timestamp(Long.parseLong(val.toString()));
                    }
                    MysqlDateTimeType returnValue =
                            new MysqlDateTimeType(generator, scale, defaultValue, config.getAllowNull());
                    if (config.getTimezone() != null) {
                        returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
                    }
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new Timestamp(Long.parseLong(config.getLowValue().toString())));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new Timestamp(Long.parseLong(config.getHighValue().toString())));
                    }
                    return returnValue;
                }
            };
    /**
     * The time type in mysql mode
     */
    private static final DataTypeFactory<MysqlTimeType, DateDataTypeConfig, BaseDateGenerator<Timestamp>> OB_MYSQL_TIME =
            new DataTypeFactory<MysqlTimeType, DateDataTypeConfig, BaseDateGenerator<Timestamp>>() {

                @Override
                public String name() {
                    return "OB_MYSQL_TIME";
                }

                @Override
                protected MysqlTimeType newInstance(DateDataTypeConfig config, BaseDateGenerator<Timestamp> generator) {
                    int scale = config.getScale() == null ? 3 : config.getScale();
                    Timestamp defaultValue = null;
                    Object val = config.getDefaultValue();
                    if (val != null) {
                        defaultValue = new Timestamp(Long.parseLong(val.toString()));
                    }
                    MysqlTimeType returnValue =
                            new MysqlTimeType(generator, scale, defaultValue, config.getAllowNull());
                    if (config.getTimezone() != null) {
                        returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
                    }
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new Timestamp(Long.parseLong(config.getLowValue().toString())));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new Timestamp(Long.parseLong(config.getHighValue().toString())));
                    }
                    return returnValue;
                }
            };
    /**
     * The timestamp type in mysql mode
     */
    private static final DataTypeFactory<MysqlTimestampType, DateDataTypeConfig, BaseDateGenerator<Timestamp>> OB_MYSQL_TIMESTAMP =
            new DataTypeFactory<MysqlTimestampType, DateDataTypeConfig, BaseDateGenerator<Timestamp>>() {

                @Override
                public String name() {
                    return "OB_MYSQL_TIMESTAMP";
                }

                @Override
                protected MysqlTimestampType newInstance(DateDataTypeConfig config,
                        BaseDateGenerator<Timestamp> generator) {
                    int scale = config.getScale() == null ? 3 : config.getScale();
                    Timestamp defaultValue = null;
                    Object val = config.getDefaultValue();
                    if (val != null) {
                        defaultValue = new Timestamp(Long.parseLong(val.toString()));
                    }
                    MysqlTimestampType returnValue =
                            new MysqlTimestampType(generator, scale, defaultValue, config.getAllowNull());
                    if (config.getTimezone() != null) {
                        returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
                    }
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new Timestamp(Long.parseLong(config.getLowValue().toString())));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new Timestamp(Long.parseLong(config.getHighValue().toString())));
                    }
                    return returnValue;
                }
            };
    /**
     * The date type in mysql mode
     */
    private static final DataTypeFactory<MysqlDateType, DateDataTypeConfig, BaseDateGenerator<Date>> OB_MYSQL_DATE =
            new DataTypeFactory<MysqlDateType, DateDataTypeConfig, BaseDateGenerator<Date>>() {

                @Override
                public String name() {
                    return "OB_MYSQL_DATE";
                }

                @Override
                protected MysqlDateType newInstance(DateDataTypeConfig config, BaseDateGenerator<Date> generator) {
                    Date defaultValue = null;
                    Object val = config.getDefaultValue();
                    if (val != null) {
                        defaultValue = new Date(Long.parseLong(val.toString()));
                    }
                    MysqlDateType returnValue = new MysqlDateType(generator, defaultValue, config.getAllowNull());
                    if (config.getTimezone() != null) {
                        returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
                    }
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new Date(Long.parseLong(config.getLowValue().toString())));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new Date(Long.parseLong(config.getHighValue().toString())));
                    }
                    return returnValue;
                }
            };
    /**
     * The varbinary type in mysql mode
     */
    private static final DataTypeFactory<MysqlBinaryType, CharDataTypeConfig, BaseByteGenerator> OB_MYSQL_VARBINARY =
            new DataTypeFactory<MysqlBinaryType, CharDataTypeConfig, BaseByteGenerator>() {
                @Override
                public String name() {
                    return "OB_MYSQL_VARBINARY";
                }

                @Override
                protected MysqlBinaryType newInstance(CharDataTypeConfig config, BaseByteGenerator generator) {
                    Validate.notNull(config.getWidth(), "Width for varbinary can not be null");
                    Validate.isTrue(config.getWidth() <= 1048576,
                            String.format("Width for varbinary is too big (max = %d)", config.getWidth()));
                    MysqlBinaryType returnValue =
                            new MysqlBinaryType(null, config.getAllowNull(), config.getWidth(), generator);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The bit type in mysql mode
     */
    private static final DataTypeFactory<MysqlBinaryType, CharDataTypeConfig, BaseByteGenerator> OB_MYSQL_BIT =
            new DataTypeFactory<MysqlBinaryType, CharDataTypeConfig, BaseByteGenerator>() {
                @Override
                public String name() {
                    return "OB_MYSQL_BIT";
                }

                @Override
                protected MysqlBinaryType newInstance(CharDataTypeConfig config, BaseByteGenerator generator) {
                    Validate.notNull(config.getWidth(), "Width for bit can not be null");
                    Validate.isTrue(config.getWidth() <= 64,
                            String.format("Width for bit is too big (max = %d)", config.getWidth()));
                    int byteWidth = config.getWidth() / 8;
                    Validate.isTrue(byteWidth > 0, "Byte width can not be equal to or smaller than zero");
                    MysqlBinaryType returnValue =
                            new MysqlBinaryType(null, config.getAllowNull(), byteWidth, generator);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue() / 8);
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue() / 8);
                    }
                    return returnValue;
                }
            };
    /**
     * The binary type in mysql mode
     */
    private static final DataTypeFactory<MysqlBinaryType, CharDataTypeConfig, BaseByteGenerator> OB_MYSQL_BINARY =
            new DataTypeFactory<MysqlBinaryType, CharDataTypeConfig, BaseByteGenerator>() {
                @Override
                public String name() {
                    return "OB_MYSQL_BINARY";
                }

                @Override
                protected MysqlBinaryType newInstance(CharDataTypeConfig config, BaseByteGenerator generator) {
                    Validate.notNull(config.getWidth(), "Width for binary can not be null");
                    Validate.isTrue(config.getWidth() <= 256,
                            String.format("Width for binary is too big (max = %d)", config.getWidth()));
                    MysqlBinaryType returnValue =
                            new MysqlBinaryType(null, config.getAllowNull(), config.getWidth(), generator);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The longblob type in mysql mode
     */
    private static final DataTypeFactory<MysqlBlobType, CharDataTypeConfig, BaseByteGenerator> OB_MYSQL_LONGBLOB =
            new DataTypeFactory<MysqlBlobType, CharDataTypeConfig, BaseByteGenerator>() {
                @Override
                public String name() {
                    return "OB_MYSQL_LONGBLOB";
                }

                @Override
                protected MysqlBlobType newInstance(CharDataTypeConfig config, BaseByteGenerator generator) {
                    MysqlBlobType returnValue = new MysqlBlobType(8192, null, config.getAllowNull(), generator);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The mediumblob type in mysql mode
     */
    private static final DataTypeFactory<MysqlBlobType, CharDataTypeConfig, BaseByteGenerator> OB_MYSQL_MEDIUMBLOB =
            new DataTypeFactory<MysqlBlobType, CharDataTypeConfig, BaseByteGenerator>() {
                @Override
                public String name() {
                    return "OB_MYSQL_MEDIUMBLOB";
                }

                @Override
                protected MysqlBlobType newInstance(CharDataTypeConfig config, BaseByteGenerator generator) {
                    MysqlBlobType returnValue = new MysqlBlobType(8192, null, config.getAllowNull(), generator);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The blob type in mysql mode
     */
    private static final DataTypeFactory<MysqlBlobType, CharDataTypeConfig, BaseByteGenerator> OB_MYSQL_BLOB =
            new DataTypeFactory<MysqlBlobType, CharDataTypeConfig, BaseByteGenerator>() {
                @Override
                public String name() {
                    return "OB_MYSQL_BLOB";
                }

                @Override
                protected MysqlBlobType newInstance(CharDataTypeConfig config, BaseByteGenerator generator) {
                    MysqlBlobType returnValue = new MysqlBlobType(4096, null, config.getAllowNull(), generator);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The tinyblob type in mysql mode
     */
    private static final DataTypeFactory<MysqlBlobType, CharDataTypeConfig, BaseByteGenerator> OB_MYSQL_TINYBLOB =
            new DataTypeFactory<MysqlBlobType, CharDataTypeConfig, BaseByteGenerator>() {
                @Override
                public String name() {
                    return "OB_MYSQL_TINYBLOB";
                }

                @Override
                protected MysqlBlobType newInstance(CharDataTypeConfig config, BaseByteGenerator generator) {
                    MysqlBlobType returnValue = new MysqlBlobType(255, null, config.getAllowNull(), generator);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The longtext type in mysql mode
     */
    private static final DataTypeFactory<MysqlTextType, CharDataTypeConfig, BaseCharGenerator> OB_MYSQL_LONGTEXT =
            new DataTypeFactory<MysqlTextType, CharDataTypeConfig, BaseCharGenerator>() {
                @Override
                public String name() {
                    return "OB_MYSQL_LONGTEXT";
                }

                @Override
                protected MysqlTextType newInstance(CharDataTypeConfig config, BaseCharGenerator generator) {
                    String charset = config.getCharset();
                    Validate.notNull(charset, "Charset can not be null for longtext");
                    CharsetType charsetType = CharsetType.valueOf(charset);
                    MysqlTextType returnValue = new MysqlTextType(4096, (String) config.getDefaultValue(),
                            config.getAllowNull(), charsetType, generator, false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The mediumtext type in mysql mode
     */
    private static final DataTypeFactory<MysqlTextType, CharDataTypeConfig, BaseCharGenerator> OB_MYSQL_MEDIUMTEXT =
            new DataTypeFactory<MysqlTextType, CharDataTypeConfig, BaseCharGenerator>() {
                @Override
                public String name() {
                    return "OB_MYSQL_MEDIUMTEXT";
                }

                @Override
                protected MysqlTextType newInstance(CharDataTypeConfig config, BaseCharGenerator generator) {
                    String charset = config.getCharset();
                    Validate.notNull(charset, "Charset can not be null for mediumtext");
                    CharsetType charsetType = CharsetType.valueOf(charset);
                    MysqlTextType returnValue = new MysqlTextType(4096, (String) config.getDefaultValue(),
                            config.getAllowNull(), charsetType, generator, false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The text type in mysql mode
     */
    private static final DataTypeFactory<MysqlTextType, CharDataTypeConfig, BaseCharGenerator> OB_MYSQL_TEXT =
            new DataTypeFactory<MysqlTextType, CharDataTypeConfig, BaseCharGenerator>() {
                @Override
                public String name() {
                    return "OB_MYSQL_TEXT";
                }

                @Override
                protected MysqlTextType newInstance(CharDataTypeConfig config, BaseCharGenerator generator) {
                    String charset = config.getCharset();
                    Validate.notNull(charset, "Charset can not be null for text");
                    CharsetType charsetType = CharsetType.valueOf(charset);
                    MysqlTextType returnValue = new MysqlTextType(4096, (String) config.getDefaultValue(),
                            config.getAllowNull(), charsetType, generator, false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The tinytext type in mysql mode
     */
    private static final DataTypeFactory<MysqlTextType, CharDataTypeConfig, BaseCharGenerator> OB_MYSQL_TINYTEXT =
            new DataTypeFactory<MysqlTextType, CharDataTypeConfig, BaseCharGenerator>() {
                @Override
                public String name() {
                    return "OB_MYSQL_TINYTEXT";
                }

                @Override
                protected MysqlTextType newInstance(CharDataTypeConfig config, BaseCharGenerator generator) {
                    String charset = config.getCharset();
                    Validate.notNull(charset, "Charset can not be null for tinytext");
                    CharsetType charsetType = CharsetType.valueOf(charset);
                    MysqlTextType returnValue = new MysqlTextType(255, (String) config.getDefaultValue(),
                            config.getAllowNull(), charsetType, generator, false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The varchar type in mysql mode
     */
    private static final DataTypeFactory<MysqlVarCharType, CharDataTypeConfig, BaseCharGenerator> OB_MYSQL_VARCHAR =
            new DataTypeFactory<MysqlVarCharType, CharDataTypeConfig, BaseCharGenerator>() {
                @Override
                public String name() {
                    return "OB_MYSQL_VARCHAR";
                }

                @Override
                protected MysqlVarCharType newInstance(CharDataTypeConfig config, BaseCharGenerator generator) {
                    String charset = config.getCharset();
                    Validate.notNull(charset, "Charset can not be null for varchar");
                    Integer length = config.getWidth();
                    Validate.notNull(length, "Length can not be null for varchar");
                    CharsetType charsetType = CharsetType.valueOf(charset);
                    MysqlVarCharType returnValue = new MysqlVarCharType(length, (String) config.getDefaultValue(),
                            config.getAllowNull(), charsetType, generator, false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The char type in mysql mode
     */
    private static final DataTypeFactory<MysqlCharType, CharDataTypeConfig, BaseCharGenerator> OB_MYSQL_CHAR =
            new DataTypeFactory<MysqlCharType, CharDataTypeConfig, BaseCharGenerator>() {
                @Override
                public String name() {
                    return "OB_MYSQL_CHAR";
                }

                @Override
                protected MysqlCharType newInstance(CharDataTypeConfig config, BaseCharGenerator generator) {
                    String charset = config.getCharset();
                    Validate.notNull(charset, "Charset can not be null for char");
                    Integer length = config.getWidth();
                    Validate.notNull(length, "Length can not be null for char");
                    CharsetType charsetType = CharsetType.valueOf(charset);
                    MysqlCharType returnValue = new MysqlCharType(length, (String) config.getDefaultValue(),
                            config.getAllowNull(), charsetType, generator, false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The double unsigned type in mysql mode
     */
    private static final DataTypeFactory<MysqlFloatType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_DOUBLE_UNSIGNED =
            new DataTypeFactory<MysqlFloatType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_DOUBLE_UNSIGNED";
                }

                @Override
                protected MysqlFloatType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    if (config.getScale() != null && config.getPrecision() == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR, "Param settings is illegal for double");
                    }
                    int scale = config.getScale() == null ? -1 : config.getScale();
                    int precision = config.getPrecision() == null ? -1 : config.getPrecision();
                    if (scale == -1 && precision != -1) {
                        precision = -1;
                    }
                    BigDecimal defaultValue = config.getDefaultValue() == null ? null
                            : new BigDecimal(config.getDefaultValue().toString());
                    MysqlFloatType returnValue =
                            new MysqlFloatType(precision, scale, generator, defaultValue, config.getAllowNull(), false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The double type in mysql mode
     */
    private static final DataTypeFactory<MysqlFloatType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_DOUBLE =
            new DataTypeFactory<MysqlFloatType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_DOUBLE";
                }

                @Override
                protected MysqlFloatType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    if (config.getScale() != null && config.getPrecision() == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR, "Param settings is illegal for double");
                    }
                    int scale = config.getScale() == null ? -1 : config.getScale();
                    int precision = config.getPrecision() == null ? -1 : config.getPrecision();
                    if (scale == -1 && precision != -1) {
                        precision = -1;
                    }
                    BigDecimal defaultValue = config.getDefaultValue() == null ? null
                            : new BigDecimal(config.getDefaultValue().toString());
                    MysqlFloatType returnValue =
                            new MysqlFloatType(precision, scale, generator, defaultValue, config.getAllowNull(), true);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The float unsigned type in mysql mode
     */
    private static final DataTypeFactory<MysqlFloatType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_FLOAT_UNSIGNED =
            new DataTypeFactory<MysqlFloatType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_FLOAT_UNSIGNED";
                }

                @Override
                protected MysqlFloatType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    if (config.getScale() != null && config.getPrecision() == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR, "Param settings is illegal for float");
                    }
                    int scale = config.getScale() == null ? -1 : config.getScale();
                    int precision = config.getPrecision() == null ? -1 : config.getPrecision();
                    if (scale == -1 && precision != -1) {
                        precision = -1;
                    }
                    BigDecimal defaultValue = config.getDefaultValue() == null ? null
                            : new BigDecimal(config.getDefaultValue().toString());
                    MysqlFloatType returnValue =
                            new MysqlFloatType(precision, scale, generator, defaultValue, config.getAllowNull(), false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The float type in mysql mode
     */
    private static final DataTypeFactory<MysqlFloatType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_FLOAT =
            new DataTypeFactory<MysqlFloatType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_FLOAT";
                }

                @Override
                protected MysqlFloatType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    if (config.getScale() != null && config.getPrecision() == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR, "Param settings is illegal for float");
                    }
                    int scale = config.getScale() == null ? -1 : config.getScale();
                    int precision = config.getPrecision() == null ? -1 : config.getPrecision();
                    if (scale == -1 && precision != -1) {
                        precision = -1;
                    }
                    BigDecimal defaultValue = config.getDefaultValue() == null ? null
                            : new BigDecimal(config.getDefaultValue().toString());
                    MysqlFloatType returnValue =
                            new MysqlFloatType(precision, scale, generator, defaultValue, config.getAllowNull(), true);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The decimal unsigned type in mysql mode
     */
    private static final DataTypeFactory<MysqlDecimalType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_DECIMAL_UNSIGNED =
            new DataTypeFactory<MysqlDecimalType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_DECIMAL_UNSIGNED";
                }

                @Override
                protected MysqlDecimalType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    if (config.getScale() != null && config.getPrecision() == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR, "Param settings is illegal for decimal");
                    }
                    int scale = config.getScale() == null ? 0 : config.getScale();
                    int precision = config.getPrecision() == null ? 10 : config.getPrecision();
                    BigDecimal defaultValue = config.getDefaultValue() == null ? null
                            : new BigDecimal(config.getDefaultValue().toString());
                    MysqlDecimalType returnValue = new MysqlDecimalType(precision, scale, generator, defaultValue,
                            config.getAllowNull(), false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The decimal type in mysql mode
     */
    private static final DataTypeFactory<MysqlDecimalType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_DECIMAL =
            new DataTypeFactory<MysqlDecimalType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_DECIMAL";
                }

                @Override
                protected MysqlDecimalType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    if (config.getScale() != null && config.getPrecision() == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR, "Param settings is illegal for decimal");
                    }
                    int scale = config.getScale() == null ? 0 : config.getScale();
                    int precision = config.getPrecision() == null ? 10 : config.getPrecision();
                    BigDecimal defaultValue = config.getDefaultValue() == null ? null
                            : new BigDecimal(config.getDefaultValue().toString());
                    MysqlDecimalType returnValue = new MysqlDecimalType(precision, scale, generator, defaultValue,
                            config.getAllowNull(), true);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The bigint unsigned type in mysql mode
     */
    private static final DataTypeFactory<MysqlBigIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_BIGINT_UNSIGNED =
            new DataTypeFactory<MysqlBigIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_BIGINT_UNSIGNED";
                }

                @Override
                protected MysqlBigIntType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    BigDecimal defaultValue = null;
                    if (config.getDefaultValue() != null) {
                        defaultValue = new BigDecimal(config.getDefaultValue().toString());
                    }
                    MysqlBigIntType returnValue =
                            new MysqlBigIntType(generator, defaultValue, config.getAllowNull(), false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The int type in mysql mode
     */
    private static final DataTypeFactory<MysqlBigIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_BIGINT =
            new DataTypeFactory<MysqlBigIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_BIGINT";
                }

                @Override
                protected MysqlBigIntType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    BigDecimal defaultValue = null;
                    if (config.getDefaultValue() != null) {
                        defaultValue = new BigDecimal(config.getDefaultValue().toString());
                    }
                    MysqlBigIntType returnValue =
                            new MysqlBigIntType(generator, defaultValue, config.getAllowNull(), true);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The int unsigned type in mysql mode
     */
    private static final DataTypeFactory<MysqlIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_INT_UNSIGNED =
            new DataTypeFactory<MysqlIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_INT_UNSIGNED";
                }

                @Override
                protected MysqlIntType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    BigDecimal defaultValue = null;
                    if (config.getDefaultValue() != null) {
                        defaultValue = new BigDecimal(config.getDefaultValue().toString());
                    }
                    MysqlIntType returnValue = new MysqlIntType(generator, defaultValue, config.getAllowNull(), false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The int type in mysql mode
     */
    private static final DataTypeFactory<MysqlIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_INT =
            new DataTypeFactory<MysqlIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_INT";
                }

                @Override
                protected MysqlIntType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    BigDecimal defaultValue = null;
                    if (config.getDefaultValue() != null) {
                        defaultValue = new BigDecimal(config.getDefaultValue().toString());
                    }
                    MysqlIntType returnValue = new MysqlIntType(generator, defaultValue, config.getAllowNull(), true);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The mediumInt unsigned type in mysql mode
     */
    private static final DataTypeFactory<MysqlMediumIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_MEDIUMINT_UNSIGNED =
            new DataTypeFactory<MysqlMediumIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_MEDIUMINT_UNSIGNED";
                }

                @Override
                protected MysqlMediumIntType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    BigDecimal defaultValue = null;
                    if (config.getDefaultValue() != null) {
                        defaultValue = new BigDecimal(config.getDefaultValue().toString());
                    }
                    MysqlMediumIntType returnValue =
                            new MysqlMediumIntType(generator, defaultValue, config.getAllowNull(), false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The medium int type in mysql mode
     */
    private static final DataTypeFactory<MysqlMediumIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_MEDIUMINT =
            new DataTypeFactory<MysqlMediumIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_MEDIUMINT";
                }

                @Override
                protected MysqlMediumIntType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    BigDecimal defaultValue = null;
                    if (config.getDefaultValue() != null) {
                        defaultValue = new BigDecimal(config.getDefaultValue().toString());
                    }
                    MysqlMediumIntType returnValue =
                            new MysqlMediumIntType(generator, defaultValue, config.getAllowNull(), true);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The smallint unsigned type in mysql mode
     */
    private static final DataTypeFactory<MysqlSmallIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_SMALLINT_UNSIGNED =
            new DataTypeFactory<MysqlSmallIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_SMALLINT_UNSIGNED";
                }

                @Override
                protected MysqlSmallIntType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    BigDecimal defaultValue = null;
                    if (config.getDefaultValue() != null) {
                        defaultValue = new BigDecimal(config.getDefaultValue().toString());
                    }
                    MysqlSmallIntType returnValue =
                            new MysqlSmallIntType(generator, defaultValue, config.getAllowNull(), false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The smallint type in mysql mode
     */
    private static final DataTypeFactory<MysqlSmallIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_SMALLINT =
            new DataTypeFactory<MysqlSmallIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_SMALLINT";
                }

                @Override
                protected MysqlSmallIntType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    BigDecimal defaultValue = null;
                    if (config.getDefaultValue() != null) {
                        defaultValue = new BigDecimal(config.getDefaultValue().toString());
                    }
                    MysqlSmallIntType returnValue =
                            new MysqlSmallIntType(generator, defaultValue, config.getAllowNull(), true);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The tinyint unsigned type in mysql mode
     */
    private static final DataTypeFactory<MysqlTinyIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_TINYINT_UNSIGNED =
            new DataTypeFactory<MysqlTinyIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_TINYINT_UNSIGNED";
                }

                @Override
                protected MysqlTinyIntType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    BigDecimal defaultValue = null;
                    if (config.getDefaultValue() != null) {
                        defaultValue = new BigDecimal(config.getDefaultValue().toString());
                    }
                    MysqlTinyIntType returnValue =
                            new MysqlTinyIntType(generator, defaultValue, config.getAllowNull(), false);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The tinyint type in mysql mode
     */
    private static final DataTypeFactory<MysqlTinyIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_MYSQL_TINYINT =
            new DataTypeFactory<MysqlTinyIntType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_MYSQL_TINYINT";
                }

                @Override
                protected MysqlTinyIntType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    BigDecimal defaultValue = null;
                    if (config.getDefaultValue() != null) {
                        defaultValue = new BigDecimal(config.getDefaultValue().toString());
                    }
                    MysqlTinyIntType returnValue =
                            new MysqlTinyIntType(generator, defaultValue, config.getAllowNull(), true);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The raw type in oracle mode
     */
    private static final DataTypeFactory<OracleRawType, CharDataTypeConfig, BaseByteGenerator> OB_ORACLE_RAW =
            new DataTypeFactory<OracleRawType, CharDataTypeConfig, BaseByteGenerator>() {
                @Override
                public String name() {
                    return "OB_ORACLE_RAW";
                }

                @Override
                protected OracleRawType newInstance(CharDataTypeConfig config, BaseByteGenerator generator) {
                    OracleRawType returnValue =
                            new OracleRawType(null, config.getAllowNull(), config.getWidth(), generator);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The clob type in oracle mode
     */
    private static final DataTypeFactory<OracleBlobType, CharDataTypeConfig, BaseByteGenerator> OB_ORACLE_CLOB =
            new DataTypeFactory<OracleBlobType, CharDataTypeConfig, BaseByteGenerator>() {
                @Override
                public String name() {
                    return "OB_ORACLE_CLOB";
                }

                @Override
                protected OracleBlobType newInstance(CharDataTypeConfig config, BaseByteGenerator generator) {
                    OracleBlobType returnValue = new OracleBlobType(null, config.getAllowNull(), generator);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The blob type in oracle mode
     */
    private static final DataTypeFactory<OracleBlobType, CharDataTypeConfig, BaseByteGenerator> OB_ORACLE_BLOB =
            new DataTypeFactory<OracleBlobType, CharDataTypeConfig, BaseByteGenerator>() {
                @Override
                public String name() {
                    return "OB_ORACLE_BLOB";
                }

                @Override
                protected OracleBlobType newInstance(CharDataTypeConfig config, BaseByteGenerator generator) {
                    OracleBlobType returnValue = new OracleBlobType(null, config.getAllowNull(), generator);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * timstamp with local time zone type in oracle mode
     */
    private static final DataTypeFactory<OracleTimestampType, DateDataTypeConfig, BaseDateGenerator<Timestamp>> OB_ORACLE_TIMESTAMP_WITH_LOCAL_TIME_ZONE =
            new DataTypeFactory<OracleTimestampType, DateDataTypeConfig, BaseDateGenerator<Timestamp>>() {

                @Override
                public String name() {
                    return "OB_ORACLE_TIMESTAMP_WITH_LOCAL_TIME_ZONE";
                }

                @Override
                protected OracleTimestampType newInstance(DateDataTypeConfig config,
                        BaseDateGenerator<Timestamp> generator) {
                    int scale = 3;
                    if (config.getScale() != null) {
                        scale = config.getScale();
                    }
                    Timestamp defaultValue = null;
                    Object val = config.getDefaultValue();
                    if (val != null) {
                        defaultValue = new Timestamp(Long.parseLong(val.toString()));
                    }
                    OracleTimestampType returnValue =
                            new OracleTimestampType(generator, scale, defaultValue, config.getAllowNull());
                    if (config.getTimezone() != null) {
                        returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
                    }
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new Timestamp(Long.parseLong(config.getLowValue().toString())));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new Timestamp(Long.parseLong(config.getHighValue().toString())));
                    }
                    return returnValue;
                }
            };
    /**
     * timstamp with time zone type in oracle mode
     */
    private static final DataTypeFactory<OracleTimestampType, DateDataTypeConfig, BaseDateGenerator<Timestamp>> OB_ORACLE_TIMESTAMP_WITH_TIME_ZONE =
            new DataTypeFactory<OracleTimestampType, DateDataTypeConfig, BaseDateGenerator<Timestamp>>() {

                @Override
                public String name() {
                    return "OB_ORACLE_TIMESTAMP_WITH_TIME_ZONE";
                }

                @Override
                protected OracleTimestampType newInstance(DateDataTypeConfig config,
                        BaseDateGenerator<Timestamp> generator) {
                    int scale = 3;
                    if (config.getScale() != null) {
                        scale = config.getScale();
                    }
                    Timestamp defaultValue = null;
                    Object val = config.getDefaultValue();
                    if (val != null) {
                        defaultValue = new Timestamp(Long.parseLong(val.toString()));
                    }
                    OracleTimestampType returnValue =
                            new OracleTimestampType(generator, scale, defaultValue, config.getAllowNull());
                    if (config.getTimezone() != null) {
                        returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
                    }
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new Timestamp(Long.parseLong(config.getLowValue().toString())));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new Timestamp(Long.parseLong(config.getHighValue().toString())));
                    }
                    return returnValue;
                }
            };
    /**
     * The timestamp type in oracle mode
     */
    private static final DataTypeFactory<OracleTimestampType, DateDataTypeConfig, BaseDateGenerator<Timestamp>> OB_ORACLE_TIMESTAMP =
            new DataTypeFactory<OracleTimestampType, DateDataTypeConfig, BaseDateGenerator<Timestamp>>() {

                @Override
                public String name() {
                    return "OB_ORACLE_TIMESTAMP";
                }

                @Override
                protected OracleTimestampType newInstance(DateDataTypeConfig config,
                        BaseDateGenerator<Timestamp> generator) {
                    int scale = config.getScale() == null ? 3 : config.getScale();
                    Timestamp defaultValue = null;
                    Object val = config.getDefaultValue();
                    if (val != null) {
                        defaultValue = new Timestamp(Long.parseLong(val.toString()));
                    }
                    OracleTimestampType returnValue =
                            new OracleTimestampType(generator, scale, defaultValue, config.getAllowNull());
                    if (config.getTimezone() != null) {
                        returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
                    }
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new Timestamp(Long.parseLong(config.getLowValue().toString())));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new Timestamp(Long.parseLong(config.getHighValue().toString())));
                    }
                    return returnValue;
                }
            };
    /**
     * The date type in oracle mode
     */
    private static final DataTypeFactory<OracleDateType, DateDataTypeConfig, BaseDateGenerator<Date>> OB_ORACLE_DATE =
            new DataTypeFactory<OracleDateType, DateDataTypeConfig, BaseDateGenerator<Date>>() {

                @Override
                public String name() {
                    return "OB_ORACLE_DATE";
                }

                @Override
                protected OracleDateType newInstance(DateDataTypeConfig config, BaseDateGenerator<Date> generator) {
                    Date defaultValue = null;
                    Object val = config.getDefaultValue();
                    if (val != null) {
                        defaultValue = new Date(Long.parseLong(val.toString()));
                    }
                    OracleDateType returnValue = new OracleDateType(generator, defaultValue, config.getAllowNull());
                    if (config.getTimezone() != null) {
                        returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
                    }
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new Date(Long.parseLong(config.getLowValue().toString())));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new Date(Long.parseLong(config.getHighValue().toString())));
                    }
                    return returnValue;
                }
            };
    /**
     * interval year to month type in oracle mode
     */
    private static final DataTypeFactory<OracleIntervalYMType, DateDataTypeConfig, BaseGenerator<Integer, INTERVALYM>> OB_ORACLE_INTERVAL_YEAR_TO_MONTH =
            new DataTypeFactory<OracleIntervalYMType, DateDataTypeConfig, BaseGenerator<Integer, INTERVALYM>>() {

                @Override
                public String name() {
                    return "OB_ORACLE_INTERVAL_YEAR_TO_MONTH";
                }

                @Override
                protected OracleIntervalYMType newInstance(DateDataTypeConfig config,
                        BaseGenerator<Integer, INTERVALYM> generator) {
                    INTERVALYM defaultValue = null;
                    Object val = config.getDefaultValue();
                    if (val != null) {
                        defaultValue = new INTERVALYM(val.toString());
                    }
                    OracleIntervalYMType returnValue =
                            new OracleIntervalYMType(generator, config.getScale(), defaultValue, config.getAllowNull());
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(Integer.valueOf(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(Integer.valueOf(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * interval year to month type in oracle mode
     */
    private static final DataTypeFactory<OracleIntervalYMType, DateDataTypeConfig, BaseGenerator<Integer, INTERVALYM>> OB_ORACLE_INTERVAL_DAY_TO_SECOND =
            new DataTypeFactory<OracleIntervalYMType, DateDataTypeConfig, BaseGenerator<Integer, INTERVALYM>>() {

                @Override
                public String name() {
                    return "OB_ORACLE_INTERVAL_DAY_TO_SECOND";
                }

                @Override
                protected OracleIntervalYMType newInstance(DateDataTypeConfig config,
                        BaseGenerator<Integer, INTERVALYM> generator) {
                    INTERVALYM defaultValue = null;
                    Object val = config.getDefaultValue();
                    if (val != null) {
                        defaultValue = new INTERVALYM(val.toString());
                    }
                    OracleIntervalYMType returnValue =
                            new OracleIntervalYMType(generator, config.getScale(), defaultValue, config.getAllowNull());
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(Integer.valueOf(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(Integer.valueOf(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * The nvarchar type in oracle mode
     */
    private static final DataTypeFactory<OracleNvarCharType, CharDataTypeConfig, BaseCharGenerator> OB_ORACLE_NVARCHAR =
            new DataTypeFactory<OracleNvarCharType, CharDataTypeConfig, BaseCharGenerator>() {

                @Override
                public String name() {
                    return "OB_ORACLE_NVARCHAR";
                }

                @Override
                protected OracleNvarCharType newInstance(CharDataTypeConfig config, BaseCharGenerator generator) {
                    String charset = config.getCharset();
                    Validate.notNull(charset, "Charset can not be null for nvarchar");
                    Integer length = config.getWidth();
                    Validate.notNull(length, "Length can not be null for nvarchar2");
                    CharsetType charsetType = CharsetType.valueOf(charset);
                    OracleNvarCharType returnValue = new OracleNvarCharType(generator, length,
                            (String) config.getDefaultValue(), config.getAllowNull(), charsetType);
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The varchar2 type in oracle mode
     */
    private static final DataTypeFactory<OracleVarCharType, CharDataTypeConfig, BaseCharGenerator> OB_ORACLE_VARCHAR2 =
            new DataTypeFactory<OracleVarCharType, CharDataTypeConfig, BaseCharGenerator>() {
                @Override
                public String name() {
                    return "OB_ORACLE_VARCHAR2";
                }

                @Override
                protected OracleVarCharType newInstance(CharDataTypeConfig config, BaseCharGenerator generator) {
                    String charset = config.getCharset();
                    Validate.notNull(charset, "Charset can not be null for varchar2");
                    Integer length = config.getWidth();
                    Validate.notNull(length, "Length can not be null for varchar2");
                    CharsetType charsetType = CharsetType.valueOf(charset);
                    OracleVarCharType returnValue = new OracleVarCharType(generator, length,
                            (String) config.getDefaultValue(), config.getAllowNull(), charsetType, config.isUnicode());
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The varchar type in oracle mode
     */
    private static final DataTypeFactory<OracleVarCharType, CharDataTypeConfig, BaseCharGenerator> OB_ORACLE_VARCHAR =
            new DataTypeFactory<OracleVarCharType, CharDataTypeConfig, BaseCharGenerator>() {
                @Override
                public String name() {
                    return "OB_ORACLE_VARCHAR";
                }

                @Override
                protected OracleVarCharType newInstance(CharDataTypeConfig config, BaseCharGenerator generator) {
                    String charset = config.getCharset();
                    Validate.notNull(charset, "Charset can not be null for varchar");
                    Integer length = config.getWidth();
                    Validate.notNull(length, "Length can not be null for varchar");
                    CharsetType charsetType = CharsetType.valueOf(charset);
                    OracleVarCharType returnValue = new OracleVarCharType(generator, length,
                            (String) config.getDefaultValue(), config.getAllowNull(), charsetType, config.isUnicode());
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The char type in oracle mode
     */
    private static final DataTypeFactory<OracleCharType, CharDataTypeConfig, BaseCharGenerator> OB_ORACLE_CHAR =
            new DataTypeFactory<OracleCharType, CharDataTypeConfig, BaseCharGenerator>() {

                @Override
                public String name() {
                    return "OB_ORACLE_CHAR";
                }

                @Override
                protected OracleCharType newInstance(CharDataTypeConfig config, BaseCharGenerator generator) {
                    String charset = config.getCharset();
                    Validate.notNull(charset, "Charset can not be null for char");
                    Integer length = config.getWidth();
                    Validate.notNull(length, "Length can not be null for char");
                    CharsetType charsetType = CharsetType.valueOf(charset);
                    OracleCharType returnValue = new OracleCharType(length, (String) config.getDefaultValue(),
                            config.getAllowNull(), charsetType, generator, config.isUnicode());
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue((Integer) config.getLowValue());
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue((Integer) config.getHighValue());
                    }
                    return returnValue;
                }
            };
    /**
     * The number type in oracle mode
     */
    private static final DataTypeFactory<OracleNumberType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>> OB_ORACLE_NUMBER =
            new DataTypeFactory<OracleNumberType, DigitDataTypeConfig, BaseDigitalGenerator<BigDecimal>>() {
                @Override
                public String name() {
                    return "OB_ORACLE_NUMBER";
                }

                @Override
                protected OracleNumberType newInstance(DigitDataTypeConfig config,
                        BaseDigitalGenerator<BigDecimal> generator) {
                    int scale = config.getScale() == null ? 0 : config.getScale();
                    Integer precision = config.getPrecision();
                    Validate.notNull(precision, "Precision can not be null for number");
                    BigDecimal defaultValue = null;
                    if (config.getDefaultValue() != null) {
                        defaultValue = new BigDecimal(config.getDefaultValue().toString());
                    }
                    OracleNumberType returnValue =
                            new OracleNumberType(precision, scale, generator, defaultValue, config.getAllowNull());
                    if (config.getLowValue() != null) {
                        returnValue.setLowValue(new BigDecimal(config.getLowValue().toString()));
                    }
                    if (config.getHighValue() != null) {
                        returnValue.setHighValue(new BigDecimal(config.getHighValue().toString()));
                    }
                    return returnValue;
                }
            };
    /**
     * Instance mapping table
     */
    private static final Map<String, DataTypeFactory<? extends AbstractDataType<?, ? extends Comparable<?>>, ? extends DataTypeConfig, ? extends BaseGenerator<? extends Comparable<?>, ?>>> FACTORYNAME_2_FACTORYINSTANCE =
            new HashMap<>();

    static {
        try {
            Field[] fields = DataTypeFactory.class.getDeclaredFields();
            for (Field field : fields) {
                Object instance = field.get(DataTypeFactory.class);
                if (Modifier.isStatic(field.getModifiers()) && instance instanceof DataTypeFactory) {
                    FACTORYNAME_2_FACTORYINSTANCE.putIfAbsent(field.getName(), (DataTypeFactory) instance);
                }
            }
        } catch (IllegalAccessException e) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, e.getMessage());
        }
    }

    /**
     * Get the name of the type factory class
     *
     * @return Return name
     */
    abstract public String name();

    /**
     * Public method, through which a column config object is converted into a specific column object
     *
     * @param config Column configuration object
     * @return Return column object
     */
    public T make(V config) {
        try {
            Validate.notNull(config, "Config can not be null for DataTypeFactory#make");
            Validate.notNull(config.getGenerator(), "DataGeneratorName can not be null for DataTypeFactory#make");
            GeneratorFactory<? extends BaseGenerator<? extends Comparable<?>, ?>> factory =
                    GeneratorFactory.getInstance(config.getGenerator());
            K generator = (K) factory.make(config.getGenParams());
            if (generator == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        "Unknown Data generator \"" + config.getGenerator() + "\"");
            }
            return newInstance(config, generator);
        } catch (Exception e) {
            throw new MockerException(MockerError.PARAMETER_ERROR, e.getMessage());
        }
    }

    /**
     * The internal construction object method is used to instantiate a data generator from the data
     * generator constructor inside the column configuration object
     *
     * @param config Column configuration object
     * @param generator Data generator object passed from upstream
     * @return Return column object
     */
    abstract protected T newInstance(V config, K generator);

    public static <T extends AbstractDataType<?, ? extends Comparable<?>>, V extends DataTypeConfig, K extends BaseGenerator<? extends Comparable<?>, ?>> DataTypeFactory<T, V, K> getInstance(
            String factoryName) {
        DataTypeFactory<T, V, K> returnVal = (DataTypeFactory<T, V, K>) FACTORYNAME_2_FACTORYINSTANCE.get(factoryName);
        if (returnVal == null) {
            throw new MockerException(MockerError.UNKNOWN_DATA_TYPE);
        }
        return returnVal;
    }

    public static List<DataTypeFactory<? extends AbstractDataType<?, ? extends Comparable<?>>, ? extends DataTypeConfig, ? extends BaseGenerator<? extends Comparable<?>, ?>>> listInstances() {
        List<DataTypeFactory<? extends AbstractDataType<?, ? extends Comparable<?>>, ? extends DataTypeConfig, ? extends BaseGenerator<? extends Comparable<?>, ?>>> returnVal =
                new ArrayList<>();
        Set<Map.Entry<String, DataTypeFactory<? extends AbstractDataType<?, ? extends Comparable<?>>, ? extends DataTypeConfig, ? extends BaseGenerator<? extends Comparable<?>, ?>>>> entries =
                FACTORYNAME_2_FACTORYINSTANCE.entrySet();
        for (Map.Entry<String, DataTypeFactory<? extends AbstractDataType<?, ? extends Comparable<?>>, ? extends DataTypeConfig, ? extends BaseGenerator<? extends Comparable<?>, ?>>> entry : entries) {
            returnVal.add(entry.getValue());
        }
        return returnVal;
    }

}
