package com.oceanbase.tools.datamocker.util;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 位图对象，用于用较小的内存开销来计算一个值是否出现过
 *
 * @author yh263208
 * @date 2021-01-09 20:47
 * @since OBMOCKER_0.1.0_snapshot
 */
public class BitMap {
    private final int capacity;
    /**
     * 位图的内置字节数组
     */
    private final byte[] bytes;
    /**
     * 放大因子，用于增大位图数组的容量，降低hash碰撞的概率
     */
    private static final int INCREASE_FACTOR = 4;
    /**
     * 字节数组的容量二进制宽度
     */
    private final int capacityWidth;

    /**
     * 构造方法，用于初始化一个特定大小的位图对象
     *
     * @param count 容量，该容量不能小于等于零
     */
    public BitMap(int count) {
        if (count <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Capacity of the bitmap can not be equal to or smaller than zero");
        }
        int pow = new Double(String.valueOf(Math.log(count) / Math.log(2))).intValue() + 1;
        int factorPow = new Double(String.valueOf(Math.log(INCREASE_FACTOR) / Math.log(2))).intValue();
        this.capacity = 1 << (pow + factorPow);
        capacityWidth = Integer.toBinaryString(Math.abs(capacity - 1)).length();
        this.bytes = new byte[this.capacity >> 3];
    }

    /**
     * 获取位图对象的大小
     *
     * @return 返回字节大小
     */
    public int size() {
        return this.bytes.length;
    }

    /**
     * 获取位图的容量
     *
     * @return 返回容量
     */
    public int capacity() {
        return this.capacity;
    }

    /**
     * 用于向位图对象中增加一个对象
     *
     * @param obj 增加的对象
     * @return 返回是否添加成功
     */
    public synchronized boolean add(Object obj) {
        if (obj == null) {
            return false;
        }
        int hashCode = obj.hashCode();
        this.bytes[getIndex(hashCode)] |= 1 << getPosition(hashCode);
        return true;
    }

    /**
     * 判断一个对象是否已经存在
     *
     * @param obj 对象
     * @return 返回判断结果
     */
    public boolean contains(Object obj) {
        if (obj == null) {
            return false;
        }
        int hashCode = obj.hashCode();
        return (this.bytes[getIndex(hashCode)] & 1 << getPosition(hashCode)) != 0;
    }

    /**
     * 重制某一个对象对应的位图位为0
     *
     * @param obj 对象
     * @return 返回重置结果
     */
    public synchronized boolean clear(Object obj) {
        if (obj == null) {
            return false;
        }
        int hashCode = obj.hashCode();
        this.bytes[getIndex(hashCode)] &= ~(1 << getPosition(hashCode));
        return true;
    }

    /**
     * 重制整个位图
     *
     * @return 返回重置结果
     */
    public synchronized void clear() {
        int length = this.bytes.length;
        for (int i = 0; i < length; i++) {
            this.bytes[i] &= 0;
        }
    }

    /**
     * 计算一个hash值在位图中的下标索引
     *
     * @param key hash值
     * @return 返回位图索引
     */
    private int getIndex(int key) {
        int hashKey = key ^ (key >>> capacityWidth);
        return (hashKey & (capacity() - 1)) >> 3;
    }

    /**
     * 获取在位图数组中某一个字节上的具体位置
     *
     * @param key hash值
     * @return 返回position信息
     */
    private int getPosition(int key) {
        return key & 0x07;
    }
}
