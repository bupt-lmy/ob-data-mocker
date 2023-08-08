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

/**
 * Byte array object type data generator
 *
 * @author yh263208
 * @date 2020-12-16 22:39
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class BaseByteGenerator extends BaseGenerator<Integer, byte[]> {
    /**
     * Pre-checking step, used to check whether the generator can work normally according to the
     * boundary value
     *
     * @param minLength Minimum
     * @param maxLength Max length for string value
     * @return Return the verification result
     */
    @Override
    abstract public Boolean preCheck(Integer minLength, Integer maxLength);

    /**
     * Data generation method interface
     *
     * @param minLength The minimum value, the character generation task reflects the minimum byte value
     *        of the character
     * @param maxLength The maximum value, the character generation task reflects the minimum byte value
     *        of the character
     * @return Returns a generated specific value
     */
    @Override
    abstract public byte[] generate(Integer minLength, Integer maxLength);

    /**
     * Return the total number of unique data that the data generator can generate
     *
     * @return Return a specific value, or null if the data generator can generate data without
     *         limitation
     */
    @Override
    abstract public Long count(Integer minLength, Integer maxLength);
}
