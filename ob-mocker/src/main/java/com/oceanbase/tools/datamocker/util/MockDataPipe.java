package com.oceanbase.tools.datamocker.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.core.task.AbstractDataPipe;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;

/**
 * 数据管道的具体实现类
 *
 * @author yh263208
 * @date 2021-01-14 19:38
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MockDataPipe extends AbstractDataPipe<Map<String, Pair<AbstractDataType, Object>>> {
    /**
     * 使用阻塞队列作为数据管道的底层实现方式
     */
    private LinkedBlockingQueue<List<Map<String, Pair<AbstractDataType, Object>>>> queue = new LinkedBlockingQueue<>();

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
