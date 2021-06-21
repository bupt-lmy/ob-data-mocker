package com.oceanbase.tools.datamocker.core.task;

import java.util.Map;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.config.AbstractTableConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import lombok.Getter;

/**
 * Table generation task metadata information
 *
 * @author yh263208
 * @date 2021-01-13 17:35
 * @since OBMOCKER_snapshot_0.1.0
 */
@Getter
public class TableTaskMetaData {
    private final String tableTaskId;
    /**
     * The maximum number of table generation tasks, when a table is generated,
     * totalCount represents the maximum number of entries of data to be generated
     */
    private final Long totalCount;
    /**
     * Table structure definition, used to describe the structure of the table,
     * including the mapping relationship between field names and types
     * key：Column name
     * value：Data type corresponding to column name
     */
    private final Map<String, AbstractDataType> tableSchema;
    private final String tableName;
    private final String schema;
    private final Boolean shouldTruncate;
    private final Long timeoutMilliseconds;
    private final Long batchSize;
    private final ObModeType dialectType;
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
