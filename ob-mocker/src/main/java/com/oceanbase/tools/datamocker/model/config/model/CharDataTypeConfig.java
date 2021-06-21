package com.oceanbase.tools.datamocker.model.config.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Character data type configuration object
 *
 * @author yh263208
 * @date 2020-12-24 20:59
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class CharDataTypeConfig extends DataTypeConfig {
    private String charset;
    /**
     * Data length, column length calculated according to Byte
     */
    private Integer width;
    /**
     * Whether to use characters to count column width
     */
    private boolean isUnicode = false;
}
