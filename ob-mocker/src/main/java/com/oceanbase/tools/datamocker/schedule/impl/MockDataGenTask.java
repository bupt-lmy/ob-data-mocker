package com.oceanbase.tools.datamocker.schedule.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.core.read.ColumnReader;
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
public class MockDataGenTask extends AbstractMockTask<Long> {
    /**
     * 缓冲区，用于存放列生成任务产生的数据
     */
    private final MockerBuffer buffer;
    /**
     * 任务是否成功状态标志
     */
    private Boolean result = false;
    /**
     * 列生成原语集合
     */
    private final List<ColumnReader> readers;
    /**
     * 列约束集合，通过该集合来判断列生成原语生成的数据是否符合约束
     */
    private List<AbstractConstraint> constraints;

    public MockDataGenTask(TableTaskMetaData metaData, MockerBuffer buffer, List<ColumnReader> readers,
            List<AbstractConstraint> constraints) {
        super(metaData);
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
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "buffer for business task can not be null");
            log.error("errors happen when init mock business task", e);
            throw e;
        }
        if (readers == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "column readers for business task can not be null");
            log.error("errors happen when init mock business task", e);
            throw e;
        }
        if (constraints == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "constraints for business task can not be null");
            log.error("errors happen when init mock business task", e);
            throw e;
        }
    }

    @Override
    protected boolean isTaskSuccess() {
        return this.result;
    }

    @Override
    public Long execute(TableTaskMetaData metaData) {
        Long counter = 0L;
        boolean exception = false;
        // 循环空转计数器，通常空转超过totalCount还未写入任意一条数据则认为写入异常
        Long emptyLoopCount = 0L;
        try {
            while ((counter++) < metaData.getTotalCount() && !Thread.currentThread().isInterrupted()
                   && this.interval() <= metaData.getTimeout()) {
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
                    buffer.write(columnNameToData);
                    for (AbstractConstraint constraint : this.constraints) {
                        constraint.mark(columnNameToData);
                    }
                }
                if (emptyLoopCount > metaData.getTotalCount() * 100 || emptyLoopCount > 1000000) {
                    throw new MockerException(MockerError.UNKNOWN_ERROR, String.format(
                            "data generation cycle idling %d exceeds the maximum number of data generation %d, too much data cannot pass "
                            + "the constraint check, data generation is terminated",
                            emptyLoopCount, metaData.getTotalCount() * 100));
                }
            }
            counter--;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                if (!Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                }
            }
            exception = true;
            log.error("some errors occured when mocking data", e);
        }
        if (Thread.currentThread().isInterrupted()) {
            log.warn("data mock business thread has been interrupted, {} data have been generated, run {}ms", counter,
                    System.currentTimeMillis() - this.startTime());
        }
        if (this.interval() >= metaData.getTimeout()) {
            log.warn("data mock business thread has been terminated cause timeout, {} data have been generated, run {}ms", counter,
                    System.currentTimeMillis() - this.startTime());
        }
        if (counter >= metaData.getTotalCount()) {
            log.info("data mock business thread has been executed successfully, {} data have been generated, run {}ms", counter,
                    System.currentTimeMillis() - this.startTime());
            if (!exception) {
                this.result = true;
            }
        }
        try {
            buffer.close();
        } catch (Exception e) {
            log.error("fail to close mock buffer", e);
            this.result = false;
        }
        return counter;
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
