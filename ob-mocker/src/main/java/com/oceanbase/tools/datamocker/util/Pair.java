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

import java.util.UUID;

/**
 * A custom Java object is used to represent the Java pairing. The Pair provided by jfxrt.jar does
 * not exist on many versions of jdk, causing great compilation difficulties
 *
 * @author yh263208
 * @date 2020-12-31 17:50
 * @since OBMOCKER_snapshot_0.1.0
 */
public class Pair<T, V> {
    private final String uniqueId = UUID.randomUUID().toString();
    private final T key;
    private final V value;

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
        return String.format("Pair(\"%s\", \"%s\")", this.key, this.value);
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
