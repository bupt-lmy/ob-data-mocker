package com.oceanbase.tools.datamocker.schedule.impl;

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.core.task.TableTaskMetaData;
import com.oceanbase.tools.datamocker.core.write.AbstractMockWriter;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.schedule.AbstractMockTask;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.extern.slf4j.Slf4j;

/**
 * Mock data output task, used to write data to the defined output source
 *
 * @author yh263208
 * @date 2021-01-17 15:32
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class MockDataOutputTask extends AbstractMockTask {
    /**
     * Output primitive
     */
    private final List<AbstractMockWriter> writers;
    /**
     * Mark the data to write out the flag bit whether the primitive is writable
     */
    private final List<Boolean> writerSymbols;

    public MockDataOutputTask(TableTaskMetaData metaData, TableTaskContext context, List<AbstractMockWriter> writers) {
        super(metaData, context);
        if (writers == null || writers.size() == 0) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Data writer can not be null or empty");
            log.error("Initialization of data output task failed because the mocker writers is null or empty", e);
            throw e;
        }
        this.writers = writers;
        this.writerSymbols = new ArrayList<>(this.writers.size());
        for (int i = 0; i < writers.size(); i++) {
            this.writerSymbols.add(Boolean.TRUE);
        }
    }

    @Override
    public Void execute(TableTaskMetaData metaData, TableTaskContext context) throws Exception {
        log.info("Start the data output task, threadName={}", Thread.currentThread().getName());
        int length = this.writerSymbols.size();
        Throwable exception = null;
        while (!Thread.currentThread().isInterrupted() && this.interval() <= metaData.getTimeoutMilliseconds()) {
            try {
                Boolean shouldBreak = Boolean.FALSE;
                for (int i = 0; i < length; i++) {
                    if (this.writerSymbols.get(i)) {
                        AbstractMockWriter writer = this.writers.get(i);
                        Long writeCounter = writer.write();
                        if (writeCounter == null) {
                            this.writerSymbols.set(i, Boolean.FALSE);
                        } else {
                            context.appendWriteInfo(new Pair<>(writer.groupId(), writeCounter));
                        }
                    }
                    for (Boolean item : this.writerSymbols) {
                        shouldBreak |= item;
                    }
                }
                if (!shouldBreak) {
                    break;
                }
            } catch (Throwable e) {
                exception = e;
                log.error("Data output task execution failed", e);
                if (e instanceof InterruptedException || MockTaskStatus.CANCELED.equals(context.getStatus())) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (exception != null) {
            throw new Exception(exception);
        }
        if (this.interval() >= metaData.getTimeoutMilliseconds()) {
            log.warn("Data output task execution timed out, duration={}ms", interval());
        }
        if (Thread.currentThread().isInterrupted()) {
            log.warn("Data output task execution is interrupted, duration={}ms", interval());
            throw new InterruptedException("data mock write thread has been interrupted by user");
        }
        if (this.interval() <= metaData.getTimeoutMilliseconds()) {
            log.info("Data output task is executed successfully, duration={}ms", interval());
        }
        return null;
    }
}
