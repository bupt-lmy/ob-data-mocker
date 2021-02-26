package com.oceanbase.tools.datamocker.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 该注解用于读取数据库时做序列化或反序列化时使用
 *
 * @author yh263208@antgroup.com
 * @date 2020-12-02 15:19
 * @since OBMOCKER-0.1.0-snapshot
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    /**
     * 默认的序列化或反序列化字段名，必填字段
     */
    String value();

    /**
     * 该字段是否忽略配置，默认为false
     */
    boolean ignore() default false;
}
