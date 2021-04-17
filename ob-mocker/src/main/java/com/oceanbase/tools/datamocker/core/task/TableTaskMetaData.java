package com.oceanbase.tools.datamocker.core.task;

import java.util.Map;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.config.AbstractTableConfig;
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
    private final String tableTaskId;
    /**
     * 表生成任务的最大数量，当生成一张表时totalCount代表要生成数据的最大条目数
     */
    private final Long totalCount;
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
    private final Long timeoutMilliseconds;
    /**
     * 批处理大小
     */
    private final Long batchSize;
    /**
     * 方言类型
     */
    private final ObModeType dialectType;
    /**
     * 任务id
     */
    private final String taskId;

    public TableTaskMetaData(Map<String, AbstractDataType> tableSchema, AbstractTableConfig tableConfig, ObModeType dialectType,
            String taskId, int columnIndex, int rowIndex) {
        this.tableSchema = tableSchema;
        this.tableName = tableConfig.tableName();
        this.schema = tableConfig.schemaName();
        this.shouldTruncate = tableConfig.truncated();
        this.timeoutMilliseconds = tableConfig.timeoutMilliseconds();
        this.batchSize = tableConfig.maxBatchSize();
        this.totalCount = tableConfig.maxCount();
        this.tableTaskId = taskId + "-[" + columnIndex + "," + rowIndex + "]";
        this.dialectType = dialectType;
        this.taskId = taskId;
    }
}
