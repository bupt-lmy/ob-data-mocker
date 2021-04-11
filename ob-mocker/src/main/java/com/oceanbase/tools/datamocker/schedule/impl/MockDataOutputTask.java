package com.oceanbase.tools.datamocker.schedule.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.core.task.TableTaskMetaData;
import com.oceanbase.tools.datamocker.core.write.AbstractMockWriter;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.schedule.AbstractMockTask;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.extern.slf4j.Slf4j;

/**
 * mock数据输出任务，用于将数据写出到定义的输出源中
 *
 * @author yh263208
 * @date 2021-01-17 15:32
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class MockDataOutputTask extends AbstractMockTask<Map<String, Long>> {
    /**
     * 输出原语
     */
    private final List<AbstractMockWriter> writers;
    /**
     * 标记数据写出原语是否可写的标志位
     */
    private final List<Boolean> writerSymbols;
    /**
     * 任务是否成功标志
     */
    private Boolean isSuccess = Boolean.FALSE;

    public MockDataOutputTask(TableTaskMetaData metaData, TableTaskContext context, List<AbstractMockWriter> writers) {
        super(metaData, context);
        if (writers == null || writers.size() == 0) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "data writer can not be null or empty");
            log.error("some errors occured when init mock data output task", e);
            throw e;
        }
        this.writers = writers;
        this.writerSymbols = new ArrayList<>(this.writers.size());
        for (int i = 0; i < writers.size(); i++) {
            this.writerSymbols.add(Boolean.TRUE);
        }
    }

    @Override
    protected boolean isTaskSuccess() {
        return this.isSuccess;
    }

    @Override
    public Map<String, Long> execute(TableTaskMetaData metaData, TableTaskContext context) {
        int length = this.writerSymbols.size();
        Map<String, Long> returnVal = new HashMap<>();
        for (AbstractMockWriter writer : this.writers) {
            returnVal.put(writer.groupId(), 0L);
        }
        boolean exception = false;
        while (!Thread.currentThread().isInterrupted() && this.interval() <= metaData.getTimeout()) {
            try {
                Boolean shouldBreak = Boolean.FALSE;
                for (int i = 0; i < length; i++) {
                    if (this.writerSymbols.get(i)) {
                        AbstractMockWriter writer = this.writers.get(i);
                        Long writeCounter = writer.write();
                        if (writeCounter == null) {
                            this.writerSymbols.set(i, Boolean.FALSE);
                        } else {
                            Long value = returnVal.get(writer.groupId());
                            context.appendWriteInfo(new Pair<>(writer.groupId(), writeCounter));
                            returnVal.put(writer.groupId(), value + writeCounter);
                        }
                    }
                    for (Boolean item : this.writerSymbols) {
                        shouldBreak |= item;
                    }
                }
                if (!shouldBreak) {
                    break;
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    if (!Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                    }
                }
                exception = true;
                log.error("some errors occured when write data", e);
            }
        }
        if (this.interval() >= metaData.getTimeout()) {
            log.warn("data mock write thread has been terminated cause timeout, run {}ms", System.currentTimeMillis() - this.startTime());
        }
        if (Thread.currentThread().isInterrupted()) {
            log.warn("data mock write thread has been interrupted, run {}ms", System.currentTimeMillis() - this.startTime());
        } else if (this.interval() <= metaData.getTimeout()) {
            if (!exception) {
                this.isSuccess = Boolean.TRUE;
            }
            log.info("data mock write thread has been executed successfully, run {}ms", System.currentTimeMillis() - this.startTime());
        }
        return returnVal;
    }
}
