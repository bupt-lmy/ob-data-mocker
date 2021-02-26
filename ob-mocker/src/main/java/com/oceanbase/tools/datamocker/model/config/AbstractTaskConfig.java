package com.oceanbase.tools.datamocker.model.config;

import java.util.List;

import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.DialectType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 抽象任务配置
 *
 * @author yh263208
 * @date 2020-12-24 15:39
 * @since OBMOCKER-snapshot-0.1.0
 */
public abstract class AbstractTaskConfig {
    /**
     * 获取OB的模式，这里只接受枚举值返回，分别为ORACLE模式和MYSQL模式
     *
     * @return 返回OB模式
     */
    abstract public DialectType obDialectType();

    /**
     * 获取数据库配置对象
     *
     * @return 返回数据库配置对象
     */
    abstract public DataBaseConfig dbConfig();

    /**
     * 返回任务配置，返回一个表任务集合
     *
     * @return 返回表任务集合
     */
    abstract public List<? extends AbstractTableConfig> tasks();

    /**
     * 获取任务mock任务名称，该配置可以不传，如果不传则由sdk自行指定任务名称
     *
     * @return 返回任务名称
     */
    abstract public String taskName();

    public int minConnection() {
        if (connectionInitCount() < 3) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "min connection count for connection pool can not be smaller than 3");
        }
        if (connectionInitCount() > connectionMaxCount()) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "min connection count can not bigger than max connection count");
        }
        return connectionInitCount();
    }

    public int maxConnection() {
        if (connectionMaxCount() < 5) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "max connection count for connection pool can not be smaller than 5");
        }
        if (connectionMaxCount() < connectionInitCount()) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "max connection count can not smaller than min connection count");
        }
        return connectionMaxCount();
    }

    public int connectionIncreasementStep() {
        if (connectionIncreaseStepCount() < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "connection pool's increase step can not be smaller than zero");
        }
        return connectionIncreaseStepCount();
    }

    /**
     * 数据库连接从最小连接数增大到最大连接数的扩增步长
     *
     * @return 返回步长
     */
    abstract protected int connectionIncreaseStepCount();

    /**
     * 数据库连接池的初始化连接数目
     *
     * @return 返回初始化连接数目
     */
    abstract protected int connectionInitCount();

    /**
     * 数据库连接池最大能容纳的的连接数目
     *
     * @return 返回最大的连接数目
     */
    abstract protected int connectionMaxCount();
}
