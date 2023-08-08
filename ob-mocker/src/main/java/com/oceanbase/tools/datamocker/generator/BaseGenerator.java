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
package com.oceanbase.tools.datamocker.generator;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.Setter;

/**
 * Base class data generation object interface, used to define the data generation method
 *
 * @author yh263208
 * @date 2020-12-11 21:16
 * @since OBMOCKER_snapshot_0.1.0
 */
@Setter
public abstract class BaseGenerator<T extends Comparable<? super T>, V> {
    /**
     * Default value for data generator
     */
    private V defaultValue;
    /**
     * Whether allow null
     */
    private Boolean allowNull = Boolean.FALSE;

    /**
     * Pre-checking step, used to check whether the generator can work normally according to the
     * boundary value
     *
     * @param leftLimit Left boundary value
     * @param rightLimit Right boundary value
     * @return Return the verification result
     */
    public abstract Boolean preCheck(T leftLimit, T rightLimit);

    /**
     * Get generated data
     *
     * @param leftLimit The left boundary value has slightly different meanings for different types of
     *        data generators. For digital generation tasks, it reflects the minimum value of the
     *        generated numbers. If it is a character generation task, it reflects the minimum value of
     *        characters.
     * @param rightLimit The right boundary value has slightly different meanings for different types of
     *        data generators. For digital generation tasks, it reflects the minimum value of the
     *        generated numbers. If it is a character generation task, it reflects the minimum value of
     *        characters.
     * @return Returns a generated specific value
     */
    abstract protected V generate(T leftLimit, T rightLimit);

    /**
     * Obtain the generated data method
     *
     * @param leftLimit The left boundary value has slightly different meanings for different types of
     *        data generators. For digital generation tasks, it reflects the minimum value of the
     *        generated numbers. If it is a character generation task, it reflects the minimum value of
     *        characters.
     * @param rightLimit The right boundary value has slightly different meanings for different types of
     *        data generators. For digital generation tasks, it reflects the minimum value of the
     *        generated numbers. If it is a character generation task, it reflects the minimum value of
     *        characters.
     * @return Returns a generated specific value
     */
    public V next(T leftLimit, T rightLimit) {
        int loopCount = 0;
        while (true) {
            V returnVal = generate(leftLimit, rightLimit);
            if (returnVal == null) {
                if (this.allowNull) {
                    return null;
                } else if (this.defaultValue != null) {
                    return this.defaultValue;
                }
                if ((loopCount++) >= 100) {
                    throw new MockerException(MockerError.OPERATION_FAILURE, "Data generator get too much null data");
                }
            } else {
                return returnVal;
            }
        }
    }

    /**
     * Return the total number of unique data that the data generator can generate
     *
     * @param leftLimit Left boundary value
     * @param rightLimit Right boundary value
     * @return Return a specific value, or null if the data generator can generate data without
     *         limitation
     */
    abstract public Long count(T leftLimit, T rightLimit);
}
