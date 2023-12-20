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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.oceanbase.tools.datamocker.core.write.DataWriter;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

/**
 * {@link ConcurrentDataWriter}
 *
 * @author yh263208
 * @date 2023-11-28 21:18
 * @since ODC-release_4.2.3
 */
class ConcurrentDataWriter implements DataWriter {

    @Setter
    private long awaitMillis = 15000;
    private volatile List<MockRowData> targets = null;
    private final List<DataWriter> dataWriters;
    private final Integer threadCount;
    private final Lock writeLock = new ReentrantLock();
    private final Condition notAllThreadsReady = writeLock.newCondition();
    private volatile Integer counter;
    private volatile boolean closed = false;

    public ConcurrentDataWriter(int threadCount, @NonNull List<DataWriter> dataWriters) {
        this.dataWriters = dataWriters;
        this.threadCount = threadCount;
        init();
    }

    @Override
    public long write(List<MockRowData> rows) {
        if (this.closed) {
            throw new IllegalStateException("DataWriter has been closed");
        }
        try {
            if (!this.writeLock.tryLock(awaitMillis, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Failed to get lock for " + awaitMillis + "ms");
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        try {
            if (this.targets == null) {
                this.targets = rows;
            } else {
                if (this.targets.size() != rows.size()) {
                    throw new IllegalStateException(String.format(
                            "Target's size != row's size, %s!=%s", this.targets.size(), rows.size()));
                }
                int len = rows.size();
                for (int i = 0; i < len; i++) {
                    MockRowData row = targets.get(i);
                    rows.get(i).getMockColumns().forEach(c -> writeRow(row, c));
                }
            }
            if (--this.counter > 0) {
                this.notAllThreadsReady.await(awaitMillis, TimeUnit.MILLISECONDS);
                return 0;
            } else {
                this.counter = this.threadCount;
                this.notAllThreadsReady.signalAll();
                return write();
            }
        } catch (Exception e) {
            init();
            throw new IllegalStateException(e);
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (isClosed()) {
            return;
        }
        this.closed = true;
        this.dataWriters.forEach(dataWriter -> {
            try {
                dataWriter.close();
            } catch (Exception e) {
                // eat exception
            }
        });
    }

    private void writeRow(MockRowData row, MockColumnData<?> target) {
        String columName = target.getColumnName();
        if (row.getMockColumn(columName) != null) {
            throw new IllegalArgumentException(String.format("Custom column \"%s\" is duplicate", columName));
        }
        row.addMockColumn(target);
    }

    private long write() {
        if (CollectionUtils.isEmpty(targets)) {
            return 0;
        }
        try {
            AtomicInteger errCounter = new AtomicInteger(0);
            long total = this.dataWriters.stream().map(dataWriter -> {
                try {
                    return dataWriter.write(targets);
                } catch (Exception e) {
                    errCounter.incrementAndGet();
                    return 0L;
                }
            }).mapToLong(value -> value).sum();
            if (errCounter.intValue() > 0) {
                return errCounter.intValue() * -1;
            }
            return total;
        } finally {
            this.targets = null;
        }
    }

    private void init() {
        if (!this.writeLock.tryLock()) {
            throw new IllegalStateException("Failed to init, reason: failed to lock");
        }
        try {
            this.counter = this.threadCount;
            this.targets = null;
            this.notAllThreadsReady.signalAll();
        } finally {
            this.writeLock.unlock();
        }
    }

}
