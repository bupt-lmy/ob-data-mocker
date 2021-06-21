package com.oceanbase.tools.datamocker.model.config.impl;

import java.util.List;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.model.config.AbstractTableConfig;
import com.oceanbase.tools.datamocker.model.enums.DuplicateStrategy;
import com.oceanbase.tools.datamocker.model.enums.ScriptType;
import lombok.Getter;
import lombok.Setter;

/**
 * Table task configuration object, used to encapsulate configuration parameters related to table
 * generation tasks
 *
 * @author yh263208
 * @date 2020-12-27 20:58
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class DefaultTableConfig extends AbstractTableConfig {
    /**
     * Column configuration collection
     */
    private List<DefaultColumnConfig> columns;
    /**
     * Maximum number of generations
     */
    private Long totalCount;
    /**
     * Handling strategy in case of conflict (abandoned)
     */
    private DuplicateStrategy strategy;
    /**
     * Batch size
     */
    private Long batchSize;
    /**
     * Whether to empty the table
     */
    private Boolean whetherTruncate;
    /**
     * The name of the table to be inserted
     */
    private String tableName;
    /**
     * The schema name of the database
     */
    private String schemaName;
    /**
     * The timeout period of the table generation task, in milliseconds, the default timeout period is 1
     * hour, which is 3600000
     */
    private Long timeout = 3600000L;
    /**
     * Data write address
     */
    private String location;
    /**
     * Maximum number of retention, set the maximum number of batch data retention in the memory
     */
    private int maxRetainedCount = -1;

    @Override
    protected Long maxRowCount() {
        return totalCount;
    }

    @Override
    public DuplicateStrategy duplicateStrategy() {
        return strategy;
    }

    @Override
    protected Long batchSize() {
        return batchSize;
    }

    @Override
    public Boolean truncated() {
        return whetherTruncate;
    }

    @Override
    public String tableName() {
        return tableName;
    }

    @Override
    public List<DefaultColumnConfig> columns() {
        return columns;
    }

    @Override
    public String schemaName() {
        return schemaName;
    }

    @Override
    public Long timeoutMilliseconds() {
        return timeout;
    }

    @Override
    public ScriptType[] scriptType() {
        ScriptType[] scriptTypes = new ScriptType[] {
                ScriptType.SQL
        };
        return scriptTypes;
    }

    @Override
    public String dataWriteLocation(ScriptType scriptType) {
        return location;
    }

    @Override
    public List<AbstractConstraint> constraints() {
        return null;
    }

    @Override
    public int maxRetainedCount() {
        return maxRetainedCount;
    }
}
