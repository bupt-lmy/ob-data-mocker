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
import org.apache.commons.lang.Validate;

/**
 * Data generator builder, use this enumeration as a builder of various data generators to generate
 * data generator objects according to the configuration
 *
 * @author yh263208
 * @date 2020-12-24 21:47
 * @since OBMOCKER-snapshot-0.1.0
 */
public abstract class GeneratorFactory<T extends BaseGenerator<? extends Comparable<?>, ?>> {
    /**
     * null data generator
     */
    private static final GeneratorFactory<NullDigitGenerator> NULL_DIGIT_GENERATOR =
            new GeneratorFactory<NullDigitGenerator>() {
                @Override
                public NullDigitGenerator make(Map<String, Object> params) {
                    return new NullDigitGenerator();
                }
            };
    /**
     * null data generator
     */
    private static final GeneratorFactory<NullByteGenerator> NULL_BYTE_GENERATOR =
            new GeneratorFactory<NullByteGenerator>() {
                @Override
                public NullByteGenerator make(Map<String, Object> params) {
                    return new NullByteGenerator();
                }
            };
    /**
     * null data generator
     */
    private static final GeneratorFactory<NullCharGenerator> NULL_CHAR_GENERATOR =
            new GeneratorFactory<NullCharGenerator>() {
                @Override
                public NullCharGenerator make(Map<String, Object> params) {
                    return new NullCharGenerator();
                }
            };
    /**
     * null data generator
     */
    private static final GeneratorFactory<NullDateGenerator> NULL_DATE_GENERATOR =
            new GeneratorFactory<NullDateGenerator>() {
                @Override
                public NullDateGenerator make(Map<String, Object> params) {
                    return new NullDateGenerator();
                }
            };
    /**
     * null data generator
     */
    private static final GeneratorFactory<NullTimestampGenerator> NULL_TIMESTAMP_GENERATOR =
            new GeneratorFactory<NullTimestampGenerator>() {
                @Override
                public NullTimestampGenerator make(Map<String, Object> params) {
                    return new NullTimestampGenerator();
                }
            };
    /**
     * null data generator
     */
    private static final GeneratorFactory<NullIntervalYMGenerator> NULL_INTERVALYM_GENERATOR =
            new GeneratorFactory<NullIntervalYMGenerator>() {
                @Override
                public NullIntervalYMGenerator make(Map<String, Object> params) {
                    return new NullIntervalYMGenerator();
                }
            };
    /**
     * Step data generator
     */
    private static final GeneratorFactory<StepNumByteGenerator> STEP_NUMBER_BYTE_GENERATOR =
            new GeneratorFactory<StepNumByteGenerator>() {

                @Override
                public StepNumByteGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for STEP_NUMBER_BYTE_GENERATOR can not be empty");
                    if (params.get("start") == null || params.get("end") == null || params.get("step") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "Start, end or step for step number generator can not be null");
                    }
                    long start = Long.parseLong(params.get("start").toString());
                    long end = Long.parseLong(params.get("end").toString());
                    long step = Long.parseLong(params.get("step").toString());
                    boolean cycle = Boolean.parseBoolean(params.getOrDefault("round", Boolean.TRUE).toString());
                    return new StepNumByteGenerator(CharCaseOption.DEFAULT, start, end, step, cycle);
                }
            };
    /**
     * Random number generator
     */
    private static final GeneratorFactory<RandomNumByteGenerator> RANDOM_NUMBER_BYTE_GENERATOR =
            new GeneratorFactory<RandomNumByteGenerator>() {
                @Override
                public RandomNumByteGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for RANDOM_NUMBER_BYTE_GENERATOR can not be empty");
                    if (params.get("start") == null || params.get("end") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "Start or end for random number generator can not be null");
                    }
                    BigDecimal start = new BigDecimal(params.get("start").toString());
                    BigDecimal end = new BigDecimal(params.get("end").toString());
                    Validate.isTrue(start.compareTo(end) <= 0,
                            String.format("Low value can not bigger than high value [%s>%s]", start.toPlainString(),
                                    end.toPlainString()));
                    Validate.isTrue(start.compareTo(new BigDecimal(Long.MIN_VALUE)) >= 0, String.format(
                            "Start value for random number generator can not be smaller than %d", Long.MIN_VALUE));
                    Validate.isTrue(end.compareTo(new BigDecimal(Long.MAX_VALUE)) <= 0, String
                            .format("End value for random number generator can not be bigger than %d", Long.MAX_VALUE));
                    return new RandomNumByteGenerator(CharCaseOption.DEFAULT, start.longValue(), end.longValue());
                }
            };
    /**
     * Step Date Data Generator
     */
    private static final GeneratorFactory<StepDateByteGenerator> STEP_DATE_BYTE_GENERATOR =
            new GeneratorFactory<StepDateByteGenerator>() {
                @Override
                public StepDateByteGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for STEP_DATE_BYTE_GENERATOR can not be empty");
                    if (params.get("startTime") == null || params.get("endTime") == null
                            || params.get("step") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "StartTime, endTime or step for step date generator can not be null");
                    }
                    long startTime = Long.parseLong(params.get("startTime").toString());
                    long endTime = Long.parseLong(params.get("endTime").toString());
                    long realStep = Long.parseLong(params.get("step").toString());
                    boolean cycle = Boolean.parseBoolean(params.getOrDefault("round", Boolean.TRUE).toString());
                    String timeUnit = (String) params.getOrDefault("timeUnit", TimeUnit.MILLISECONDS.name());
                    String timezone = params.get("timezone") == null ? null : params.get("timezone").toString();
                    return new StepDateByteGenerator(CharCaseOption.DEFAULT, startTime, endTime, realStep,
                            TimeUnit.valueOf(timeUnit), cycle, timezone);
                }
            };
    /**
     * Fixed value date data generator
     */
    private static final GeneratorFactory<FixDateByteGenerator> FIX_DATE_BYTE_GENERATOR =
            new GeneratorFactory<FixDateByteGenerator>() {
                @Override
                public FixDateByteGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for FIX_DATE_BYTE_GENERATOR can not be empty");
                    if (params.get("timestamp") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "Timestamp for fix date generator can not be null");
                    }
                    long timestamp = Long.parseLong(params.get("timestamp").toString());
                    String timezone = params.get("timezone") == null ? null : params.get("timezone").toString();
                    return new FixDateByteGenerator(CharCaseOption.ALL_UPPER_CASE, timestamp, timezone);
                }
            };
    /**
     * Random date generator
     */
    private static final GeneratorFactory<RandomDateByteGenerator> RANDOM_DATE_BYTE_GENERATOR =
            new GeneratorFactory<RandomDateByteGenerator>() {
                @Override
                public RandomDateByteGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for RANDOM_DATE_BYTE_GENERATOR can not be empty");
                    if (params.get("startTime") == null || params.get("endTime") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "StartTime or endTime for random date generator can not be null");
                    }
                    long startTime = Long.parseLong(params.get("startTime").toString());
                    long endTime = Long.parseLong(params.get("endTime").toString());
                    String timezone = params.get("timezone") == null ? null : params.get("timezone").toString();
                    return new RandomDateByteGenerator(CharCaseOption.ALL_UPPER_CASE, startTime, endTime, timezone);
                }
            };
    /**
     * Boolean text data generator
     */
    private static final GeneratorFactory<BoolByteGenerator> BOOL_BYTE_GENERATOR =
            new GeneratorFactory<BoolByteGenerator>() {
                @Override
                public BoolByteGenerator make(Map<String, Object> params) {
                    String fixText = null;
                    if (params != null) {
                        fixText = params.get("fixText").toString();
                    }
                    return new BoolByteGenerator(CharCaseOption.ALL_UPPER_CASE, fixText);
                }
            };
    /**
     * Fixed value text data generator
     */
    private static final GeneratorFactory<FixByteGenerator> FIX_BYTE_GENERATOR =
            new GeneratorFactory<FixByteGenerator>() {
                @Override
                public FixByteGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for FIX_BYTE_GENERATOR can not be empty");
                    String caseType = params.getOrDefault("caseOption", CharCaseOption.DEFAULT.name()).toString();
                    String fixText = params.get("fixText").toString();
                    if (fixText == null || fixText.length() == 0) {
                        throw new MockerException(MockerError.PARAMETER_ERROR, "Fix text can not be null or empty");
                    }
                    CharCaseOption type = CharCaseOption.valueOf(caseType);
                    return new FixByteGenerator(type, fixText);
                }
            };
    /**
     * Random string data generator
     */
    private static final GeneratorFactory<RandomByteGenerator> RANDOM_BYTE_GENERATOR =
            new GeneratorFactory<RandomByteGenerator>() {
                @Override
                public RandomByteGenerator make(Map<String, Object> params) {
                    String caseType = CharCaseOption.DEFAULT.name();
                    if (params != null) {
                        caseType = params.getOrDefault("caseOption", CharCaseOption.DEFAULT.name()).toString();
                    }
                    CharCaseOption type = CharCaseOption.valueOf(caseType);
                    return new RandomByteGenerator(type);
                }
            };
    /**
     * Regular expression byte generator
     */
    private static final GeneratorFactory<RegExpByteGenerator> REGEXP_BYTE_GENERATOR =
            new GeneratorFactory<RegExpByteGenerator>() {
                @Override
                public RegExpByteGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for REGEXP_BYTE_GENERATOR can not be empty");
                    CharCaseOption type = CharCaseOption
                            .valueOf(params.getOrDefault("caseOption", CharCaseOption.DEFAULT.name()).toString());
                    String regText = params.get("regText").toString();
                    if (regText == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR, "Reg text can not be null");
                    }
                    return new RegExpByteGenerator(type, regText);
                }
            };
    /**
     * Sequential date generator
     */
    private static final GeneratorFactory<StepTimestampGenerator> STEP_TIMESTAMP_GENERATOR =
            new GeneratorFactory<StepTimestampGenerator>() {
                @Override
                public StepTimestampGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for STEP_TIMESTAMP_GENERATOR can not be empty");
                    if (params.get("step") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "Step for step date generator can not be null");
                    }
                    long realStep = Long.parseLong(params.get("step").toString());
                    boolean cycle = Boolean.parseBoolean(params.getOrDefault("round", Boolean.TRUE).toString());
                    String timeUnit = (String) params.getOrDefault("timeUnit", TimeUnit.MILLISECONDS.name());
                    return new StepTimestampGenerator(realStep, TimeUnit.valueOf(timeUnit), cycle);
                }
            };
    /**
     * Random date data generator
     */
    private static final GeneratorFactory<RandomTimestampGenerator> RANDOM_TIMESTAMP_GENERATOR =
            new GeneratorFactory<RandomTimestampGenerator>() {
                @Override
                public RandomTimestampGenerator make(Map<String, Object> params) {
                    return new RandomTimestampGenerator();
                }
            };
    /**
     * Fixed time range data generator
     */
    private static final GeneratorFactory<FixIntervalYMGenerator> FIX_INTERVALYM_GENERATOR =
            new GeneratorFactory<FixIntervalYMGenerator>() {
                @Override
                public FixIntervalYMGenerator make(Map<String, Object> params) {
                    throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                            "Fix interval year to month has not been supported yet");
                    // if (params == null) {
                    // throw new MockerException(MockerError.PARAMETER_ERROR, "param for fix INTERVALYM generator can
                    // not be null");
                    // }
                    // return new FixIntervalYMGenerator(params.get("fixText"));
                }
            };
    /**
     * Fixed timestamp data generator
     */
    private static final GeneratorFactory<FixTimestampGenerator> FIX_TIMESTAMP_GENERATOR =
            new GeneratorFactory<FixTimestampGenerator>() {
                @Override
                public FixTimestampGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for FIX_TIMESTAMP_GENERATOR can not be empty");
                    if (params.get("timestamp") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "Time stamp for fix date generator can not be null");
                    }
                    long timestamp = Long.parseLong(params.get("timestamp").toString());
                    return new FixTimestampGenerator(timestamp);
                }
            };
    /**
     * Sequential date generator
     */
    private static final GeneratorFactory<StepDateGenerator> STEP_DATE_GENERATOR =
            new GeneratorFactory<StepDateGenerator>() {
                @Override
                public StepDateGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for STEP_DATE_GENERATOR can not be empty");
                    if (params.get("step") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "Step for step date generator can not be null");
                    }
                    long realStep = Long.parseLong(params.get("step").toString());
                    boolean cycle = Boolean.parseBoolean(params.getOrDefault("round", Boolean.TRUE).toString());
                    String timeUnit = (String) params.getOrDefault("timeUnit", TimeUnit.MILLISECONDS.name());
                    return new StepDateGenerator(realStep, TimeUnit.valueOf(timeUnit), cycle);
                }
            };
    /**
     * Random date data generator
     */
    private static final GeneratorFactory<RandomDateGenerator> RANDOM_DATE_GENERATOR =
            new GeneratorFactory<RandomDateGenerator>() {
                @Override
                public RandomDateGenerator make(Map<String, Object> params) {
                    return new RandomDateGenerator();
                }
            };
    /**
     * Fixed date data generator
     */
    private static final GeneratorFactory<FixDateGenerator> FIX_DATE_GENERATOR =
            new GeneratorFactory<FixDateGenerator>() {
                @Override
                public FixDateGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for FIX_DATE_GENERATOR can not be empty");
                    if (params.get("timestamp") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "Time stamp for fix date generator can not be null");
                    }
                    long timestamp = Long.parseLong(params.get("timestamp").toString());
                    return new FixDateGenerator(timestamp);
                }
            };
    /**
     * Step data generator
     */
    private static final GeneratorFactory<StepNumGenerator> STEP_NUMBER_GENERATOR =
            new GeneratorFactory<StepNumGenerator>() {
                @Override
                public StepNumGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for STEP_NUMBER_GENERATOR can not be empty");
                    if (params.get("start") == null || params.get("end") == null || params.get("step") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "Start, end or step for step number generator can not be null");
                    }
                    long start = Long.parseLong(params.get("start").toString());
                    long end = Long.parseLong(params.get("end").toString());
                    long step = Long.parseLong(params.get("step").toString());
                    boolean cycle = Boolean.parseBoolean(params.getOrDefault("round", Boolean.TRUE).toString());
                    return new StepNumGenerator(CharCaseOption.DEFAULT, start, end, step, cycle);
                }
            };
    /**
     * Random number generator
     */
    private static final GeneratorFactory<RandomNumGenerator> RANDOM_NUMBER_GENERATOR =
            new GeneratorFactory<RandomNumGenerator>() {
                @Override
                public RandomNumGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for RANDOM_NUMBER_GENERATOR can not be empty");
                    if (params.get("start") == null || params.get("end") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "Start or end for random number generator can not be null");
                    }
                    BigDecimal start = new BigDecimal(params.get("start").toString());
                    BigDecimal end = new BigDecimal(params.get("end").toString());
                    Validate.isTrue(start.compareTo(end) <= 0,
                            String.format("Low value can not bigger than high value [%s>%s]", start.toPlainString(),
                                    end.toPlainString()));
                    Validate.isTrue(start.compareTo(new BigDecimal(Long.MIN_VALUE)) >= 0, String.format(
                            "Start value for random number generator can not be smaller than %d", Long.MIN_VALUE));
                    Validate.isTrue(end.compareTo(new BigDecimal(Long.MAX_VALUE)) <= 0, String
                            .format("End value for random number generator can not be bigger than %d", Long.MAX_VALUE));
                    return new RandomNumGenerator(CharCaseOption.DEFAULT, start.longValue(), end.longValue());
                }
            };
    /**
     * Random number generator
     */
    private static final GeneratorFactory<StepDateCharGenerator> STEP_DATE_CHAR_GENERATOR =
            new GeneratorFactory<StepDateCharGenerator>() {
                @Override
                public StepDateCharGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for STEP_DATE_CHAR_GENERATOR can not be empty");
                    if (params.get("startTime") == null || params.get("endTime") == null
                            || params.get("step") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "StartTime, endTime or step for step date generator can not be null");
                    }
                    long startTime = Long.parseLong(params.get("startTime").toString());
                    long endTime = Long.parseLong(params.get("endTime").toString());
                    long realStep = Long.parseLong(params.get("step").toString());
                    boolean cycle = Boolean.parseBoolean(params.getOrDefault("round", Boolean.TRUE).toString());
                    String timeUnit = (String) params.getOrDefault("timeUnit", TimeUnit.MILLISECONDS.name());
                    String timezone = params.get("timezone") == null ? null : params.get("timezone").toString();
                    return new StepDateCharGenerator(CharCaseOption.DEFAULT, startTime, endTime, realStep,
                            TimeUnit.valueOf(timeUnit), cycle, timezone);
                }
            };
    /**
     * Fixed value date data generator
     */
    private static final GeneratorFactory<FixDateCharGenerator> FIX_DATE_CHAR_GENERATOR =
            new GeneratorFactory<FixDateCharGenerator>() {
                @Override
                public FixDateCharGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for FIX_DATE_CHAR_GENERATOR can not be empty");
                    if (params.get("timestamp") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "Timestamp for fix date generator can not be null");
                    }
                    long timestamp = Long.parseLong(params.get("timestamp").toString());
                    String timezone = params.get("timezone") == null ? null : params.get("timezone").toString();
                    return new FixDateCharGenerator(CharCaseOption.ALL_UPPER_CASE, timestamp, timezone);
                }
            };
    /**
     * Random date generator
     */
    private static final GeneratorFactory<RandomDateCharGenerator> RANDOM_DATE_CHAR_GENERATOR =
            new GeneratorFactory<RandomDateCharGenerator>() {
                @Override
                public RandomDateCharGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for RANDOM_DATE_CHAR_GENERATOR can not be empty");
                    if (params.get("startTime") == null || params.get("endTime") == null) {
                        throw new MockerException(MockerError.PARAMETER_ERROR,
                                "StartTime or endTime for random date generator can not be null");
                    }
                    long startTime = Long.parseLong(params.get("startTime").toString());
                    long endTime = Long.parseLong(params.get("endTime").toString());
                    String timezone = params.get("timezone") == null ? null : params.get("timezone").toString();
                    return new RandomDateCharGenerator(CharCaseOption.ALL_UPPER_CASE, startTime, endTime, timezone);
                }
            };
    /**
     * Boolean text data generator
     */
    private static final GeneratorFactory<BoolCharGenerator> BOOL_CHAR_GENERATOR =
            new GeneratorFactory<BoolCharGenerator>() {
                @Override
                public BoolCharGenerator make(Map<String, Object> params) {
                    String fixText = null;
                    if (params != null) {
                        fixText = params.get("fixText").toString();
                    }
                    return new BoolCharGenerator(CharCaseOption.ALL_UPPER_CASE, fixText);
                }
            };
    /**
     * Fixed value text data generator
     */
    private static final GeneratorFactory<FixCharGenerator> FIX_CHAR_GENERATOR =
            new GeneratorFactory<FixCharGenerator>() {
                @Override
                public FixCharGenerator make(Map<String, Object> params) {
                    Validate.notEmpty(params, "Generator params for FIX_CHAR_GENERATOR can not be empty");
                    String caseType = params.getOrDefault("caseOption", CharCaseOption.DEFAULT.name()).toString();
                    String fixText = params.get("fixText").toString();
                    if (fixText == null || fixText.length() == 0) {
                        throw new MockerException(MockerError.PARAMETER_ERROR, "Fix text can not be null or empty");
                    }
                    CharCaseOption type = CharCaseOption.valueOf(caseType);
                    return new FixCharGenerator(type, fixText);
                }
            };
    /**
     * Random string data generator
     */
    private static final GeneratorFactory<RegExpGenerator> REGEXP_GENERATOR = new GeneratorFactory<RegExpGenerator>() {
        @Override
        public RegExpGenerator make(Map<String, Object> params) {
            Validate.notEmpty(params, "Generator params for REGEXP_GENERATOR can not be empty");
            CharCaseOption type =
                    CharCaseOption.valueOf(params.getOrDefault("caseOption", CharCaseOption.DEFAULT.name()).toString());
            String regText = params.get("regText").toString();
            if (regText == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Reg text can not be null");
            }
            return new RegExpGenerator(type, regText);
        }
    };
    /**
     * Random string data generator
     */
    private static final GeneratorFactory<RandomGenerator> RANDOM_GENERATOR = new GeneratorFactory<RandomGenerator>() {
        @Override
        public RandomGenerator make(Map<String, Object> params) {
            String caseType = CharCaseOption.DEFAULT.name();
            if (params != null) {
                caseType = params.getOrDefault("caseOption", CharCaseOption.DEFAULT.name()).toString();
            }
            CharCaseOption type = CharCaseOption.valueOf(caseType);
            return new RandomGenerator(type);
        }
    };
    /**
     * Step size digital data generator
     */
    private static final GeneratorFactory<StepGenerator> STEP_GENERATOR = new GeneratorFactory<StepGenerator>() {
        @Override
        public StepGenerator make(Map<String, Object> params) {
            Validate.notEmpty(params, "Generator params for STEP_GENERATOR can not be empty");
            if (params.get("step") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Step for step generator can not be null");
            }
            double step = Double.parseDouble(params.get("step").toString());
            boolean cycle = Boolean.parseBoolean(params.getOrDefault("round", Boolean.TRUE).toString());
            return new StepGenerator(step, cycle);
        }
    };
    /**
     * Fixed number generator
     */
    private static final GeneratorFactory<FixNumGenerator> FIX_GENERATOR = new GeneratorFactory<FixNumGenerator>() {
        @Override
        public FixNumGenerator make(Map<String, Object> params) {
            Validate.notEmpty(params, "Generator params for FIX_GENERATOR can not be empty");
            if (params.get("fixNum") == null) {
                throw new MockerException(MockerError.PARAMETER_ERROR, "Fix number for fix generator can not be null");
            }
            BigDecimal fixNum = new BigDecimal(params.get("fixNum").toString());
            return new FixNumGenerator(fixNum);
        }
    };
    /**
     * Normally distributed data generator factory class
     */
    private static final GeneratorFactory<NormalGenerator> NORMAL_GENERATOR = new GeneratorFactory<NormalGenerator>() {
        @Override
        public NormalGenerator make(Map<String, Object> params) {
            double average = Double.parseDouble(params.getOrDefault("average", 0).toString());
            double variance = Double.parseDouble(params.getOrDefault("variance", 1).toString());
            return new NormalGenerator(average, variance);
        }
    };
    /**
     * Randomly distributed data generator factory class
     */
    private static final GeneratorFactory<UniformGenerator> UNIFORM_GENERATOR =
            new GeneratorFactory<UniformGenerator>() {
                @Override
                public UniformGenerator make(Map<String, Object> params) {
                    return new UniformGenerator();
                }
            };
    /**
     * Instance mapping table
     */
    private static final Map<String, GeneratorFactory<? extends BaseGenerator<? extends Comparable<?>, ?>>> FACTORYNAME_2_FACTORYINSTANCE =
            new HashMap<>();

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
     * Abstract construction method, used to instantiate a data generator
     *
     * @param params Input parameters, pass in different parameters according to different data
     *        generators
     * @return Return the instantiated data generator
     */
    abstract public T make(Map<String, Object> params);

    /**
     * Get a type factory instance based on the name
     *
     * @param factoryName Factory instance name
     * @return Returns the factory instance object
     */
    public static GeneratorFactory<? extends BaseGenerator<? extends Comparable<?>, ?>> getInstance(
            String factoryName) {
        GeneratorFactory<? extends BaseGenerator<? extends Comparable<?>, ?>> returnVal =
                FACTORYNAME_2_FACTORYINSTANCE.get(factoryName);
        if (returnVal == null) {
            throw new MockerException(MockerError.UNKNOWN_DATA_TYPE);
        }
        return returnVal;
    }

    /**
     * Get all the factory class instances
     *
     * @return Return all instances of the factory class
     */
    public static List<GeneratorFactory<? extends BaseGenerator<? extends Comparable<?>, ?>>> listInstances() {
        List<GeneratorFactory<? extends BaseGenerator<? extends Comparable<?>, ?>>> returnVal = new ArrayList<>();
        Set<Entry<String, GeneratorFactory<? extends BaseGenerator<? extends Comparable<?>, ?>>>> entries =
                FACTORYNAME_2_FACTORYINSTANCE.entrySet();
        for (Map.Entry<String, GeneratorFactory<? extends BaseGenerator<? extends Comparable<?>, ?>>> entry : entries) {
            returnVal.add(entry.getValue());
        }
        return returnVal;
    }

}
