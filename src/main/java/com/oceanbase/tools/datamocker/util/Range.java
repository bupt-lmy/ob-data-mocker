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

/**
 * Represents a numeric interval, which is a left-closed right-closed interval
 *
 * @author yh263208
 * @date 2020-12-11 20：55
 * @since OBMOCKER_snapshot_0.1.0
 */
public class Range<T extends Comparable<? super T>> {

    private final T min;
    private final T max;

    public Range(T min, T max) {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Min value can not be bigger than max value");
        }
        this.min = min;
        this.max = max;
    }

    public boolean contains(T value) {
        return min.compareTo(value) <= 0 && max.compareTo(value) >= 0;
    }

    public T getMin() {
        return this.min;
    }

    public T getMax() {
        return this.max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Range<T> that = (Range<T>) o;
        return this.min.compareTo(that.min) == 0 && this.max.compareTo(that.max) == 0;
    }

    @Override
    public int hashCode() {
        String buffer = this.min.hashCode() + this.max.hashCode() + "";
        return buffer.hashCode();
    }

}
