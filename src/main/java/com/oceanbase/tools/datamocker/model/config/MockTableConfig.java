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
package com.oceanbase.tools.datamocker.model.config;

import java.util.List;

import com.oceanbase.tools.datamocker.model.enums.DuplicateStrategy;
import lombok.Getter;
import lombok.Setter;

/**
 * Table task configuration object, used to encapsulate configuration parameters related to table
 * generation tasks
 *
 * @author yh263208
 * @date 2020-12-27 20:58
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class MockTableConfig {

    private Long totalCount;
    private DuplicateStrategy strategy;
    private Long batchSize;
    private Boolean whetherTruncate;
    private String tableName;
    private String schemaName;
    private Long maxSingleFileSizeInBytes = 200 * 1024 * 1024L;
    private Long timeoutMillis = 3600000L;
    private String outputDir;
    private int maxErrors = 0;
    private Integer concurrent;
    private List<MockColumnConfig> columns;

    public Long getTotalCount() {
        if (this.totalCount == null) {
            throw new IllegalArgumentException("Max count can not be null");
        }
        if (this.totalCount < 0) {
            throw new IllegalArgumentException("Max count can not be smaller than 0");
        }
        return this.totalCount;
    }

    public Long getMaxBatchSize() {
        if (this.batchSize == null) {
            throw new IllegalArgumentException("Batch size can not be null");
        }
        if (this.batchSize < 0) {
            throw new IllegalArgumentException("Batch size can not be smaller than 0");
        } else if (this.batchSize > 100000) {
            throw new IllegalArgumentException("Batch size can not be bigger than 100000");
        }
        return this.batchSize;
    }

    public Integer getConcurrent() {
        if (this.concurrent == null || this.concurrent <= 0) {
            return 1;
        }
        long epoch = this.concurrent * this.batchSize;
        if (epoch < 0 || epoch >= this.totalCount) {
            return Long.valueOf(this.totalCount / this.batchSize).intValue();
        }
        return this.concurrent;
    }

}
