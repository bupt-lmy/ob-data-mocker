package com.oceanbase.tools.datamocker.util;

import java.util.UUID;

/**
 * 自定义一个Java对象用于表示Java配对，jfxrt.jar提供的Pair在很多版本的jdk上都不存在，造成很大的编译困难
 *
 * @author yh263208
 * @date 2020-12-31 17:50
 * @since OBMOCKER_snapshot_0.1.0
 */
public class Pair<T, V> {
    private final String uniqueId = UUID.randomUUID().toString();
    private T key;
    private V value;

    public Pair(T key, V value) {
        this.key = key;
        this.value = value;
    }

    public T getKey() {
        return this.key;
    }

    public V getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return String.format("Pair(\"%s\", \"%s\")", this.key.toString(), this.value.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Pair that = (Pair) o;
        return this.uniqueId == that.uniqueId;
    }

    @Override
    public int hashCode() {
        return this.uniqueId.hashCode();
    }
}
