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

import javax.xml.datatype.DatatypeFactory;

import com.alipay.oceanbase.jdbc.extend.datatype.INTERVALYM;

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
import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.generator.ByteGeneratorBase;
import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.generator.DateGeneratorBase;
import com.oceanbase.tools.datamocker.generator.DigitalGeneratorBase;
import com.oceanbase.tools.datamocker.generator.GeneratorFactory;
import com.oceanbase.tools.datamocker.model.config.model.CharDataTypeConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataTypeConfig;
import com.oceanbase.tools.datamocker.model.config.model.DateDataTypeConfig;
import com.oceanbase.tools.datamocker.model.config.model.DigitDataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 抽象数据类型类，用于封装一些基础的数据类型逻辑
 *
 * @author yh263208
 * @date 2020-12-10 15:42
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class DataTypeFactory<T extends AbstractDataType, V extends DataTypeConfig, K extends BaseGenerator> {
    /**
     * mysql模式下的year类型
     */
    private static final DataTypeFactory OB_MYSQL_YEAR = new DataTypeFactory<MysqlYearType, DateDataTypeConfig, DateGeneratorBase>() {

        @Override
        public String name() {
            return "OB_MYSQL_YEAR";
        }

        @Override
        protected MysqlYearType newInstance(DateDataTypeConfig config, DateGeneratorBase generator) {
            int scale = config.getScale() == null ? 3 : config.getScale();
            Date defaultValue = null;
            Object val = config.getDefaultValue();
            if (val != null) {
                defaultValue = new Date(Long.valueOf(val.toString()));
            }
            MysqlYearType returnValue = new MysqlYearType(generator, scale, defaultValue, config.getAllowNull());
            if (config.getTimezone() != null) {
                returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
            }
            if (config.getLowValue() != null) {
                returnValue.setLowValue(new Date(Long.valueOf(config.getLowValue().toString())));
            }
            if (config.getHighValue() != null) {
                returnValue.setHighValue(new Date(Long.valueOf(config.getHighValue().toString())));
            }
            return returnValue;
        }
    };

    /**
     * mysql模式下的datetime类型
     */
    private static final DataTypeFactory OB_MYSQL_DATETIME
            = new DataTypeFactory<MysqlDateTimeType, DateDataTypeConfig, DateGeneratorBase>() {

        @Override
        public String name() {
            return "OB_MYSQL_DATETIME";
        }

        @Override
        protected MysqlDateTimeType newInstance(DateDataTypeConfig config, DateGeneratorBase generator) {
            int scale = config.getScale() == null ? 0 : config.getScale();
            Timestamp defaultValue = null;
            Object val = config.getDefaultValue();
            if (val != null) {
                defaultValue = new Timestamp(Long.valueOf(val.toString()));
            }
            MysqlDateTimeType returnValue = new MysqlDateTimeType(generator, scale, defaultValue, config.getAllowNull());
            if (config.getTimezone() != null) {
                returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
            }
            if (config.getLowValue() != null) {
                returnValue.setLowValue(new Timestamp(Long.valueOf(config.getLowValue().toString())));
            }
            if (config.getHighValue() != null) {
                returnValue.setHighValue(new Timestamp(Long.valueOf(config.getHighValue().toString())));
            }
            return returnValue;
        }
    };

    /**
     * mysql模式下的time类型
     */
    private static final DataTypeFactory OB_MYSQL_TIME = new DataTypeFactory<MysqlTimeType, DateDataTypeConfig, DateGeneratorBase>() {

        @Override
        public String name() {
            return "OB_MYSQL_TIME";
        }

        @Override
        protected MysqlTimeType newInstance(DateDataTypeConfig config, DateGeneratorBase generator) {
            int scale = config.getScale() == null ? 3 : config.getScale();
            Timestamp defaultValue = null;
            Object val = config.getDefaultValue();
            if (val != null) {
                defaultValue = new Timestamp(Long.valueOf(val.toString()));
            }
            MysqlTimeType returnValue = new MysqlTimeType(generator, scale, defaultValue, config.getAllowNull());
            if (config.getTimezone() != null) {
                returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
            }
            if (config.getLowValue() != null) {
                returnValue.setLowValue(new Timestamp(Long.valueOf(config.getLowValue().toString())));
            }
            if (config.getHighValue() != null) {
                returnValue.setHighValue(new Timestamp(Long.valueOf(config.getHighValue().toString())));
            }
            return returnValue;
        }
    };

    /**
     * mysql模式下的timstamp类型
     */
    private static final DataTypeFactory OB_MYSQL_TIMESTAMP
            = new DataTypeFactory<MysqlTimestampType, DateDataTypeConfig, DateGeneratorBase>() {

        @Override
        public String name() {
            return "OB_MYSQL_TIMESTAMP";
        }

        @Override
        protected MysqlTimestampType newInstance(DateDataTypeConfig config, DateGeneratorBase generator) {
            int scale = config.getScale() == null ? 3 : config.getScale();
            Timestamp defaultValue = null;
            Object val = config.getDefaultValue();
            if (val != null) {
                defaultValue = new Timestamp(Long.valueOf(val.toString()));
            }
            MysqlTimestampType returnValue = new MysqlTimestampType(generator, scale, defaultValue, config.getAllowNull());
            if (config.getTimezone() != null) {
                returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
            }
            if (config.getLowValue() != null) {
                returnValue.setLowValue(new Timestamp(Long.valueOf(config.getLowValue().toString())));
            }
            if (config.getHighValue() != null) {
                returnValue.setHighValue(new Timestamp(Long.valueOf(config.getHighValue().toString())));
            }
            return returnValue;
        }
    };

    /**
     * mysql模式下的date类型
     */
    private static final DataTypeFactory OB_MYSQL_DATE = new DataTypeFactory<MysqlDateType, DateDataTypeConfig, DateGeneratorBase>() {

        @Override
        public String name() {
            return "OB_MYSQL_DATE";
        }

        @Override
        protected MysqlDateType newInstance(DateDataTypeConfig config, DateGeneratorBase generator) {
            Date defaultValue = null;
            Object val = config.getDefaultValue();
            if (val != null) {
                defaultValue = new Date(Long.valueOf(val.toString()));
            }
            MysqlDateType returnValue = new MysqlDateType(generator, defaultValue, config.getAllowNull());
            if (config.getTimezone() != null) {
                returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
            }
            if (config.getLowValue() != null) {
                returnValue.setLowValue(new Date(Long.valueOf(config.getLowValue().toString())));
            }
            if (config.getHighValue() != null) {
                returnValue.setHighValue(new Date(Long.valueOf(config.getHighValue().toString())));
            }
            return returnValue;
        }
    };

    /**
     * mysql模式下的varbinary类型
     */
    private static final DataTypeFactory OB_MYSQL_VARBINARY
            = new DataTypeFactory<MysqlBinaryType, CharDataTypeConfig, ByteGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_VARBINARY";
        }

        @Override
        protected MysqlBinaryType newInstance(CharDataTypeConfig config, ByteGeneratorBase generator) {
            if (config.getWidth() == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "width for varbinary can not be null");
            }
            if (config.getWidth() > 1048576) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        String.format("width for varbinary is too big (max = %d)", config.getWidth()));
            }
            MysqlBinaryType returnValue = new MysqlBinaryType(null, config.getAllowNull(), config.getWidth(), generator);
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
     * mysql模式下的bit类型
     */
    private static final DataTypeFactory OB_MYSQL_BIT = new DataTypeFactory<MysqlBinaryType, CharDataTypeConfig, ByteGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_BIT";
        }

        @Override
        protected MysqlBinaryType newInstance(CharDataTypeConfig config, ByteGeneratorBase generator) {
            if (config.getWidth() == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "width for bit can not be null");
            }
            if (config.getWidth() > 64) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        String.format("width for bit is too big (max = %d)", config.getWidth()));
            }
            int byteWidth = config.getWidth() / 8;
            if (byteWidth <= 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "byte width can not be equal to or smaller than zero");
            }
            MysqlBinaryType returnValue = new MysqlBinaryType(null, config.getAllowNull(), byteWidth, generator);
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
     * mysql模式下的binary类型
     */
    private static final DataTypeFactory OB_MYSQL_BINARY = new DataTypeFactory<MysqlBinaryType, CharDataTypeConfig, ByteGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_BINARY";
        }

        @Override
        protected MysqlBinaryType newInstance(CharDataTypeConfig config, ByteGeneratorBase generator) {
            if (config.getWidth() == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "width for binary can not be null");
            }
            if (config.getWidth() > 256) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        String.format("width for binary is too big (max = %d)", config.getWidth()));
            }
            MysqlBinaryType returnValue = new MysqlBinaryType(null, config.getAllowNull(), config.getWidth(), generator);
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
     * mysql模式下的longblob类型
     */
    private static final DataTypeFactory OB_MYSQL_LONGBLOB = new DataTypeFactory<MysqlBlobType, CharDataTypeConfig, ByteGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_LONGBLOB";
        }

        @Override
        protected MysqlBlobType newInstance(CharDataTypeConfig config, ByteGeneratorBase generator) {
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
     * mysql模式下的mediumblob类型
     */
    private static final DataTypeFactory OB_MYSQL_MEDIUMBLOB = new DataTypeFactory<MysqlBlobType, CharDataTypeConfig, ByteGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_MEDIUMBLOB";
        }

        @Override
        protected MysqlBlobType newInstance(CharDataTypeConfig config, ByteGeneratorBase generator) {
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
     * mysql模式下的blob类型
     */
    private static final DataTypeFactory OB_MYSQL_BLOB = new DataTypeFactory<MysqlBlobType, CharDataTypeConfig, ByteGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_BLOB";
        }

        @Override
        protected MysqlBlobType newInstance(CharDataTypeConfig config, ByteGeneratorBase generator) {
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
     * mysql模式下的tinyblob类型
     */
    private static final DataTypeFactory OB_MYSQL_TINYBLOB = new DataTypeFactory<MysqlBlobType, CharDataTypeConfig, ByteGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_TINYBLOB";
        }

        @Override
        protected MysqlBlobType newInstance(CharDataTypeConfig config, ByteGeneratorBase generator) {
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
     * mysql模式下的LONGTEXT类型
     */
    private static final DataTypeFactory OB_MYSQL_LONGTEXT = new DataTypeFactory<MysqlTextType, CharDataTypeConfig, CharGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_LONGTEXT";
        }

        @Override
        protected MysqlTextType newInstance(CharDataTypeConfig config, CharGeneratorBase generator) {
            String charset = config.getCharset();
            if (charset == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "error parameters for longtext");
            }
            CharsetType charsetType = CharsetType.valueOf(charset);
            MysqlTextType returnValue = new MysqlTextType(4096, (String) config.getDefaultValue(), config.getAllowNull(),
                    charsetType, generator, false);
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
     * mysql模式下的MEDIUMTEXT类型
     */
    private static final DataTypeFactory OB_MYSQL_MEDIUMTEXT = new DataTypeFactory<MysqlTextType, CharDataTypeConfig, CharGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_MEDIUMTEXT";
        }

        @Override
        protected MysqlTextType newInstance(CharDataTypeConfig config, CharGeneratorBase generator) {
            String charset = config.getCharset();
            if (charset == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "error parametes for mediumtext");
            }
            CharsetType charsetType = CharsetType.valueOf(charset);
            MysqlTextType returnValue = new MysqlTextType(4096, (String) config.getDefaultValue(), config.getAllowNull(),
                    charsetType, generator, false);
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
     * mysql模式下的TEXT类型
     */
    private static final DataTypeFactory OB_MYSQL_TEXT = new DataTypeFactory<MysqlTextType, CharDataTypeConfig, CharGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_TEXT";
        }

        @Override
        protected MysqlTextType newInstance(CharDataTypeConfig config, CharGeneratorBase generator) {
            String charset = config.getCharset();
            if (charset == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "error parametes for text");
            }
            CharsetType charsetType = CharsetType.valueOf(charset);
            MysqlTextType returnValue = new MysqlTextType(4096, (String) config.getDefaultValue(), config.getAllowNull(),
                    charsetType, generator, false);
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
     * mysql模式下的TINYTEXT类型
     */
    private static final DataTypeFactory OB_MYSQL_TINYTEXT = new DataTypeFactory<MysqlTextType, CharDataTypeConfig, CharGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_TINYTEXT";
        }

        @Override
        protected MysqlTextType newInstance(CharDataTypeConfig config, CharGeneratorBase generator) {
            String charset = config.getCharset();
            if (charset == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "error parametes for tinytext");
            }
            CharsetType charsetType = CharsetType.valueOf(charset);
            MysqlTextType returnValue = new MysqlTextType(255, (String) config.getDefaultValue(), config.getAllowNull(),
                    charsetType, generator, false);
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
     * mysql模式下的varchar类型
     */
    private static final DataTypeFactory OB_MYSQL_VARCHAR = new DataTypeFactory<MysqlVarCharType, CharDataTypeConfig, CharGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_VARCHAR";
        }

        @Override
        protected MysqlVarCharType newInstance(CharDataTypeConfig config, CharGeneratorBase generator) {
            String charset = config.getCharset();
            Integer length = config.getWidth();
            if (length == null || charset == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "error parametes for VARCHAR");
            }
            CharsetType charsetType = CharsetType.valueOf(charset);
            MysqlVarCharType returnValue = new MysqlVarCharType(length, (String) config.getDefaultValue(), config.getAllowNull(),
                    charsetType, generator, false);
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
     * mysql模式下的char类型
     */
    private static final DataTypeFactory OB_MYSQL_CHAR = new DataTypeFactory<MysqlCharType, CharDataTypeConfig, CharGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_CHAR";
        }

        @Override
        protected MysqlCharType newInstance(CharDataTypeConfig config, CharGeneratorBase generator) {
            String charset = config.getCharset();
            Integer length = config.getWidth();
            if (length == null || charset == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "error parametes for CHAR");
            }
            CharsetType charsetType = CharsetType.valueOf(charset);
            MysqlCharType returnValue = new MysqlCharType(length, (String) config.getDefaultValue(), config.getAllowNull(),
                    charsetType, generator, false);
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
     * mysql模式下的double unsigned类型
     */
    private static final DataTypeFactory OB_MYSQL_DOUBLE_UNSIGNED
            = new DataTypeFactory<MysqlFloatType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_DOUBLE_UNSIGNED";
        }

        @Override
        protected MysqlFloatType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            if (config.getScale() != null && config.getPrecision() == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "param settings is illegal for double");
            }
            Integer scale = config.getScale() == null ? -1 : config.getScale();
            Integer precision = config.getPrecision() == null ? -1 : config.getPrecision();
            if (scale == -1 && precision != -1) {
                precision = -1;
            }
            BigDecimal defaultValue = config.getDefaultValue() == null ? null : new BigDecimal(config.getDefaultValue().toString());
            MysqlFloatType returnValue = new MysqlFloatType(precision, scale, generator, defaultValue, config.getAllowNull(), false);
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
     * mysql模式下的double类型
     */
    private static final DataTypeFactory OB_MYSQL_DOUBLE
            = new DataTypeFactory<MysqlFloatType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_DOUBLE";
        }

        @Override
        protected MysqlFloatType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            if (config.getScale() != null && config.getPrecision() == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "param settings is illegal for double");
            }
            Integer scale = config.getScale() == null ? -1 : config.getScale();
            Integer precision = config.getPrecision() == null ? -1 : config.getPrecision();
            if (scale == -1 && precision != -1) {
                precision = -1;
            }
            BigDecimal defaultValue = config.getDefaultValue() == null ? null : new BigDecimal(config.getDefaultValue().toString());
            MysqlFloatType returnValue = new MysqlFloatType(precision, scale, generator, defaultValue, config.getAllowNull(), true);
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
     * mysql模式下的float unsigned类型
     */
    private static final DataTypeFactory OB_MYSQL_FLOAT_UNSIGNED
            = new DataTypeFactory<MysqlFloatType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_FLOAT_UNSIGNED";
        }

        @Override
        protected MysqlFloatType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            if (config.getScale() != null && config.getPrecision() == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "param settings is illegal for float");
            }
            Integer scale = config.getScale() == null ? -1 : config.getScale();
            Integer precision = config.getPrecision() == null ? -1 : config.getPrecision();
            if (scale == -1 && precision != -1) {
                precision = -1;
            }
            BigDecimal defaultValue = config.getDefaultValue() == null ? null : new BigDecimal(config.getDefaultValue().toString());
            MysqlFloatType returnValue = new MysqlFloatType(precision, scale, generator, defaultValue, config.getAllowNull(), false);
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
     * mysql模式下的float类型
     */
    private static final DataTypeFactory OB_MYSQL_FLOAT = new DataTypeFactory<MysqlFloatType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_FLOAT";
        }

        @Override
        protected MysqlFloatType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            if (config.getScale() != null && config.getPrecision() == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "param settings is illegal for float");
            }
            Integer scale = config.getScale() == null ? -1 : config.getScale();
            Integer precision = config.getPrecision() == null ? -1 : config.getPrecision();
            if (scale == -1 && precision != -1) {
                precision = -1;
            }
            BigDecimal defaultValue = config.getDefaultValue() == null ? null : new BigDecimal(config.getDefaultValue().toString());
            MysqlFloatType returnValue = new MysqlFloatType(precision, scale, generator, defaultValue, config.getAllowNull(), true);
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
     * mysql模式下的decimal unsigned类型
     */
    private static final DataTypeFactory OB_MYSQL_DECIMAL_UNSIGNED
            = new DataTypeFactory<MysqlDecimalType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_DECIMAL_UNSIGNED";
        }

        @Override
        protected MysqlDecimalType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            if (config.getScale() != null && config.getPrecision() == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "param settings is illegal for decimal");
            }
            Integer scale = config.getScale() == null ? 0 : config.getScale();
            Integer precision = config.getPrecision() == null ? 10 : config.getPrecision();
            BigDecimal defaultValue = config.getDefaultValue() == null ? null : new BigDecimal(config.getDefaultValue().toString());
            MysqlDecimalType returnValue = new MysqlDecimalType(precision, scale, generator, defaultValue, config.getAllowNull(), false);
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
     * mysql模式下的decimal类型
     */
    private static final DataTypeFactory OB_MYSQL_DECIMAL
            = new DataTypeFactory<MysqlDecimalType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_DECIMAL";
        }

        @Override
        protected MysqlDecimalType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            if (config.getScale() != null && config.getPrecision() == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "param settings is illegal for decimal");
            }
            Integer scale = config.getScale() == null ? 0 : config.getScale();
            Integer precision = config.getPrecision() == null ? 10 : config.getPrecision();
            BigDecimal defaultValue = config.getDefaultValue() == null ? null : new BigDecimal(config.getDefaultValue().toString());
            MysqlDecimalType returnValue = new MysqlDecimalType(precision, scale, generator, defaultValue, config.getAllowNull(), true);
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
     * mysql模式下的int unsigned类型
     */
    private static final DataTypeFactory OB_MYSQL_BIGINT_UNSIGNED
            = new DataTypeFactory<MysqlBigIntType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_BIGINT_UNSIGNED";
        }

        @Override
        protected MysqlBigIntType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            BigDecimal defaultValue = null;
            if (config.getDefaultValue() != null) {
                defaultValue = new BigDecimal(config.getDefaultValue().toString());
            }
            MysqlBigIntType returnValue = new MysqlBigIntType(generator, defaultValue, config.getAllowNull(), false);
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
     * mysql模式下的int类型
     */
    private static final DataTypeFactory OB_MYSQL_BIGINT
            = new DataTypeFactory<MysqlBigIntType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_BIGINT";
        }

        @Override
        protected MysqlBigIntType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            BigDecimal defaultValue = null;
            if (config.getDefaultValue() != null) {
                defaultValue = new BigDecimal(config.getDefaultValue().toString());
            }
            MysqlBigIntType returnValue = new MysqlBigIntType(generator, defaultValue, config.getAllowNull(), true);
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
     * mysql模式下的int unsigned类型
     */
    private static final DataTypeFactory OB_MYSQL_INT_UNSIGNED
            = new DataTypeFactory<MysqlIntType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_INT_UNSIGNED";
        }

        @Override
        protected MysqlIntType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
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
     * mysql模式下的int类型
     */
    private static final DataTypeFactory OB_MYSQL_INT = new DataTypeFactory<MysqlIntType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_INT";
        }

        @Override
        protected MysqlIntType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
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
     * mysql模式下的mediumInt unsigned类型
     */
    private static final DataTypeFactory OB_MYSQL_MEDIUMINT_UNSIGNED
            = new DataTypeFactory<MysqlMediumIntType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_MEDIUMINT_UNSIGNED";
        }

        @Override
        protected MysqlMediumIntType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            BigDecimal defaultValue = null;
            if (config.getDefaultValue() != null) {
                defaultValue = new BigDecimal(config.getDefaultValue().toString());
            }
            MysqlMediumIntType returnValue = new MysqlMediumIntType(generator, defaultValue, config.getAllowNull(), false);
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
     * mysql模式下的mediumInt类型
     */
    private static final DataTypeFactory OB_MYSQL_MEDIUMINT
            = new DataTypeFactory<MysqlMediumIntType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_MEDIUMINT";
        }

        @Override
        protected MysqlMediumIntType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            BigDecimal defaultValue = null;
            if (config.getDefaultValue() != null) {
                defaultValue = new BigDecimal(config.getDefaultValue().toString());
            }
            MysqlMediumIntType returnValue = new MysqlMediumIntType(generator, defaultValue, config.getAllowNull(), true);
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
     * mysql模式下的SmallInt unsigned类型
     */
    private static final DataTypeFactory OB_MYSQL_SMALLINT_UNSIGNED
            = new DataTypeFactory<MysqlSmallIntType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_SMALLINT_UNSIGNED";
        }

        @Override
        protected MysqlSmallIntType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            BigDecimal defaultValue = null;
            if (config.getDefaultValue() != null) {
                defaultValue = new BigDecimal(config.getDefaultValue().toString());
            }
            MysqlSmallIntType returnValue = new MysqlSmallIntType(generator, defaultValue, config.getAllowNull(), false);
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
     * mysql模式下的SmallInt类型
     */
    private static final DataTypeFactory OB_MYSQL_SMALLINT
            = new DataTypeFactory<MysqlSmallIntType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_SMALLINT";
        }

        @Override
        protected MysqlSmallIntType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            BigDecimal defaultValue = null;
            if (config.getDefaultValue() != null) {
                defaultValue = new BigDecimal(config.getDefaultValue().toString());
            }
            MysqlSmallIntType returnValue = new MysqlSmallIntType(generator, defaultValue, config.getAllowNull(), true);
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
     * mysql模式下的tinyint unsigned类型
     */
    private static final DataTypeFactory OB_MYSQL_TINYINT_UNSIGNED
            = new DataTypeFactory<MysqlTinyIntType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_TINYINT_UNSIGNED";
        }

        @Override
        protected MysqlTinyIntType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            BigDecimal defaultValue = null;
            if (config.getDefaultValue() != null) {
                defaultValue = new BigDecimal(config.getDefaultValue().toString());
            }
            MysqlTinyIntType returnValue = new MysqlTinyIntType(generator, defaultValue, config.getAllowNull(), false);
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
     * mysql模式下的tinyint类型
     */
    private static final DataTypeFactory OB_MYSQL_TINYINT
            = new DataTypeFactory<MysqlTinyIntType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_MYSQL_TINYINT";
        }

        @Override
        protected MysqlTinyIntType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            BigDecimal defaultValue = null;
            if (config.getDefaultValue() != null) {
                defaultValue = new BigDecimal(config.getDefaultValue().toString());
            }
            MysqlTinyIntType returnValue = new MysqlTinyIntType(generator, defaultValue, config.getAllowNull(), true);
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
     * oracle模式下的RAW类型
     */
    private static final DataTypeFactory OB_ORACLE_RAW = new DataTypeFactory<OracleRawType, CharDataTypeConfig, ByteGeneratorBase>() {
        @Override
        public String name() {
            return "OB_ORACLE_RAW";
        }

        @Override
        protected OracleRawType newInstance(CharDataTypeConfig config, ByteGeneratorBase generator) {
            OracleRawType returnValue = new OracleRawType(null, config.getAllowNull(), config.getWidth(), generator);
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
     * oracle模式下的clob类型
     */
    private static final DataTypeFactory OB_ORACLE_CLOB = new DataTypeFactory<OracleBlobType, CharDataTypeConfig, ByteGeneratorBase>() {
        @Override
        public String name() {
            return "OB_ORACLE_CLOB";
        }

        @Override
        protected OracleBlobType newInstance(CharDataTypeConfig config, ByteGeneratorBase generator) {
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
     * oracle模式下的blob类型
     */
    private static final DataTypeFactory OB_ORACLE_BLOB = new DataTypeFactory<OracleBlobType, CharDataTypeConfig, ByteGeneratorBase>() {
        @Override
        public String name() {
            return "OB_ORACLE_BLOB";
        }

        @Override
        protected OracleBlobType newInstance(CharDataTypeConfig config, ByteGeneratorBase generator) {
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
     * oracle模式下的timstamp with local time zone类型
     */
    private static final DataTypeFactory OB_ORACLE_TIMESTAMP_WITH_LOCAL_TIME_ZONE
            = new DataTypeFactory<OracleTimestampType, DateDataTypeConfig, DateGeneratorBase>() {

        @Override
        public String name() {
            return "OB_ORACLE_TIMESTAMP_WITH_LOCAL_TIME_ZONE";
        }

        @Override
        protected OracleTimestampType newInstance(DateDataTypeConfig config, DateGeneratorBase generator) {
            int scale = 3;
            if (config.getScale() != null) {
                scale = config.getScale();
            }
            Timestamp defaultValue = null;
            Object val = config.getDefaultValue();
            if (val != null) {
                defaultValue = new Timestamp(Long.valueOf(val.toString()));
            }
            OracleTimestampType returnValue = new OracleTimestampType(generator, scale, defaultValue, config.getAllowNull());
            if (config.getTimezone() != null) {
                returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
            }
            if (config.getLowValue() != null) {
                returnValue.setLowValue(new Timestamp(Long.valueOf(config.getLowValue().toString())));
            }
            if (config.getHighValue() != null) {
                returnValue.setHighValue(new Timestamp(Long.valueOf(config.getHighValue().toString())));
            }
            return returnValue;
        }
    };

    /**
     * oracle模式下的timstamp with time zone类型
     */
    private static final DataTypeFactory OB_ORACLE_TIMESTAMP_WITH_TIME_ZONE
            = new DataTypeFactory<OracleTimestampType, DateDataTypeConfig, DateGeneratorBase>() {

        @Override
        public String name() {
            return "OB_ORACLE_TIMESTAMP_WITH_TIME_ZONE";
        }

        @Override
        protected OracleTimestampType newInstance(DateDataTypeConfig config, DateGeneratorBase generator) {
            int scale = 3;
            if (config.getScale() != null) {
                scale = config.getScale();
            }
            Timestamp defaultValue = null;
            Object val = config.getDefaultValue();
            if (val != null) {
                defaultValue = new Timestamp(Long.valueOf(val.toString()));
            }
            OracleTimestampType returnValue = new OracleTimestampType(generator, scale, defaultValue, config.getAllowNull());
            if (config.getTimezone() != null) {
                returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
            }
            if (config.getLowValue() != null) {
                returnValue.setLowValue(new Timestamp(Long.valueOf(config.getLowValue().toString())));
            }
            if (config.getHighValue() != null) {
                returnValue.setHighValue(new Timestamp(Long.valueOf(config.getHighValue().toString())));
            }
            return returnValue;
        }
    };

    /**
     * oracle模式下的timstamp类型
     */
    private static final DataTypeFactory OB_ORACLE_TIMESTAMP
            = new DataTypeFactory<OracleTimestampType, DateDataTypeConfig, DateGeneratorBase>() {

        @Override
        public String name() {
            return "OB_ORACLE_TIMESTAMP";
        }

        @Override
        protected OracleTimestampType newInstance(DateDataTypeConfig config, DateGeneratorBase generator) {
            int scale = config.getScale() == null ? 3 : config.getScale();
            Timestamp defaultValue = null;
            Object val = config.getDefaultValue();
            if (val != null) {
                defaultValue = new Timestamp(Long.valueOf(val.toString()));
            }
            OracleTimestampType returnValue = new OracleTimestampType(generator, scale, defaultValue, config.getAllowNull());
            if (config.getTimezone() != null) {
                returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
            }
            if (config.getLowValue() != null) {
                returnValue.setLowValue(new Timestamp(Long.valueOf(config.getLowValue().toString())));
            }
            if (config.getHighValue() != null) {
                returnValue.setHighValue(new Timestamp(Long.valueOf(config.getHighValue().toString())));
            }
            return returnValue;
        }
    };

    /**
     * oracle模式下的date类型
     */
    private static final DataTypeFactory OB_ORACLE_DATE = new DataTypeFactory<OracleDateType, DateDataTypeConfig, DateGeneratorBase>() {

        @Override
        public String name() {
            return "OB_ORACLE_DATE";
        }

        @Override
        protected OracleDateType newInstance(DateDataTypeConfig config, DateGeneratorBase generator) {
            Date defaultValue = null;
            Object val = config.getDefaultValue();
            if (val != null) {
                defaultValue = new Date(Long.valueOf(val.toString()));
            }
            OracleDateType returnValue = new OracleDateType(generator, defaultValue, config.getAllowNull());
            if (config.getTimezone() != null) {
                returnValue.setTimeZone(TimeZone.getTimeZone(config.getTimezone()));
            }
            if (config.getLowValue() != null) {
                returnValue.setLowValue(new Date(Long.valueOf(config.getLowValue().toString())));
            }
            if (config.getHighValue() != null) {
                returnValue.setHighValue(new Date(Long.valueOf(config.getHighValue().toString())));
            }
            return returnValue;
        }
    };

    /**
     * oracle模式下的interval year to month类型
     */
    private static final DataTypeFactory OB_ORACLE_INTERVAL_YEAR_TO_MONTH
            = new DataTypeFactory<OracleIntervalYMType, DateDataTypeConfig, BaseGenerator>() {

        @Override
        public String name() {
            return "OB_ORACLE_INTERVAL_YEAR_TO_MONTH";
        }

        @Override
        protected OracleIntervalYMType newInstance(DateDataTypeConfig config, BaseGenerator generator) {
            INTERVALYM defaultValue = null;
            Object val = config.getDefaultValue();
            if (val != null) {
                defaultValue = new INTERVALYM(defaultValue.toString());
            }
            OracleIntervalYMType returnValue = new OracleIntervalYMType(generator, config.getScale(), defaultValue, config.getAllowNull());
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
     * oracle模式下的interval year to month类型
     */
    private static final DataTypeFactory OB_ORACLE_INTERVAL_DAY_TO_SECOND
            = new DataTypeFactory<OracleIntervalYMType, DateDataTypeConfig, BaseGenerator>() {

        @Override
        public String name() {
            return "OB_ORACLE_INTERVAL_DAY_TO_SECOND";
        }

        @Override
        protected OracleIntervalYMType newInstance(DateDataTypeConfig config, BaseGenerator generator) {
            INTERVALYM defaultValue = null;
            Object val = config.getDefaultValue();
            if (val != null) {
                defaultValue = new INTERVALYM(defaultValue.toString());
            }
            OracleIntervalYMType returnValue = new OracleIntervalYMType(generator, config.getScale(), defaultValue, config.getAllowNull());
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
     * oracle模式下的nvarchar2类型
     */
    private static final DataTypeFactory OB_ORACLE_NVARCHAR
            = new DataTypeFactory<OracleNvarCharType, CharDataTypeConfig, CharGeneratorBase>() {

        @Override
        public String name() {
            return "OB_ORACLE_NVARCHAR";
        }

        @Override
        protected OracleNvarCharType newInstance(CharDataTypeConfig config, CharGeneratorBase generator) {
            String charset = config.getCharset();
            Integer length = config.getWidth();
            if (length == null || charset == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "error parametes for VARCHAR2");
            }
            CharsetType charsetType = CharsetType.valueOf(charset);
            OracleNvarCharType returnValue = new OracleNvarCharType(generator, length, (String) config.getDefaultValue(),
                    config.getAllowNull(), charsetType);
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
     * oracle模式下的varchar2类型
     */
    private static final DataTypeFactory OB_ORACLE_VARCHAR2
            = new DataTypeFactory<OracleVarCharType, CharDataTypeConfig, CharGeneratorBase>() {
        @Override
        public String name() {
            return "OB_ORACLE_VARCHAR2";
        }

        @Override
        protected OracleVarCharType newInstance(CharDataTypeConfig config, CharGeneratorBase generator) {
            String charset = config.getCharset();
            Integer length = config.getWidth();
            if (length == null || charset == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "error parametes for VARCHAR2");
            }
            CharsetType charsetType = CharsetType.valueOf(charset);
            OracleVarCharType returnValue = new OracleVarCharType(generator, length, (String) config.getDefaultValue(),
                    config.getAllowNull(), charsetType, config.isUnicode());
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
     * oracle模式下的varchar类型
     */
    private static final DataTypeFactory OB_ORACLE_VARCHAR
            = new DataTypeFactory<OracleVarCharType, CharDataTypeConfig, CharGeneratorBase>() {
        @Override
        public String name() {
            return "OB_ORACLE_VARCHAR";
        }

        @Override
        protected OracleVarCharType newInstance(CharDataTypeConfig config, CharGeneratorBase generator) {
            String charset = config.getCharset();
            Integer length = config.getWidth();
            if (length == null || charset == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "error parametes for VARCHAR");
            }
            CharsetType charsetType = CharsetType.valueOf(charset);
            OracleVarCharType returnValue = new OracleVarCharType(generator, length, (String) config.getDefaultValue(),
                    config.getAllowNull(), charsetType, config.isUnicode());
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
     * oracle模式下字符类型的工厂实例
     */
    private static final DataTypeFactory OB_ORACLE_CHAR = new DataTypeFactory<OracleCharType, CharDataTypeConfig, CharGeneratorBase>() {

        @Override
        public String name() {
            return "OB_ORACLE_CHAR";
        }

        @Override
        protected OracleCharType newInstance(CharDataTypeConfig config, CharGeneratorBase generator) {
            String charset = config.getCharset();
            Integer length = config.getWidth();
            if (length == null || charset == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "error parametes for CHAR");
            }
            CharsetType charsetType = CharsetType.valueOf(charset);
            OracleCharType returnValue = new OracleCharType(length, (String) config.getDefaultValue(), config.getAllowNull(), charsetType,
                    generator, config.isUnicode());
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
     * oracle模式下NUMBER数据类型的工厂类实例
     */
    private static final DataTypeFactory OB_ORACLE_NUMBER
            = new DataTypeFactory<OracleNumberType, DigitDataTypeConfig, DigitalGeneratorBase>() {
        @Override
        public String name() {
            return "OB_ORACLE_NUMBER";
        }

        @Override
        protected OracleNumberType newInstance(DigitDataTypeConfig config, DigitalGeneratorBase generator) {
            Integer scale = config.getScale() == null ? 0 : config.getScale();
            Integer precision = config.getPrecision();
            if (precision == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "error parametes for NUMBER");
            }
            BigDecimal defaultValue = null;
            if (config.getDefaultValue() != null) {
                defaultValue = new BigDecimal(config.getDefaultValue().toString());
            }
            OracleNumberType returnValue = new OracleNumberType(precision, scale, generator, defaultValue, config.getAllowNull());
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
     * 实例映射表
     */
    private static final Map<String, DataTypeFactory> FACTORYNAME_2_FACTORYINSTANCE = new HashMap<>();

    static {
        try {
            Field[] fields = DataTypeFactory.class.getDeclaredFields();
            for (Field field : fields) {
                Object instance = field.get(DatatypeFactory.class);
                if (Modifier.isStatic(field.getModifiers()) && instance instanceof DataTypeFactory) {
                    FACTORYNAME_2_FACTORYINSTANCE.putIfAbsent(field.getName(), (DataTypeFactory) instance);
                }
            }
        } catch (IllegalAccessException e) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, e.getMessage());
        }
    }

    /**
     * 获取类型工厂类的名称
     *
     * @return 返回名称
     */
    abstract public String name();

    /**
     * 公共方法，通过该方法将一个列的config对象转为一个具体的列对象
     *
     * @param config 列配置对象
     * @return 返回列对象
     */
    public T make(V config) {
        try {
            if (config == null || config.getGenerator() == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "generator or generator builder can not be null");
            }
            GeneratorFactory factory = GeneratorFactory.getInstance(config.getGenerator());
            K generator = (K) factory.make(config.getGenParams());
            if (generator == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "generator can not be null");
            }
            return newInstance(config, generator);
        } catch (Exception e) {
            throw new MockerException(MockerError.PARAMETER_ERROR, e.getMessage());
        }
    }

    /**
     * 内部构造对象方法，用于从列配置对象内部的数据生成器构造器中实例化一个数据生成器
     *
     * @param config    列配置对象
     * @param generator 由上游传过来的数据生成器对象
     * @return 返回列对象
     */
    abstract protected T newInstance(V config, K generator);

    /**
     * 根据名称获取一个类型工厂实例
     *
     * @param factoryName 工厂实例名称
     * @return 返回工厂实例对象
     */
    public static DataTypeFactory getInstance(String factoryName) {
        DataTypeFactory returnVal = FACTORYNAME_2_FACTORYINSTANCE.get(factoryName);
        if (returnVal == null) {
            throw new MockerException(MockerError.UNKNOWN_DATA_TYPE);
        }
        return returnVal;
    }

    /**
     * 获取所有的工厂类实例
     *
     * @return 返回所有的工厂类实例
     */
    public static List<DataTypeFactory> listInstances() {
        List<DataTypeFactory> returnVal = new ArrayList<>();
        Set<Map.Entry<String, DataTypeFactory>> entries = FACTORYNAME_2_FACTORYINSTANCE.entrySet();
        for (Map.Entry<String, DataTypeFactory> entry : entries) {
            returnVal.add(entry.getValue());
        }
        return returnVal;
    }
}
