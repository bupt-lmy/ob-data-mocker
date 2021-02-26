package com.oceanbase.tools.datamocker.generator.chartype;

import java.io.UnsupportedEncodingException;

import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 定值文本数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 23:13
 * @since OBMOCKER_snapshot_0.1.0
 */
public class FixCharGenerator extends CharGeneratorBase {
    /**
     * 定值文本
     */
    private final String fixText;

    /**
     * 构造方法
     *
     * @param caseType 字符大小写控制配置
     * @param fixText  定值文本
     */
    public FixCharGenerator(CharCaseOption caseType, String fixText) {
        super(caseType);
        this.fixText = fixText;
    }

    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        int realLength = -1;
        if (unicode()) {
            realLength = this.fixText.length();
        } else {
            try {
                realLength = this.fixText.getBytes(this.charset().getCharSet()).length;
            } catch (UnsupportedEncodingException e) {
                throw new MockerException(MockerError.UNKNOWN_ERROR, e.getMessage());
            }
        }
        if (realLength >= minLength) {
            if (realLength <= maxLength) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String generate(Integer minLength, Integer maxLength) {
        return caseOption().convert(this.fixText);
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        return 1L;
    }
}
