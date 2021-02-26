package com.oceanbase.tools.datamocker.core.read;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.extern.slf4j.Slf4j;

/**
 * 列数据读取器
 *
 * @author yh263208
 * @date 2020-12-31 17:41
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class ColumnReader<T> extends AbstractMockReader<T> {
    /**
     * 抽象数据类型
     */
    private AbstractDataType<T, ?> dataType;
    /**
     * 列名
     */
    private String columnName;
    /**
     * 分组ID
     */
    private String groupId;

    public ColumnReader(AbstractDataType<T, ?> dataType, String columnName, String groupId) {
        if (dataType == null || columnName == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "abstract data type or column name for column reader can not be null");
        }
        this.dataType = dataType;
        this.columnName = columnName;
        this.groupId = groupId;
    }

    @Override
    public Pair<String, Pair<AbstractDataType, T>> read() throws Exception {
        Pair<AbstractDataType, T> value = new Pair<>(dataType, dataType.acquire());
        return new Pair<>(columnName, value);
    }

    public String columnName() {
        return this.columnName;
    }

    @Override
    public String groupId() {
        return this.groupId;
    }
}
