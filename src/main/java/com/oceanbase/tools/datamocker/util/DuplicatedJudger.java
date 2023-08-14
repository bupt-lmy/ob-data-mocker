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

import java.util.HashSet;
import java.util.Set;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Tool class, used to determine whether a value has repeatedly appeared
 *
 * @author yh263208
 * @date 2021-01-10 22:56
 * @since OBMOCKER_0.1.0_snapshot
 */
public class DuplicatedJudger {
    /**
     * Set, used for judging whether it is repeated when the amount of data is small
     */
    private Set<Object> set;
    /**
     * Bitmap, used to judge whether the data is duplicated when there is a large amount of data
     */
    private BitMap bitMap;
    /**
     * The maximum number that can be carried
     */
    private final int maxCount;
    /**
     * Current cursor
     */
    private int cursor;

    public DuplicatedJudger(int count) {
        if (count <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Count for DuplicatedJudger can not be equal to or smaleer than zero");
        }
        if (count < 10000) {
            this.set = new HashSet<>();
        } else {
            this.bitMap = new BitMap(count);
        }
        this.maxCount = count;
        this.cursor = 0;
    }

    public boolean contains(Object obj) {
        if (this.set != null) {
            return this.set.contains(obj);
        } else {
            return this.bitMap.contains(obj);
        }
    }

    public boolean add(Object obj) {
        if (++this.cursor > this.maxCount) {
            throw new MockerException(MockerError.OPERATION_FAILURE,
                    String.format("The max count for DuplicatedJudger is %d, can not add more", this.maxCount));
        }
        if (this.set != null) {
            return this.set.add(obj);
        } else {
            return this.bitMap.add(obj);
        }
    }

    public void clear() {
        this.cursor = 0;
        if (this.set != null) {
            this.set.clear();
        } else {
            this.bitMap.clear();
        }
    }
}
