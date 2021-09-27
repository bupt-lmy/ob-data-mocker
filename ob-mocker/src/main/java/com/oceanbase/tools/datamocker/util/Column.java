package com.oceanbase.tools.datamocker.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used for serialization or deserialization when reading the database
 *
 * @author yh263208@antgroup.com
 * @date 2020-12-02 15:19
 * @since OBMOCKER-0.1.0-snapshot
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    /**
     * Default serialized or deserialized field name, required field
     */
    String value();

    /**
     * Whether this field ignores the configuration, the default is false
     */
    boolean ignore() default false;
}
