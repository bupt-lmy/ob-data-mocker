package com.oceanbase.tools.datamocker.model.config;

import java.util.List;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.model.enums.DuplicateStrategy;
import com.oceanbase.tools.datamocker.model.enums.ScriptType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * The configuration object of Mock data,
 * the key parameters of Mock data are configured in the configuration object
 *
 * @author yh263208
 * @date 2020-12-22 20:40
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class AbstractTableConfig {
    /**
     * The maximum number of generations, must be an integer greater than 0,
     * the maximum limit is 100000
     *
     * @return 返回数量
     */
    abstract protected Long maxRowCount();

    /**
     * Get the maximum number of data generation tasks.
     * Here, the maximum number of generations passed in by the user is checked.
     * Only a value between 0-1000000 is allowed to be passed in.
     *
     * @return Returns the maximum number of generations
     */
    public Long maxCount() {
        if (maxRowCount() == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Max count can not be null");
        }
        if (maxRowCount() < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Max count can not be smaller than zero");
        } else if (maxRowCount() > 1000000) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Max count can not be bigger than 1000000");
        }
        return maxRowCount();
    }

    /**
     * How to deal with conflicts, return an enumeration value
     *
     * @return Returns an enumeration value
     */
    @Deprecated
    abstract public DuplicateStrategy duplicateStrategy();

    /**
     * Batch size, supports a value of 0-100000
     *
     * @return Return batch size
     */
    abstract protected Long batchSize();

    /**
     * Check the batch size value
     *
     * @return Return batch size
     */
    public Long maxBatchSize() {
        if (batchSize() == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Batch size can not be null");
        }
        if (batchSize() < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Batch size can not be smaller than zero");
        } else if (batchSize() > 100000) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Batch size can not be bigger than 100000");
        }
        return batchSize();
    }

    /**
     * Whether to empty the table
     *
     * @return Returns a boolean value
     */
    abstract public Boolean truncated();

    /**
     * Returns the name of the table to be mocked
     *
     * @return Return the name of the table
     */
    abstract public String tableName();

    /**
     * Get a list of tasks
     *
     * @return Return to the list of tasks
     */
    abstract public List<? extends AbstractColumnConfig> columns();

    /**
     * Return the schema name of the table task
     *
     * @return Return the schema name
     */
    abstract public String schemaName();

    /**
     * Timeout period of table generation task
     *
     * @return Return timeout
     */
    abstract public Long timeoutMilliseconds();

    /**
     * Script type enumeration, mock data can be defined to write script type,
     * multiple scripts can be output
     *
     * @return Returns the script type array
     */
    abstract public ScriptType[] scriptType();

    /**
     * Data write address
     *
     * @param scriptType Script type
     * @return Return the data write address of the corresponding script
     */
    abstract public String dataWriteLocation(ScriptType scriptType);

    /**
     * Obtain mock data table constraints
     *
     * @return Return table constraint set
     */
    abstract public List<AbstractConstraint> constraints();

    /**
     * Maximum retention quantity, which means the maximum quantity of batch data retained in the memory
     */
    abstract public int maxRetainedCount();
}
