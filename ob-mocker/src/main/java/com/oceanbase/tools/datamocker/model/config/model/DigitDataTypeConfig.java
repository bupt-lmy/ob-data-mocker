package com.oceanbase.tools.datamocker.model.config.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Type information encapsulation for digital types
 *
 * @author yh263208
 * @date 2020-12-24 20:52
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class DigitDataTypeConfig extends DataTypeConfig {
    /**
     * The precision of the data,
     * representing the effective number of digits
     */
    private Integer precision;
    /**
     * The precision of the number type,
     * representing the number of digits after the decimal point
     */
    private Integer scale;
}
