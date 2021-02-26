package com.oceanbase.tools.datamocker.model.config.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 日期类型配置对象
 *
 * @author yh263208
 * @date 2020-12-24 20:59
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class DateDataTypeConfig extends DataTypeConfig {
    /**
     * 时区
     */
    private String timezone;
    /**
     * 精度
     */
    private Integer scale;
}
