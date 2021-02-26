package com.oceanbase.tools.datamocker.generator;

/**
 * 字节数组对象类型数据生成器
 *
 * @author yh263208
 * @date 2020-12-16 22:39
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class ByteGeneratorBase extends BaseGenerator<Integer, byte[]> {
    /**
     * 预检查步骤，用于根据边界值校验该生成器是否可以正常工作
     *
     * @param minLength 最小值，
     * @param maxLength 最大值
     * @return 返回校验结果
     */
    @Override
    abstract public Boolean preCheck(Integer minLength, Integer maxLength);

    /**
     * 数据生成方法接口
     *
     * @param minLength 最小值，对于不同类型的数据生成器含义略有不同，对于数字型的生成任务反映的是生成数字的最小值，
     *                  如果是字符型的生成任务反映的是字符的字节最小值
     * @param maxLength 最大值，对于不同类型的数据生成器含义略有不同，对于数字型的生成任务反映的是生成数字的最小值，
     *                  如果是字符型的生成任务反映的是字符的字节最小值
     * @return 返回一个生成的具体值
     */
    @Override
    abstract public byte[] generate(Integer minLength, Integer maxLength);

    /**
     * 返回数据生成器一共能够生成的不重复的数据个数
     *
     * @return 返回具体的数值，如果数据生成器可以无限制生成数据则返回null
     */
    @Override
    abstract public Long count(Integer minLength, Integer maxLength);
}
