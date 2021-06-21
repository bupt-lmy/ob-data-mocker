package com.oceanbase.tools.datamocker.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.core.task.AbstractDataPipe;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;

/**
 * The concrete realization class of the data pipeline
 *
 * @author yh263208
 * @date 2021-01-14 19:38
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MockDataPipe extends AbstractDataPipe<Map<String, Pair<AbstractDataType, Object>>> {
    /**
     * Use blocking queues as the underlying implementation of data pipelines
     */
    private final LinkedBlockingQueue<List<Map<String, Pair<AbstractDataType, Object>>>> queue = new LinkedBlockingQueue<>();

    public MockDataPipe(int maxRetained) {
        super(maxRetained);
    }

    @Override
    public void doWrite(List<Map<String, Pair<AbstractDataType, Object>>> row, long timout, TimeUnit timeUnit) throws Exception {
        queue.put(row);
    }

    @Override
    public List<Map<String, Pair<AbstractDataType, Object>>> doRead(long timout, TimeUnit timeUnit) throws Exception {
        if (timout >= 0) {
            return queue.poll(timout, timeUnit);
        }
        return queue.take();
    }

    @Override
    public Long size() {
        synchronized (this.queue) {
            return (long) this.queue.size();
        }
    }
}
