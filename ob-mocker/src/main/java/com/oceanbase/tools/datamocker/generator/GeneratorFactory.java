package com.oceanbase.tools.datamocker.generator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.bytetype.BoolByteGenerator;
import com.oceanbase.tools.datamocker.generator.bytetype.FixByteGenerator;
import com.oceanbase.tools.datamocker.generator.bytetype.FixDateByteGenerator;
import com.oceanbase.tools.datamocker.generator.bytetype.NullByteGenerator;
import com.oceanbase.tools.datamocker.generator.bytetype.RandomByteGenerator;
import com.oceanbase.tools.datamocker.generator.bytetype.RandomDateByteGenerator;
import com.oceanbase.tools.datamocker.generator.bytetype.RandomNumByteGenerator;
import com.oceanbase.tools.datamocker.generator.bytetype.RegExpByteGenerator;
import com.oceanbase.tools.datamocker.generator.bytetype.StepDateByteGenerator;
import com.oceanbase.tools.datamocker.generator.bytetype.StepNumByteGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.BoolCharGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.FixCharGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.FixDateCharGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.NullCharGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.RandomDateCharGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.RandomGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.RandomNumGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.RegExpGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.StepDateCharGenerator;
import com.oceanbase.tools.datamocker.generator.chartype.StepNumGenerator;
import com.oceanbase.tools.datamocker.generator.date.FixDateGenerator;
import com.oceanbase.tools.datamocker.generator.date.FixIntervalYMGenerator;
import com.oceanbase.tools.datamocker.generator.date.FixTimestampGenerator;
import com.oceanbase.tools.datamocker.generator.date.NullDateGenerator;
import com.oceanbase.tools.datamocker.generator.date.NullIntervalYMGenerator;
import com.oceanbase.tools.datamocker.generator.date.NullTimestampGenerator;
import com.oceanbase.tools.datamocker.generator.date.RandomDateGenerator;
import com.oceanbase.tools.datamocker.generator.date.RandomTimestampGenerator;
import com.oceanbase.tools.datamocker.generator.date.StepDateGenerator;
import com.oceanbase.tools.datamocker.generator.date.StepTimestampGenerator;
import com.oceanbase.tools.datamocker.generator.digit.FixNumGenerator;
import com.oceanbase.tools.datamocker.generator.digit.NormalGenerator;
import com.oceanbase.tools.datamocker.generator.digit.NullDigitGenerator;
import com.oceanbase.tools.datamocker.generator.digit.StepGenerator;
import com.oceanbase.tools.datamocker.generator.digit.UniformGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 数据生成器builder，使用该枚举作为各种数据生成器的Builder用于根据配置具体生成数据生成器对象
 *
 * @author yh263208
 * @date 2020-12-24 21:47
 * @since OBMOCKER-snapshot-0.1.0
 */
public abstract class GeneratorFactory<T extends BaseGenerator, V extends Object> {
    /**
     * null数据生成器
     */
    private static final GeneratorFactory NULL_DIGIT_GENERATOR = new GeneratorFactory<NullDigitGenerator, Object>() {

        @Override
        public NullDigitGenerator make(Map<String, Object> params) {
            return new NullDigitGenerator();
        }
    };
    /**
     * null数据生成器
     */
    private static final GeneratorFactory NULL_BYTE_GENERATOR = new GeneratorFactory<NullByteGenerator, Object>() {

        @Override
        public NullByteGenerator make(Map<String, Object> params) {
            return new NullByteGenerator();
        }
    };
    /**
     * null数据生成器
     */
    private static final GeneratorFactory NULL_CHAR_GENERATOR = new GeneratorFactory<NullCharGenerator, Object>() {

        @Override
        public NullCharGenerator make(Map<String, Object> params) {
            return new NullCharGenerator();
        }
    };
    /**
     * null数据生成器
     */
    private static final GeneratorFactory NULL_DATE_GENERATOR = new GeneratorFactory<NullDateGenerator, Object>() {

        @Override
        public NullDateGenerator make(Map<String, Object> params) {
            return new NullDateGenerator();
        }
    };
    /**
     * null数据生成器
     */
    private static final GeneratorFactory NULL_TIMESTAMP_GENERATOR = new GeneratorFactory<NullTimestampGenerator, Object>() {

        @Override
        public NullTimestampGenerator make(Map<String, Object> params) {
            return new NullTimestampGenerator();
        }
    };
    /**
     * null数据生成器
     */
    private static final GeneratorFactory NULL_INTERVALYM_GENERATOR = new GeneratorFactory<NullIntervalYMGenerator, Object>() {

        @Override
        public NullIntervalYMGenerator make(Map<String, Object> params) {
            return new NullIntervalYMGenerator();
        }
    };
    /**
     * 步长数据生成器
     */
    private static final GeneratorFactory STEP_NUMBER_BYTE_GENERATOR = new GeneratorFactory<StepNumByteGenerator, Object>() {

        @Override
        public StepNumByteGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator params for step date generator can not be null");
            }
            if (params.get("start") == null || params.get("end") == null || params.get("step") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Start, end or step for step number generator can not be null");
            }
            Long start = Long.valueOf(params.get("start").toString());
            Long end = Long.valueOf(params.get("end").toString());
            Long step = Long.valueOf(params.get("step").toString());
            Boolean cycle = (Boolean) params.getOrDefault("round", Boolean.TRUE);
            return new StepNumByteGenerator(CharCaseOption.DEFAULT, start, end, step, cycle);
        }
    };
    /**
     * 随机数字生成器
     */
    private static final GeneratorFactory RANDOM_NUMBER_BYTE_GENERATOR = new GeneratorFactory<RandomNumByteGenerator, Object>() {

        @Override
        public RandomNumByteGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator params for step date generator can not be null");
            }
            if (params.get("start") == null || params.get("end") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Start or end for random number generator can not be null");
            }
            BigDecimal start = new BigDecimal(params.get("start").toString());
            BigDecimal end = new BigDecimal(params.get("end").toString());
            if (start.compareTo(end) > 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        String.format("Low value can not bigger than high value [%s>%s]", start.toPlainString(),
                                end.toPlainString()));
            }
            if (start.compareTo(new BigDecimal(Long.MIN_VALUE)) < 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        String.format("Start value for random number generator can not be smaller than %d", Long.MIN_VALUE));
            }
            if (end.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        String.format("End value for random number generator can not be bigger than %d", Long.MAX_VALUE));
            }
            return new RandomNumByteGenerator(CharCaseOption.DEFAULT, start.longValue(), end.longValue());
        }
    };
    /**
     * 步长日期数据生成器
     */
    private static final GeneratorFactory STEP_DATE_BYTE_GENERATOR = new GeneratorFactory<StepDateByteGenerator, Object>() {

        @Override
        public StepDateByteGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator params for step date generator can not be null");
            }
            if (params.get("startTime") == null || params.get("endTime") == null || params.get("step") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        "StartTime, endTime or step for step date generator can not be null");
            }
            Long startTime = Long.valueOf(params.get("startTime").toString());
            Long endTime = Long.valueOf(params.get("endTime").toString());
            Long realStep = Long.valueOf(params.get("step").toString());
            Boolean cycle = (Boolean) params.getOrDefault("round", Boolean.TRUE);
            String timeUnit = (String) params.getOrDefault("timeUnit", TimeUnit.MILLISECONDS.name());
            String timezone = params.get("timezone") == null ? null : params.get("timezone").toString();
            return new StepDateByteGenerator(CharCaseOption.DEFAULT, startTime, endTime, realStep, TimeUnit.valueOf(timeUnit), cycle,
                    timezone);
        }
    };
    /**
     * 定值日期数据生成器
     */
    private static final GeneratorFactory FIX_DATE_BYTE_GENERATOR = new GeneratorFactory<FixDateByteGenerator, Object>() {

        @Override
        public FixDateByteGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator params for fix date generator can not be null");
            }
            if (params.get("timestamp") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Timestamp for fix date generator can not be null");
            }
            Long timestamp = Long.valueOf(params.get("timestamp").toString());
            String timezone = params.get("timezone") == null ? null : params.get("timezone").toString();
            return new FixDateByteGenerator(CharCaseOption.ALL_UPPER_CASE, timestamp, timezone);
        }
    };
    /**
     * 随机日期生成器
     */
    private static final GeneratorFactory RANDOM_DATE_BYTE_GENERATOR = new GeneratorFactory<RandomDateByteGenerator, Object>() {

        @Override
        public RandomDateByteGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator params for random date generator can not be null");
            }
            if (params.get("startTime") == null || params.get("endTime") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        "StartTime or endTime for random date generator can not be null");
            }
            Long startTime = Long.valueOf(params.get("startTime").toString());
            Long endTime = Long.valueOf(params.get("endTime").toString());
            String timezone = params.get("timezone") == null ? null : params.get("timezone").toString();
            return new RandomDateByteGenerator(CharCaseOption.ALL_UPPER_CASE, startTime, endTime, timezone);
        }
    };
    /**
     * 布尔文本数据生成器
     */
    private static final GeneratorFactory BOOL_BYTE_GENERATOR = new GeneratorFactory<BoolByteGenerator, String>() {

        @Override
        public BoolByteGenerator make(Map<String, String> params) {
            String fixText = null;
            if (params != null) {
                fixText = params.get("fixText");
            }
            return new BoolByteGenerator(CharCaseOption.ALL_UPPER_CASE, fixText);
        }
    };
    /**
     * 定值文本数据生成器
     */
    private static final GeneratorFactory FIX_BYTE_GENERATOR = new GeneratorFactory<FixByteGenerator, String>() {

        @Override
        public FixByteGenerator make(Map<String, String> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator parameters can not be null");
            }
            String caseType = params.getOrDefault("caseOption", CharCaseOption.DEFAULT.name());
            String fixText = params.get("fixText");
            if (fixText == null || fixText.length() == 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Fix text can not be null or empty");
            }
            CharCaseOption type = CharCaseOption.valueOf(caseType);
            return new FixByteGenerator(type, fixText);
        }
    };
    /**
     * 随机字符串数据生成器
     */
    private static final GeneratorFactory RANDOM_BYTE_GENERATOR = new GeneratorFactory<RandomByteGenerator, String>() {

        @Override
        public RandomByteGenerator make(Map<String, String> params) {
            String caseType = CharCaseOption.DEFAULT.name();
            if (params != null) {
                caseType = params.getOrDefault("caseOption", CharCaseOption.DEFAULT.name());
            }
            CharCaseOption type = CharCaseOption.valueOf(caseType);
            return new RandomByteGenerator(type);
        }
    };
    /**
     * 正则表达式字节生成器
     */
    private static final GeneratorFactory REGEXP_BYTE_GENERATOR = new GeneratorFactory<RegExpByteGenerator, String>() {

        @Override
        public RegExpByteGenerator make(Map<String, String> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator for reg exp generator can not be null");
            }
            CharCaseOption type = CharCaseOption.valueOf(params.getOrDefault("caseOption", CharCaseOption.DEFAULT.name()));
            String regText = params.get("regText");
            if (regText == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Reg text can not be null");
            }
            return new RegExpByteGenerator(type, regText);
        }
    };
    /**
     * 顺序日期生成器
     */
    private static final GeneratorFactory STEP_TIMESTAMP_GENERATOR = new GeneratorFactory<StepTimestampGenerator, Object>() {

        @Override
        public StepTimestampGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator params for step date generator can not be null");
            }
            if (params.get("step") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Step for step date generator can not be null");
            }
            Long realStep = Long.valueOf(params.get("step").toString());
            Boolean cycle = (Boolean) params.getOrDefault("round", Boolean.TRUE);
            String timeUnit = (String) params.getOrDefault("timeUnit", TimeUnit.MILLISECONDS.name());
            return new StepTimestampGenerator(realStep, TimeUnit.valueOf(timeUnit), cycle);
        }
    };
    /**
     * 随机日期数据生成器
     */
    private static final GeneratorFactory RANDOM_TIMESTAMP_GENERATOR = new GeneratorFactory<RandomTimestampGenerator, String>() {

        @Override
        public RandomTimestampGenerator make(Map<String, String> params) {
            return new RandomTimestampGenerator();
        }
    };
    /**
     * 固定时间范围数据生成器
     */
    private static final GeneratorFactory FIX_INTERVALYM_GENERATOR = new GeneratorFactory<FixIntervalYMGenerator, String>() {

        @Override
        public FixIntervalYMGenerator make(Map<String, String> params) {
            throw new MockerException(MockerError.NOT_SUPPORT_FEATURE, "Fix interval year to month has not been supported yet");
            //if (params == null) {
            //    throw new MockerException(MockerError.PARAMETER_ERROR, "param for fix INTERVALYM generator can not be null");
            //}
            //return new FixIntervalYMGenerator(params.get("fixText"));
        }
    };
    /**
     * 固定时间戳数据生成器
     */
    private static final GeneratorFactory FIX_TIMESTAMP_GENERATOR = new GeneratorFactory<FixTimestampGenerator, Object>() {

        @Override
        public FixTimestampGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Param for fix date generator can not be null");
            }
            if (params.get("timestamp") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Time stamp for fix date generator can not be null");
            }
            Long timestamp = Long.valueOf(params.get("timestamp").toString());
            return new FixTimestampGenerator(timestamp);
        }
    };
    /**
     * 顺序日期生成器
     */
    private static final GeneratorFactory STEP_DATE_GENERATOR = new GeneratorFactory<StepDateGenerator, Object>() {

        @Override
        public StepDateGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator params for step date generator can not be null");
            }
            if (params.get("step") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Step for step date generator can not be null");
            }
            Long realStep = Long.valueOf(params.get("step").toString());
            Boolean cycle = (Boolean) params.getOrDefault("round", Boolean.TRUE);
            String timeUnit = (String) params.getOrDefault("timeUnit", TimeUnit.MILLISECONDS.name());
            return new StepDateGenerator(realStep, TimeUnit.valueOf(timeUnit), cycle);
        }
    };
    /**
     * 随机日期数据生成器
     */
    private static final GeneratorFactory RANDOM_DATE_GENERATOR = new GeneratorFactory<RandomDateGenerator, String>() {

        @Override
        public RandomDateGenerator make(Map<String, String> params) {
            return new RandomDateGenerator();
        }
    };
    /**
     * 固定日期数据生成器
     */
    private static final GeneratorFactory FIX_DATE_GENERATOR = new GeneratorFactory<FixDateGenerator, Object>() {

        @Override
        public FixDateGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Param for fix date generator can not be null");
            }
            if (params.get("timestamp") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Time stamp for fix date generator can not be null");
            }
            Long timestamp = Long.valueOf(params.get("timestamp").toString());
            return new FixDateGenerator(timestamp);
        }
    };

    /**
     * 步长数据生成器
     */
    private static final GeneratorFactory STEP_NUMBER_GENERATOR = new GeneratorFactory<StepNumGenerator, Object>() {

        @Override
        public StepNumGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator params for step date generator can not be null");
            }
            if (params.get("start") == null || params.get("end") == null || params.get("step") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Start, end or step for step number generator can not be null");
            }
            Long start = Long.valueOf(params.get("start").toString());
            Long end = Long.valueOf(params.get("end").toString());
            Long step = Long.valueOf(params.get("step").toString());
            Boolean cycle = (Boolean) params.getOrDefault("round", Boolean.TRUE);
            return new StepNumGenerator(CharCaseOption.DEFAULT, start, end, step, cycle);
        }
    };
    /**
     * 随机数字生成器
     */
    private static final GeneratorFactory RANDOM_NUMBER_GENERATOR = new GeneratorFactory<RandomNumGenerator, Object>() {

        @Override
        public RandomNumGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator params for step date generator can not be null");
            }
            if (params.get("start") == null || params.get("end") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Start or end for random number generator can not be null");
            }
            BigDecimal start = new BigDecimal(params.get("start").toString());
            BigDecimal end = new BigDecimal(params.get("end").toString());
            if (start.compareTo(end) > 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        String.format("Low value can not bigger than high value [%s>%s]", start.toPlainString(),
                                end.toPlainString()));
            }
            if (start.compareTo(new BigDecimal(Long.MIN_VALUE)) < 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        String.format("Start value for random number generator can not be smaller than %d", Long.MIN_VALUE));
            }
            if (end.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        String.format("End value for random number generator can not be bigger than %d", Long.MAX_VALUE));
            }
            return new RandomNumGenerator(CharCaseOption.DEFAULT, start.longValue(), end.longValue());
        }
    };
    /**
     * 步长日期数据生成器
     */
    private static final GeneratorFactory STEP_DATE_CHAR_GENERATOR = new GeneratorFactory<StepDateCharGenerator, Object>() {

        @Override
        public StepDateCharGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator params for step date generator can not be null");
            }
            if (params.get("startTime") == null || params.get("endTime") == null || params.get("step") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        "StartTime, endTime or step for step date generator can not be null");
            }
            Long startTime = Long.valueOf(params.get("startTime").toString());
            Long endTime = Long.valueOf(params.get("endTime").toString());
            Long realStep = Long.valueOf(params.get("step").toString());
            Boolean cycle = (Boolean) params.getOrDefault("round", Boolean.TRUE);
            String timeUnit = (String) params.getOrDefault("timeUnit", TimeUnit.MILLISECONDS.name());
            String timezone = params.get("timezone") == null ? null : params.get("timezone").toString();
            return new StepDateCharGenerator(CharCaseOption.DEFAULT, startTime, endTime, realStep, TimeUnit.valueOf(timeUnit), cycle,
                    timezone);
        }
    };
    /**
     * 定值日期数据生成器
     */
    private static final GeneratorFactory FIX_DATE_CHAR_GENERATOR = new GeneratorFactory<FixDateCharGenerator, Object>() {

        @Override
        public FixDateCharGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator params for fix date generator can not be null");
            }
            if (params.get("timestamp") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Timestamp for fix date generator can not be null");
            }
            Long timestamp = Long.valueOf(params.get("timestamp").toString());
            String timezone = params.get("timezone") == null ? null : params.get("timezone").toString();
            return new FixDateCharGenerator(CharCaseOption.ALL_UPPER_CASE, timestamp, timezone);
        }
    };
    /**
     * 随机日期生成器
     */
    private static final GeneratorFactory RANDOM_DATE_CHAR_GENERATOR = new GeneratorFactory<RandomDateCharGenerator, Object>() {

        @Override
        public RandomDateCharGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator params for random date generator can not be null");
            }
            if (params.get("startTime") == null || params.get("endTime") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        "StartTime or endTime for random date generator can not be null");
            }
            Long startTime = Long.valueOf(params.get("startTime").toString());
            Long endTime = Long.valueOf(params.get("endTime").toString());
            String timezone = params.get("timezone") == null ? null : params.get("timezone").toString();
            return new RandomDateCharGenerator(CharCaseOption.ALL_UPPER_CASE, startTime, endTime, timezone);
        }
    };
    /**
     * 布尔文本数据生成器
     */
    private static final GeneratorFactory BOOL_CHAR_GENERATOR = new GeneratorFactory<BoolCharGenerator, String>() {

        @Override
        public BoolCharGenerator make(Map<String, String> params) {
            String fixText = null;
            if (params != null) {
                fixText = params.get("fixText");
            }
            return new BoolCharGenerator(CharCaseOption.ALL_UPPER_CASE, fixText);
        }
    };
    /**
     * 定值文本数据生成器
     */
    private static final GeneratorFactory FIX_CHAR_GENERATOR = new GeneratorFactory<FixCharGenerator, String>() {

        @Override
        public FixCharGenerator make(Map<String, String> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator parameters can not be null");
            }
            String caseType = params.getOrDefault("caseOption", CharCaseOption.DEFAULT.name());
            String fixText = params.get("fixText");
            if (fixText == null || fixText.length() == 0) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Fix text can not be null or empty");
            }
            CharCaseOption type = CharCaseOption.valueOf(caseType);
            return new FixCharGenerator(type, fixText);
        }
    };
    /**
     * 随机字符串数据生成器
     */
    private static final GeneratorFactory REGEXP_GENERATOR = new GeneratorFactory<RegExpGenerator, String>() {

        @Override
        public RegExpGenerator make(Map<String, String> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator for reg exp generator can not be null");
            }
            CharCaseOption type = CharCaseOption.valueOf(params.getOrDefault("caseOption", CharCaseOption.DEFAULT.name()));
            String regText = params.get("regText");
            if (regText == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Reg text can not be null");
            }
            return new RegExpGenerator(type, regText);
        }
    };
    /**
     * 随机字符串数据生成器
     */
    private static final GeneratorFactory RANDOM_GENERATOR = new GeneratorFactory<RandomGenerator, String>() {

        @Override
        public RandomGenerator make(Map<String, String> params) {
            String caseType = CharCaseOption.DEFAULT.name();
            if (params != null) {
                caseType = params.getOrDefault("caseOption", CharCaseOption.DEFAULT.name());
            }
            CharCaseOption type = CharCaseOption.valueOf(caseType);
            return new RandomGenerator(type);
        }
    };
    /**
     * 步长数字数据生成器
     */
    private static final GeneratorFactory STEP_GENERATOR = new GeneratorFactory<StepGenerator, Object>() {

        @Override
        public StepGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Generator params for step generator can not be null");
            }
            if (params.get("step") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Step for step generator can not be null");
            }
            Double step = Double.valueOf(params.get("step").toString());
            Boolean cycle = (Boolean) params.getOrDefault("round", Boolean.TRUE);
            return new StepGenerator(step, cycle);
        }
    };
    /**
     * 固定的数字生成器
     */
    private static final GeneratorFactory FIX_GENERATOR = new GeneratorFactory<FixNumGenerator, Object>() {

        @Override
        public FixNumGenerator make(Map<String, Object> params) {
            if (params == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Param for fix generator can not be null");
            }
            if (params.get("fixNum") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Fix number for fix generator can not be null");
            }
            BigDecimal fixNum = new BigDecimal(params.get("fixNum").toString());
            return new FixNumGenerator(fixNum);
        }
    };
    /**
     * 正态分布数据生成器工厂类
     */
    private static final GeneratorFactory NORMAL_GENERATOR = new GeneratorFactory<NormalGenerator, Double>() {
        @Override
        public NormalGenerator make(Map<String, Double> params) {
            Double average = params.getOrDefault("average", 0D);
            Double variance = params.getOrDefault("variance", 1D);
            return new NormalGenerator(average, variance);
        }
    };
    /**
     * 随机分布数据生成器工厂类
     */
    private static final GeneratorFactory UNIFORM_GENERATOR = new GeneratorFactory<UniformGenerator, Object>() {
        @Override
        public UniformGenerator make(Map<String, Object> params) {
            return new UniformGenerator();
        }

    };

    /**
     * 实例映射表
     */
    private static final Map<String, GeneratorFactory> FACTORYNAME_2_FACTORYINSTANCE = new HashMap<>();

    static {
        try {
            Field[] fields = GeneratorFactory.class.getDeclaredFields();
            for (Field field : fields) {
                Object instance = field.get(GeneratorFactory.class);
                if (Modifier.isStatic(field.getModifiers()) && instance instanceof GeneratorFactory) {
                    FACTORYNAME_2_FACTORYINSTANCE.putIfAbsent(field.getName(), (GeneratorFactory) instance);
                }
            }
        } catch (IllegalAccessException e) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, e.getMessage());
        }
    }

    /**
     * 抽象构建方法，用于示例化一个数据生成器
     *
     * @param params 入参，根据数据生成器的不同传入不同的参数
     * @return 返回实例化好的数据生成器
     */
    abstract public T make(Map<String, V> params);

    /**
     * 根据名称获取一个类型工厂实例
     *
     * @param factoryName 工厂实例名称
     * @return 返回工厂实例对象
     */
    public static GeneratorFactory getInstance(String factoryName) {
        GeneratorFactory returnVal = FACTORYNAME_2_FACTORYINSTANCE.get(factoryName);
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
    public static List<GeneratorFactory> listInstances() {
        List<GeneratorFactory> returnVal = new ArrayList<>();
        Set<Entry<String, GeneratorFactory>> entries = FACTORYNAME_2_FACTORYINSTANCE.entrySet();
        for (Map.Entry<String, GeneratorFactory> entry : entries) {
            returnVal.add(entry.getValue());
        }
        return returnVal;
    }
}
