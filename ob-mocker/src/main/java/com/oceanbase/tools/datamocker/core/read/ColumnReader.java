package com.oceanbase.tools.datamocker.core.read;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.Validate;

/**
 * Column reader, which is used to get a column data from data generator
 *
 * @author yh263208
 * @date 2020-12-31 17:41
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class ColumnReader<T> extends AbstractMockReader<T> {
    /**
     * Data type for a column
     */
    private final AbstractDataType<T, ? extends Comparable<?>> dataType;
    /**
     * Dolumn name
     */
    @Getter
    private final String columnName;
    /**
     * Group ID
     */
    private final String groupId;

    public ColumnReader(AbstractDataType<T, ? extends Comparable<?>> dataType, String columnName, String groupId) {
        Validate.notNull(dataType, "DataType can not be null for ColumnReader");
        Validate.notNull(columnName, "ColumnName can not be null for ColumnReader");
        this.dataType = dataType;
        this.columnName = columnName;
        this.groupId = groupId;
    }

    @Override
    public MockColumnData<T> read() {
        return new MockColumnData<>(columnName, dataType, dataType.acquire());
    }

    @Override
    public String groupId() {
        return this.groupId;
    }
}
