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
package com.oceanbase.tools.datamocker.core.read;

import com.oceanbase.tools.datamocker.model.mock.MockColumnData;

/**
 * Data reader, you can read data from this reader
 *
 * @author yh263208
 * @date 20201-01-14 14:14
 * @since OBMOCKER_0.1.0_snapshot
 */
public abstract class AbstractMockReader<T> {
    /**
     * Read method, you can read data from this method
     *
     * @return data which is read
     * @throws Exception exception will be thrown when error occured
     */
    abstract public MockColumnData<T> read() throws Exception;

    /**
     * Get the groupId of the primitive. The primitive is an atomic part of an overall operation, so a
     * groupId is needed to identify which primitives belong to the same overall operation
     *
     * @return group id string value
     */
    abstract public String groupId();
}
