/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.tools.datamocker.util;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Bitmap object, used to calculate whether a value has appeared or not with a small memory overhead
 *
 * @author yh263208
 * @date 2021-01-09 20:47
 * @since OBMOCKER_0.1.0_snapshot
 */
public class BitMap {
    private final int capacity;
    /**
     * Built-in byte array of bitmap
     */
    private final byte[] bytes;
    /**
     * Amplification factor, used to increase the capacity of the bitmap array and reduce the
     * probability of hash collision
     */
    private static final int INCREASE_FACTOR = 4;
    /**
     * Binary width of the capacity of the byte array
     */
    private final int capacityWidth;

    /**
     * Constructor, used to initialize a bitmap object of a specific size
     *
     * @param count Capacity, the capacity cannot be less than or equal to zero
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
     * Get the size of the bitmap object
     *
     * @return Return size in bytes
     */
    public int size() {
        return this.bytes.length;
    }

    /**
     * Get the capacity of the bitmap
     *
     * @return Return capacity
     */
    public int capacity() {
        return this.capacity;
    }

    /**
     * Used to add an object to the bitmap object
     *
     * @param obj object
     * @return Return whether the addition is successful
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
     * Determine whether an object already exists
     *
     * @param obj object
     * @return Return the judgment result
     */
    public boolean contains(Object obj) {
        if (obj == null) {
            return false;
        }
        int hashCode = obj.hashCode();
        return (this.bytes[getIndex(hashCode)] & 1 << getPosition(hashCode)) != 0;
    }

    /**
     * Reproduce the bitmap bit corresponding to an object to 0
     *
     * @param obj object
     * @return Return reset result
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
     * Remake the entire bitmap
     */
    public synchronized void clear() {
        int length = this.bytes.length;
        for (int i = 0; i < length; i++) {
            this.bytes[i] &= 0;
        }
    }

    /**
     * Calculate the subscript index of a hash value in the bitmap
     *
     * @param key hash value
     * @return Return position information
     */
    private int getIndex(int key) {
        int hashKey = key ^ (key >>> capacityWidth);
        return (hashKey & (capacity() - 1)) >> 3;
    }

    /**
     * Get the specific position on a byte in the bitmap array
     *
     * @param key hash value
     * @return Return position information
     */
    private int getPosition(int key) {
        return key & 0x07;
    }
}
