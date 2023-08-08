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

import java.sql.Date;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.generator.BaseDateGenerator;

/**
 * Random date data generator
 *
 * @author yh263208
 * @date 2020-12-16 16:09
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RandomDateGenerator extends BaseDateGenerator<Date> {

    @Override
    protected Boolean doPreCheck(Date startTime, Date endTime, int scale, TimeUnit minTimeUnit) {
        return true;
    }

    @Override
    protected Date doGenerate(Date startTime, Date endTime, int scale, TimeUnit minTimeUnit) {
        long timstamp = (long) (Math.random() * (endTime.getTime() - startTime.getTime()) + startTime.getTime());
        return new Date(timstamp);
    }

    @Override
    protected Long doCount(Date startTime, Date endTime, int scale, TimeUnit minTimeUnit) {
        long interval = endTime.getTime() - startTime.getTime();
        return minTimeUnit.convert(interval, TimeUnit.MILLISECONDS);
    }

}
