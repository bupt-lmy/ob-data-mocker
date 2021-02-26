package com.oceanbase.tools.datamocker.model.config.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 字符数据类型配置对象
 *
 * @author yh263208
 * @date 2020-12-24 20:59
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class CharDataTypeConfig extends DataTypeConfig {
    /**
     * 字符串的编码格式
     */
    private String charset;
    /**
     * 数据长度，按照字节Byte计算得到的列长度
     */
    private Integer width;
    /**
     * 是否使用字符统计列宽度
     */
    private boolean isUnicode = false;
}
