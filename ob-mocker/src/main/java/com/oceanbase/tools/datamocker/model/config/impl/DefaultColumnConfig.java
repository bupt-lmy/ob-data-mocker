package com.oceanbase.tools.datamocker.model.config.impl;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.datatype.DataTypeFactory;
import com.oceanbase.tools.datamocker.model.config.AbstractColumnConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataTypeConfig;
import lombok.Getter;
import lombok.Setter;

/**
 * Column task configuration object, used to indicate the configuration information of the column
 * generation task
 *
 * @author yh263208
 * @date 2020-12-27 20:57
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class DefaultColumnConfig extends AbstractColumnConfig {
    private String columnName;
    /**
     * Detailed configuration of column generation tasks
     */
    private DataTypeConfig typeConfig;
    /**
     * Whether to allow null values, the default is true
     */
    private Boolean allowNull = true;
    /**
     * The default value of the column
     */
    private Object defaultValue;
    /**
     * Type information
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
