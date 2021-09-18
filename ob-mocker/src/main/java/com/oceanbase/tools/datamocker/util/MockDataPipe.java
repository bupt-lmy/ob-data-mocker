package com.oceanbase.tools.datamocker.util;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.core.task.AbstractDataPipe;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;

/**
 * The concrete realization class of the data pipeline
 *
 * @author yh263208
 * @date 2021-01-14 19:38
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MockDataPipe extends AbstractDataPipe<List<MockRowData>> {
    /**
     * Use blocking queues as the underlying implementation of data pipelines
     */
    private final LinkedBlockingQueue<List<MockRowData>> queue = new LinkedBlockingQueue<>();

    public MockDataPipe(int maxRetained) {
        super(maxRetained);
    }

    @Override
    public void doWrite(List<MockRowData> row, long timout, TimeUnit timeUnit) throws InterruptedException {
        queue.put(row);
    }

    @Override
    public List<MockRowData> doRead(long timout, TimeUnit timeUnit) throws InterruptedException {
        return queue.poll();
    }

    @Override
    public long size() {
        synchronized (this.queue) {
            return this.queue.size();
        }
    }
}
