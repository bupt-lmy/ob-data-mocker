package com.oceanbase.tools.datamocker.generator.chartype;

import java.util.Random;

import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharCaseOption;

/**
 * 随机文本数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 16:14
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomGenerator extends CharGeneratorBase {
    /**
     * 可打印字符数组
     */
    private final static char[] PRINT_CHAR = new char[] {
            '!', '"', '#', '$', '%', '&', '(', ')', '*', '+', ',', '-', '.', '/', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':',
            ';', '<', '=', '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
            'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', ']', '^', '_', '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '{', '|', '}'
    };

    public RandomGenerator(CharCaseOption caseType) {
        super(caseType);
    }

    /**
     * 随机文本数据生成器没有参数，预检查模块默认返回真
     *
     * @param minLength 字符串的最小长度
     * @param maxLength 字符串的最大长度
     * @return 返回检查结果
     */
    @Override
    public Boolean preCheck(Integer minLength, Integer maxLength) {
        return true;
    }

    @Override
    public String generate(Integer minLength, Integer maxLength) {
        int actualLength = minLength + new Random().nextInt((maxLength - minLength) + 1);
        char[] returnVal = new char[actualLength];
        for (int i = 0; i < actualLength; i++) {
            int index = new Random().nextInt(PRINT_CHAR.length);
            returnVal[i] = PRINT_CHAR[index];
        }
        return caseOption().convert(new String(returnVal));
    }

    @Override
    public Long count(Integer minLength, Integer maxLength) {
        if (maxLength >= 4) {
            return Long.MAX_VALUE;
        }
        int interval = maxLength - minLength;
        double returnVal = 0;
        for (int i = 0; i < interval; i++) {
            returnVal += Math.pow(PRINT_CHAR.length, i + 1);
        }
        returnVal *= Math.pow(PRINT_CHAR.length, minLength);
        return (long) returnVal;
    }
}
