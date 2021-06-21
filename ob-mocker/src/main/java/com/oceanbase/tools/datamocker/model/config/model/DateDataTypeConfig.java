package com.oceanbase.tools.datamocker.model.config.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Date type configuration object
 *
 * @author yh263208
 * @date 2020-12-24 20:59
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class DateDataTypeConfig extends DataTypeConfig {
    private String timezone;
    /**
     * Time precision, mainly for the timestamp type,
     * indicating the length of the time in nanoseconds
     */
    private Integer scale;
}
