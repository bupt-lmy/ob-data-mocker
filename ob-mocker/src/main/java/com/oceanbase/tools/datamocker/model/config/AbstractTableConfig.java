package com.oceanbase.tools.datamocker.model.config;

import java.util.List;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.model.enums.DuplicateStrategy;
import com.oceanbase.tools.datamocker.model.enums.ScriptType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Mock数据的配置对象，在该配置对象中配置Mock数据的关键参数
 *
 * @author yh263208
 * @date 2020-12-22 20:40
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class AbstractTableConfig {
    /**
     * 最大生成数量，必须是一个大于0的整数，最大限制在100000
     *
     * @return 返回数量
     */
    abstract protected Long maxRowCount();

    /**
     * 获取数据生成任务的最大生成数量，在这里对用户传入的最大生成量做了校验，只允许传入一个0-1000000之间的值
     *
     * @return 返回最大生成数量
     */
    public Long maxCount() {
        if (maxRowCount() == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "max count can not be null");
        }
        if (maxRowCount() < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "max count can not be smaller than zero");
        } else if (maxRowCount() > 1000000) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "max count can not be bigger than 1000000");
        }
        return maxRowCount();
    }

    /**
     * 发生冲突时的处理方式，返回一个枚举值
     *
     * @return 返回一个枚举值
     */
    abstract public DuplicateStrategy duplicateStrategy();

    /**
     * 批处理大小，支持一个0-100000的值
     *
     * @return 返回批处理大小
     */
    abstract protected Long batchSize();

    /**
     * 针对批处理大小的值进行校验
     *
     * @return 返回批处理大小
     */
    public Long maxBatchSize() {
        if (batchSize() == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "batch size can not be null");
        }
        if (batchSize() < 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "batch size can not be smaller than zero");
        } else if (batchSize() > 100000) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "batch size can not be bigger than 100000");
        }
        return batchSize();
    }

    /**
     * 是否清空表
     *
     * @return 返回一个布尔型的值
     */
    abstract public Boolean truncated();

    /**
     * 返回要Mock的表的名称
     *
     * @return 返回表的名称
     */
    abstract public String tableName();

    /**
     * 获取列任务列表
     *
     * @return 返回列任务集合
     */
    abstract public List<? extends AbstractColumnConfig> columns();

    /**
     * 返回表任务的schema名称
     *
     * @return 返回schema名称
     */
    abstract public String schemaName();

    /**
     * 表生成任务的超时时间
     *
     * @return 返回超时时间
     */
    abstract public Long timeoutMilliseconds();

    /**
     * 脚本类型枚举，mock数据可以定义写出脚本类型，可以输出多个脚本
     *
     * @return 返回脚本类型数组
     */
    abstract public ScriptType[] scriptType();

    /**
     * 数据写出地址
     *
     * @param scriptType 脚本类型
     * @return 返回对应脚本的数据写出地址
     */
    abstract public String dataWriteLocation(ScriptType scriptType);

    /**
     * 获取mock数据表约束
     *
     * @return 返回表约束集合
     */
    abstract public List<AbstractConstraint> constraints();

    /**
     * 最大留存数量，意为内存中最大滞留的批数据数量
     */
    abstract public int maxRetainedCount();
}
