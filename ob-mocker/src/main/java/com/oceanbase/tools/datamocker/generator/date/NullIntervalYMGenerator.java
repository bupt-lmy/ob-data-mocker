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
package com.oceanbase.tools.datamocker.generator.date;

import com.oceanbase.jdbc.extend.datatype.INTERVALYM;

import com.oceanbase.tools.datamocker.generator.BaseGenerator;

/**
 * Null data generator of interval year to month type
 *
 * @author yh263208
 * @date 2021-01-26 14:42
 * @since OBMOCKER_snapshot_0.1.0
 */
public class NullIntervalYMGenerator extends BaseGenerator<Integer, INTERVALYM> {

    @Override
    public Boolean preCheck(Integer leftLimit, Integer rightLimit) {
        return true;
    }

    @Override
    protected INTERVALYM generate(Integer leftLimit, Integer rightLimit) {
        return null;
    }

    @Override
    public Long count(Integer leftLimit, Integer rightLimit) {
        return 1L;
    }
}
