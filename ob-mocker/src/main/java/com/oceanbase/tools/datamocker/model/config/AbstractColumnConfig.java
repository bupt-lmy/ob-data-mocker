package com.oceanbase.tools.datamocker.model.config;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;

/**
 * 针对具体的一列的生成任务的配置对象
 *
 * @author yh263208
 * @date 2020-12-22 22:07
 * @since OBMOCKER-snapshot-0.1.0
 */
public abstract class AbstractColumnConfig {
    /**
     * 返回该列的列名
     */
    abstract public String columnName();

    /**
     * 获取数据类型封装对象，在其中加入了约束的封装逻辑
     *
     * @return 返回数据类型封装对象
     */
    abstract public AbstractDataType columnType();

    /**
     * 是否允许为空，若为真则有可能往其中插入null值
     *
     * @return 返回是否为空的结果
     */
    abstract public Boolean allowNull();

    /**
     * 该列的默认值
     *
     * @return 返回该默认值
     */
    abstract public Object defaultValue();

}
