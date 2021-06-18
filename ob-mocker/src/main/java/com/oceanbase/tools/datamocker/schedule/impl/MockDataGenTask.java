package com.oceanbase.tools.datamocker.schedule.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.core.read.ColumnReader;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.core.task.TableTaskMetaData;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.schedule.AbstractMockTask;
import com.oceanbase.tools.datamocker.util.MockerBuffer;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.extern.slf4j.Slf4j;

/**
 * mock数据承担实际数据生成的任务，主要逻辑是根据生成原语生成数据
 *
 * @author yh263208
 * @date 2021-01-14 11:17
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class MockDataGenTask extends AbstractMockTask {
    /**
     * 缓冲区，用于存放列生成任务产生的数据
     */
    private final MockerBuffer buffer;
    /**
     * 列生成原语集合
     */
    private final List<ColumnReader> readers;
    /**
     * 列约束集合，通过该集合来判断列生成原语生成的数据是否符合约束
     */
    private List<AbstractConstraint> constraints;

    public MockDataGenTask(TableTaskMetaData metaData, TableTaskContext context, MockerBuffer buffer, List<ColumnReader> readers,
            List<AbstractConstraint> constraints) {
        super(metaData, context);
        validateParam(buffer, readers, constraints);
        this.buffer = buffer;
        this.readers = readers;
        this.constraints = new ArrayList<>();
        for (AbstractConstraint constraint : constraints) {
            Set<String> colSet = constraint.columns().get(metaData.getTableName()).keySet();
            for (ColumnReader reader : readers) {
                if (colSet.contains(reader.columnName())) {
                    this.constraints.add(constraint);
                    break;
                }
            }
        }
    }

    /**
     * 验证构造函数的参数是否合法正确
     *
     * @param buffer      线程缓冲
     * @param readers     列生成原语集合
     * @param constraints 约束集合
     * @throws MockerException 验证失败抛出异常
     */
    private void validateParam(MockerBuffer buffer, List<ColumnReader> readers, List<AbstractConstraint> constraints) {
        if (buffer == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Buffer for business task can not be null");
            log.error("Initialization of the data generation task failed because the buffer is null", e);
            throw e;
        }
        if (readers == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Column readers for business task can not be null");
            log.error("Initialization of the data generation task failed because the column reader list is null", e);
            throw e;
        }
        if (constraints == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Constraints for business task can not be null");
            log.error("Initialization of the data generation task failed because the constraint list is null", e);
            throw e;
        }
    }

    @Override
    public Void execute(TableTaskMetaData metaData, TableTaskContext context) throws Exception {
        List<String> columnNames = new ArrayList<>();
        this.readers.forEach(columnReader -> {
            columnNames.add(columnReader.columnName());
        });
        log.info("Start the data generation task, threadName={},columnName={}", Thread.currentThread().getName(),
                String.join(",", columnNames));
        long counter = 0;
        // 循环空转计数器，通常空转超过totalCount还未写入任意一条数据则认为写入异常
        long emptyLoopCount = 0;
        Exception exception = null;
        try {
            while ((counter++) < metaData.getTotalCount() && !Thread.currentThread().isInterrupted()
                   && this.interval() <= metaData.getTimeoutMilliseconds()) {
                Map<String, Pair<AbstractDataType, Object>> columnNameToData = new HashMap<>(this.readers.size());
                for (ColumnReader item : readers) {
                    Pair<String, Pair<AbstractDataType, Object>> pair = item.read();
                    columnNameToData.put(pair.getKey(), pair.getValue());
                }
                boolean passCheck = true;
                emptyLoopCount++;
                for (AbstractConstraint constraint : this.constraints) {
                    if (!constraint.check(columnNameToData)) {
                        passCheck = false;
                        counter--;
                        break;
                    }
                }
                if (passCheck) {
                    emptyLoopCount = 0L;
                    long writeTimeout = metaData.getTimeoutMilliseconds() - this.interval();
                    if (writeTimeout <= 0) {
                        writeTimeout = 2000;
                    }
                    buffer.write(columnNameToData, writeTimeout, TimeUnit.MILLISECONDS);
                    for (AbstractConstraint constraint : this.constraints) {
                        constraint.mark(columnNameToData);
                    }
                }
                if (emptyLoopCount > metaData.getTotalCount() * 100 || emptyLoopCount > 1000000) {
                    throw new MockerException(MockerError.UNKNOWN_ERROR, String.format(
                            "Data generation cycle idling %d exceeds the maximum number of data generation %d, too much data cannot pass "
                            + "the constraint check, data generation is terminated",
                            emptyLoopCount, metaData.getTotalCount() * 100));
                }
            }
            counter--;
        } catch (Exception e) {
            exception = e;
            log.error("Data generation task execution failed", e);
        } finally {
            long writeTimeout = metaData.getTimeoutMilliseconds() - this.interval();
            if (writeTimeout < 0) {
                writeTimeout = 0;
            }
            buffer.close(writeTimeout, TimeUnit.MILLISECONDS);
        }
        if (exception != null) {
            throw exception;
        }
        if (Thread.currentThread().isInterrupted()) {
            log.warn("Data generation task execution is interrupted, totalDataGenerated={}, duration={}ms", counter, interval());
            throw new InterruptedException("data mock business has been interrupted by user");
        }
        if (this.interval() >= metaData.getTimeoutMilliseconds()) {
            log.warn("Data generation task execution timed out, totalDataGenerated={}, duration={}ms", counter,
                    interval());
        }
        if (counter >= metaData.getTotalCount()) {
            log.info("Data generation task is executed successfully, totalDataGenerated={}, duration={}ms", counter,
                    interval());
        }
        context.appendDataGenInfo(counter);
        return null;
    }

    /**
     * 设置约束
     *
     * @param constraints 约束集合
     */
    public void setConstraints(List<AbstractConstraint> constraints) {
        if (constraints != null) {
            int length = this.constraints.size();
            for (int i = 0; i < length; i++) {
                AbstractConstraint constraint = this.constraints.get(i);
                for (AbstractConstraint item : constraints) {
                    if (item.name().equals(constraint.name())) {
                        this.constraints.set(i, item);
                    }
                }
            }
        }
    }
}
