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
package com.oceanbase.tools.datamocker.core.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.tools.datamocker.constraint.Constraint;
import com.oceanbase.tools.datamocker.core.read.ColumnReader;
import com.oceanbase.tools.datamocker.core.write.DataWriter;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * Mock data undertakes the task of actual data generation, and the main logic is to generate data
 * based on the generation primitives
 *
 * @author yh263208
 * @date 2021-01-14 11:17
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class MockDataTask extends AbstractMockTask {

    private final DataWriter dataWriter;
    private final List<ColumnReader<?>> readers;
    private final List<Constraint> constraints;
    private Integer failedWriteBatchCount = 0;

    public MockDataTask(TableTaskMetaData metaData,
            TableTaskContext context,
            @NonNull DataWriter dataWriter,
            @NonNull List<ColumnReader<?>> readers,
            @NonNull List<Constraint> constraints) {
        super(metaData, context);
        this.readers = readers;
        this.dataWriter = dataWriter;
        this.constraints = constraints.stream().filter(c -> {
            Set<String> colSet = c.columns().get(metaData.getTableName()).keySet();
            return readers.stream().anyMatch(r -> colSet.contains(r.getColumnName()));
        }).collect(Collectors.toList());
    }

    @Override
    public void execute(TableTaskMetaData metaData, TableTaskContext context) throws Exception {
        log.info("Mock data task is running... constraintName={}, columnName={}",
                this.constraints.stream().map(Constraint::name).collect(Collectors.toList()),
                this.readers.stream().map(ColumnReader::getColumnName).collect(Collectors.toList()));
        long counter = 0;
        // Cyclic idling counter, usually if idling exceeds totalCount and has not written any data, it is
        // considered that the write is abnormal
        long emptyLoopCount = 0;
        long batchSize = metaData.getBatchSize() * metaData.getConcurrent();
        List<MockRowData> batchList = new ArrayList<>(((Long) batchSize).intValue() + 10);
        try {
            while ((counter++) < metaData.getTotalCount()
                    && !isCancelled()
                    && this.interval() <= metaData.getTimeoutMillis()) {
                if (batchList.size() >= batchSize) {
                    context.accumulateGenerateCountAndGet(batchList.size());
                    long len = writeBatch(batchList, context);
                    batchList = new ArrayList<>(((Long) batchSize).intValue() + 10);
                    if (len < 0) {
                        this.failedWriteBatchCount++;
                    }
                    if (reachMaxErrors(metaData)) {
                        break;
                    }
                }
                MockRowData mockRowData = new MockRowData(this.readers.size());
                for (ColumnReader<?> item : readers) {
                    MockColumnData<?> mockColumn = item.read();
                    mockRowData.addMockColumn(mockColumn);
                }
                boolean passCheck = true;
                emptyLoopCount++;
                for (Constraint constraint : this.constraints) {
                    if (!constraint.check(mockRowData)) {
                        passCheck = false;
                        counter--;
                        break;
                    }
                }
                if (passCheck) {
                    emptyLoopCount = 0L;
                    batchList.add(mockRowData);
                    for (Constraint constraint : this.constraints) {
                        constraint.mark(mockRowData);
                    }
                }
                if (emptyLoopCount > metaData.getTotalCount() * 100 || emptyLoopCount > 1000000) {
                    throw new IllegalStateException(String.format(
                            "Data generation cycle idling %d exceeds the maximum number of data generation %d, too much data cannot pass the constraint check, data generation is terminated",
                            emptyLoopCount, metaData.getTotalCount() * 100));
                }
            }
            counter--;
        } catch (Exception e) {
            log.warn("Mock data task is failed", e);
            throw e;
        } finally {
            try {
                context.accumulateGenerateCountAndGet(batchList.size());
                writeBatch(batchList, context);
            } catch (Exception e) {
                // eat exception
            } finally {
                this.dataWriter.close();
            }
        }
        long writeCount = context.getTotalWriteCountByCurrentThread();
        if (isCancelled()) {
            log.warn("Mock data task is cancelled, totalGenerate={}, totalWrite={}, duration={}", counter,
                    writeCount, DurationFormatUtils.formatDurationHMS(interval()));
            throw new InterruptedException("Mock data task has been interrupted");
        }
        if (this.interval() >= metaData.getTimeoutMillis()) {
            log.warn("Mock data task is timeout, totalGenerate={}, totalWrite={}, duration={}", counter,
                    writeCount, DurationFormatUtils.formatDurationHMS(interval()));
        }
        if (counter >= metaData.getTotalCount()) {
            log.info("Mock data task is succeed, totalGenerate={}, totalWrite={}, duration={}", counter,
                    writeCount, DurationFormatUtils.formatDurationHMS(interval()));
        }
    }

    private long writeBatch(List<MockRowData> batchList, TableTaskContext context) {
        if (this.dataWriter.isClosed()) {
            throw new IllegalStateException("DataWriter has been closed");
        }
        try {
            long len = this.dataWriter.write(batchList);
            if (len > 0) {
                context.accumulateWriteCountAndGet(batchList.size());
            }
            return len;
        } catch (Exception e) {
            context.accumulateWriteCountAndGet(0);
            log.warn("Failed to write a batch, batchListSize={}", batchList.size(), e);
            return -1;
        }
    }

    private boolean reachMaxErrors(TableTaskMetaData metaData) {
        if (metaData.getMaxErrors() <= 0) {
            return false;
        }
        return this.failedWriteBatchCount >= metaData.getMaxErrors();
    }

}
