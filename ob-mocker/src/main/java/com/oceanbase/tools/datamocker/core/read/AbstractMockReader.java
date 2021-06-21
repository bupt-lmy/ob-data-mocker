package com.oceanbase.tools.datamocker.core.read;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.util.Pair;

/**
 * Data reader, you can read data from this reader
 *
 * @author yh263208
 * @date 20201-01-14 14:14
 * @since OBMOCKER_0.1.0_snapshot
 */
public abstract class AbstractMockReader<T> {
    /**
     * Read method, you can read data from this method
     *
     * @return data which is read
     * @throws Exception exception will be thrown when error occured
     */
    abstract public Pair<String, Pair<AbstractDataType, T>> read() throws Exception;

    /**
     * Get the groupId of the primitive. The primitive is an atomic part of an overall operation, so a
     * groupId is needed to identify which primitives belong to the same overall operation
     *
     * @return group id string value
     */
    abstract public String groupId();
}
