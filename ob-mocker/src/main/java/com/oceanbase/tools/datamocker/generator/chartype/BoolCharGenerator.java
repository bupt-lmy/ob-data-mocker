package com.oceanbase.tools.datamocker.generator.chartype;

import com.oceanbase.tools.datamocker.generator.BaseCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * Boolean data generator
 *
 * @author yh263208
 * @date 2020-12-16 23:38
 * @since OBMOCKER_snapshot_0.1.0
 */
public class BoolCharGenerator extends BaseCharGenerator {
    /**
     * Fixed boolean type, null if not passed, representing random boolean type
     */
    private final Boolean fixBool;

    public BoolCharGenerator(CharCaseOption caseType, String fixBool) {
        super(caseType);
        if (fixBool == null || fixBool.length() == 0) {
            this.fixBool = null;
        } else {
            this.fixBool = Boolean.valueOf(fixBool);
        }
    }

    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        int realLength = "FALSE".length();
        if (realLength >= minLength) {
            return realLength <= maxLength;
        }
        return false;
    }

    @Override
    public String generate(Integer minLength, Integer maxLength) {
        if (this.fixBool != null) {
            return caseOption().convert(this.fixBool.toString());
        }
        if (Math.random() > 0.5) {
            return "TRUE";
        }
        return "FALSE";
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        if (this.fixBool == null) {
            return 2L;
        }
        return 1L;
    }
}
