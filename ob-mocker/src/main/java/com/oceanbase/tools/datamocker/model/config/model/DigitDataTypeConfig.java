package com.oceanbase.tools.datamocker.model.config.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 数字类型的类型信息封装
 *
 * @author yh263208
 * @date 2020-12-24 20:52
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class DigitDataTypeConfig extends DataTypeConfig {
    /**
     * 数据的精度，代表有效位数
     */
    private Integer precision;
    /**
     * 数字类型的精度，代表小数点后的位数
     */
    private Integer scale;
}
