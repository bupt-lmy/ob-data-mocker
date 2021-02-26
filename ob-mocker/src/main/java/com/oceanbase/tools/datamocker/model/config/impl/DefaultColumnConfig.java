package com.oceanbase.tools.datamocker.model.config.impl;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.model.config.AbstractColumnConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataTypeConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * 列任务配置对象，用于标明列生成任务的配置信息
 *
 * @author yh263208
 * @date 2020-12-27 20:57
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class DefaultColumnConfig extends AbstractColumnConfig {
    /**
     * 一列的列名
     */
    private String columnName;
    /**
     * 列生成任务的详细配置
     */
    private DataTypeConfig typeConfig;
    /**
     * 是否允许空值，默认为真
     */
    private Boolean allowNull = true;
    /**
     * 列的默认值
     */
    private Object defaultValue;
    /**
     * 类型信息
     */
    private AbstractDataType dataType = null;

    @Override
    public String columnName() {
        return columnName;
    }

    @Override
    public synchronized AbstractDataType columnType() {
        if (dataType != null) {
            return dataType;
        }
        DataTypeFactory factory = DataTypeFactory.getInstance(typeConfig.getColumnType());
        typeConfig.setAllowNull(allowNull());
        typeConfig.setDefaultValue(defaultValue());
        this.dataType = factory.make(typeConfig);
        return this.dataType;
    }

    @Override
    public Boolean allowNull() {
        return allowNull;
    }

    @Override
    public Object defaultValue() {
        return defaultValue;
    }
}
