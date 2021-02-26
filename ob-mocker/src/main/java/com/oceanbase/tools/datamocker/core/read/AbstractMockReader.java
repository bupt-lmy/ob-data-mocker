package com.oceanbase.tools.datamocker.core.read;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.util.Pair;

/**
 * 数据的读取器，通过该接口读取出一条数据
 *
 * @author yh263208
 * @date 20201-01-14 14:14
 * @since OBMOCKER_0.1.0_snapshot
 */
public abstract class AbstractMockReader<T> {
    /**
     * 读出方法，通过调用该方法读出一条数据
     *
     * @return 返回产生的一条数据
     * @throws Exception 产生数据时可能会抛出异常
     */
    abstract public Pair<String, Pair<AbstractDataType, T>> read() throws Exception;

    /**
     * 获取原语的groupId，原语是一个整体操作的原子组成部分，因此需要一个groupId来标定哪些原语是属于同一个整体操作
     *
     * @return 返回groupId字符串
     */
    abstract public String groupId();
}
