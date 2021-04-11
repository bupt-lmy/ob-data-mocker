package com.oceanbase.tools.datamocker.core.task;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.Getter;
import lombok.Setter;

/**
 * mock数据的上下文，也是操作mock数据任务的句柄
 *
 * @author yh263208
 * @date 2021-01-18 11:14
 * @since OBMOCKER_snapshot_0.1.0
 */
public class TableTaskContext {
    /**
     * 任务名称
     */
    @Getter
    private final String taskName;
    /**
     * 任务ID
     */
    @Getter
    private final String taskId;
    /**
     * 批处理大小
     */
    @Getter
    private final Long batchSize;
    /**
     * mock数据当前的任务状态
     */
    @Getter
    @Setter
    private MockTaskStatus status;
    /**
     * 一共要生成的数据量
     */
    @Getter
    private final Long totalCount;
    /**
     * 表结构定义，用于描述表的结构，包括各字段名和类型的映射关系
     */
    @Getter
    private final Map<String, AbstractDataType> tableSchema;
    /**
     * 表名
     */
    @Getter
    private final String tableName;
    /**
     * 表所在的schema
     */
    @Getter
    private final String schema;
    /**
     * 是否清空表
     */
    @Getter
    private final Boolean truncate;
    /**
     * 超时时间
     */
    @Getter
    private final Long timeoutSeconds;
    /**
     * 句柄集合，用于控制线程任务
     */
    private List<Future> handlers;
    /**
     * 数据写出统计信息
     * 这里的Key代表的是不同的输出源的名称：例如写DB的输出元名称和写文件的输出源名称
     * 这里的Value代表输出源写出的数据量
     */
    @Getter
    private Map<String, Long> writerName2writeCount;
    /**
     * 数据生成统计信息
     */
    @Getter
    private Long totalDataGenerateCount = null;
    /**
     * 当前数据库表中的记录数目
     */
    @Getter
    @Setter
    private Long currentRecordNum;
    /**
     * 数据源头
     */
    @Getter
    private final DataSource dataSource;
    /**
     * 文件管理器
     */
    @Getter
    private final List<MockerFile> fileManagers;
    /**
     * 方言类型
     */
    @Getter
    private final ObModeType dialectType;
    /**
     * 顶部指针索引
     */
    @Getter
    private final int topIndex;

    public TableTaskContext(TableTaskInfo taskInfo, String taskName, int index) {
        TableTaskMetaData metaData = taskInfo.getMetaData();
        this.taskId = metaData.getTableTaskId();
        this.taskName = taskName;
        this.batchSize = metaData.getBatchSize();
        this.totalCount = metaData.getTotalCount();
        this.tableSchema = metaData.getTableSchema();
        this.tableName = metaData.getTableName();
        this.schema = metaData.getSchema();
        this.truncate = metaData.getShouldTruncate();
        this.timeoutSeconds = metaData.getTimeout();
        this.status = MockTaskStatus.CREATED;
        this.handlers = new LinkedList<>();
        this.writerName2writeCount = new HashMap<>();
        this.dataSource = taskInfo.getDataSource();
        this.fileManagers = taskInfo.getFileManagers();
        this.dialectType = metaData.getDialectType();
        this.topIndex = index;
    }

    /**
     * 增加一个句柄
     *
     * @param handle 具体的句柄对象
     */
    public void appendHandle(Future handle) {
        if (handle == null) {
            return;
        }
        if (MockTaskStatus.RUNNING.equals(this.status)) {
            this.handlers.add(handle);
        }
    }

    /**
     * 终止任务的执行
     *
     * @return 返回关闭的结果
     */
    public Boolean shutdown() {
        Boolean returnVal = Boolean.TRUE;
        this.status = MockTaskStatus.CANCELED;
        for (Future task : this.handlers) {
            if (!task.isCancelled() && !task.isDone()) {
                returnVal &= task.cancel(true);
            }
        }
        return returnVal;
    }

    /**
     * 追加一条写原语的统计信息
     *
     * @param result 统计结果
     */
    public synchronized void appendWriteInfo(Pair<String, Long> result) {
        if (result == null || result.getKey() == null || result.getValue() == null) {
            return;
        }
        Long value = this.writerName2writeCount.getOrDefault(result.getKey(), 0L);
        this.writerName2writeCount.put(result.getKey(), value + result.getValue());
    }

    /**
     * 追加一条数据生成原语的统计信息
     *
     * @param result 统计结果
     * @throws MockerException 所有的数据生成原语都必须产生相同数量的数据，如果违反则报错
     */
    public synchronized void appendDataGenInfo(Long result) {
        if (result == null) {
            return;
        }
        if (this.totalDataGenerateCount == null) {
            this.totalDataGenerateCount = result;
        } else {
            if (!this.totalDataGenerateCount.equals(result)) {
                throw new MockerException(MockerError.OPERATION_FAILURE, "all column readers have to generate same number of data");
            }
        }
    }

    /**
     * get table task progress
     *
     * @return progress of task
     */
    public double getProgress() {
        if (totalCount != 0 && writerName2writeCount.size() != 0) {
            Collection<Long> values = writerName2writeCount.values();
            Iterator<Long> iter = values.iterator();
            double totalProgress = 0.0;
            while (iter.hasNext()) {
                Long value = iter.next();
                totalProgress += value.doubleValue() / totalCount;
            }
            return totalProgress / writerName2writeCount.size();
        }
        return 0.0;
    }
}
