package com.oceanbase.tools.datamocker.generator.chartype;

import com.oceanbase.tools.datamocker.generator.BaseCharGenerator;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;

/**
 * Null data generator of char type
 *
 * @author yh263208
 * @date 2021-01-26 14:41
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NullCharGenerator extends BaseCharGenerator {
    public NullCharGenerator() {
        super(CharCaseOption.DEFAULT);
    }

    @Override
    protected Boolean doPreCheck(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
        return true;
    }

    @Override
    protected String doGenerate(Integer minLength, Integer maxLength, CharsetType charsetType,
            CharCaseOption caseOption,
            boolean isUnicode) {
        return null;
    }

    @Override
    protected Long doCount(Integer minLength, Integer maxLength, CharsetType charsetType, CharCaseOption caseOption,
            boolean isUnicode) {
        return 1L;
    }

}
