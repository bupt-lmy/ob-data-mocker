package com.oceanbase.tools.datamocker.core.task;

import java.util.Map;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.enums.DuplicateStrategy;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import lombok.Getter;

/**
 * 表生成任务元数据信息
 *
 * @author yh263208
 * @date 2021-01-13 17:35
 * @since OBMOCKER_snapshot_0.1.0
 */
@Getter
public class TableTaskMetaData {
    /**
     * 任务ID
     */
    private final String taskId;
    /**
     * 表生成任务的最大数量，当生成一张表时totalCount代表要生成数据的最大条目数
     */
    private final Long totalCount;
    /**
     * 方言类型
     */
    private final ObModeType dialectType;
    /**
     * 表结构定义，用于描述表的结构，包括各字段名和类型的映射关系
     *
     * key：列名
     * value：列名对应的数据类型
     */
    private final Map<String, AbstractDataType> tableSchema;
    /**
     * 表名
     */
    private final String tableName;
    /**
     * 表所在的schema
     */
    private final String schema;
    /**
     * 是否清空表
     */
    private final Boolean shouldTruncate;
    /**
     * 超时时间
     */
    private final Long timeout;
    /**
     * 批处理大小
     */
    private final Long batchSize;
    /**
     * 数据冲突时的策略
     */
    private final DuplicateStrategy strategy;

    public TableTaskMetaData(Map<String, AbstractDataType> tableSchema, String tableName, String schema, Boolean truncate, Long timeout,
            Long batchSize, DuplicateStrategy strategy, ObModeType dialectType, Long totalCount, String taskId) {
        this.tableSchema = tableSchema;
        this.tableName = tableName;
        this.schema = schema;
        this.shouldTruncate = truncate;
        this.timeout = timeout;
        this.batchSize = batchSize;
        this.strategy = strategy;
        this.dialectType = dialectType;
        this.totalCount = totalCount;
        this.taskId = taskId;
    }
}
