package com.oceanbase.tools.datamocker.model.config.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Getter;
import lombok.Setter;

/**
 * 类型配置，用于表示数据库的类型配置信息
 *
 * @author yh263208
 * @date 2020-12-24 20:26
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "name")
@JsonSubTypes(value = {
        @JsonSubTypes.Type(value = DigitDataTypeConfig.class, name = "DIGIT"),
        @JsonSubTypes.Type(value = DateDataTypeConfig.class, name = "DATE"),
        @JsonSubTypes.Type(value = CharDataTypeConfig.class, name = "CHAR")
})
public class DataTypeConfig {
    /**
     * 默认值
     */
    private Object defaultValue;
    /**
     * 列是否可以为空
     */
    private Boolean allowNull;
    /**
     * 列的类型枚举
     */
    private String columnType;
    /**
     * 低值
     */
    private Object lowValue;
    /**
     * 高值
     */
    private Object highValue;
    /**
     * 和列绑定的数据生成器初始化参数，可能有多个值因此用List对象封装
     */
    private Map<String, ?> genParams;
    /**
     * 数据生成器builder对象，用于根据上述params对象生成一个具体的数据生成器供列对象来使用
     */
    private String generator;

}
