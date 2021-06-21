package com.oceanbase.tools.datamocker.model.config;

import java.util.List;

import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Abstract task configuration
 *
 * @author yh263208
 * @date 2020-12-24 15:39
 * @since OBMOCKER-snapshot-0.1.0
 */
public abstract class AbstractTaskConfig {
    /**
     * Get the mode of OB, here only accept enumeration value return,
     * respectively ORACLE mode and MYSQL mode
     *
     * @return Return to OB mode
     */
    abstract public ObModeType obDialectType();

    /**
     * Get database configuration object
     *
     * @return Returns the database configuration object
     */
    abstract public DataBaseConfig dbConfig();

    /**
     * Return task configuration, return a table task collection
     *
     * @return Return to table task collection
     */
    abstract public List<? extends AbstractTableConfig> tasks();

    /**
     * Get the task name of the task mock, the configuration can be omitted,
     * if not, the sdk will specify the task name by itself
     *
     * @return Return task name
     */
    abstract public String taskName();

    public int minConnection() {
        if (connectionInitCount() < 3) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Min connection count for connection pool can not be smaller than 3");
        }
        if (connectionInitCount() > connectionMaxCount()) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Min connection count can not bigger than max connection count");
        }
        return connectionInitCount();
    }

    public int maxConnection() {
        if (connectionMaxCount() < 5) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Max connection count for connection pool can not be smaller than 5");
        }
        if (connectionMaxCount() < connectionInitCount()) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Max connection count can not smaller than min connection count");
        }
        return connectionMaxCount();
    }

    public int connectionIncreasementStep() {
        if (connectionIncreaseStepCount() < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Connection pool's increase step can not be smaller than zero");
        }
        return connectionIncreaseStepCount();
    }

    /**
     * The increase step size of the database connection from the minimum number
     * of connections to the maximum number of connections
     *
     * @return Return step
     */
    abstract protected int connectionIncreaseStepCount();

    /**
     * Number of initial connections in the database connection pool
     *
     * @return Returns the number of initial connections
     */
    abstract protected int connectionInitCount();

    /**
     * The maximum number of connections that the database connection pool can hold
     *
     * @return Return the maximum number of connections
     */
    abstract protected int connectionMaxCount();
}
