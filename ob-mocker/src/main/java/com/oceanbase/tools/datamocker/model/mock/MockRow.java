package com.oceanbase.tools.datamocker.model.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.apache.commons.lang.StringUtils;

/**
 * 一行数据，包含模拟数据生成的一行数据，包含多个列
 *
 * @author yh263208
 * @date 2021-02-22 11:50
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MockRow {
    /**
     * 一行数据，包含了多个列，是一个key-value的数据结构，key为该列的列名，value为该列的实际数据
     */
    private Map<String, MockColumn> row;

    public MockRow() {
        this.row = new HashMap<>();
    }

    /**
     * 获取一个列数据
     *
     * @param name 列名
     * @return 返回列数据
     */
    public MockColumn get(String name) {
        if (StringUtils.isBlank(name)) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "name for mock row can not be null or empty");
        }
        if (row == null) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, "mock row is null, unknown error");
        }
        return this.row.get(name);
    }

    /**
     * 设置一个列数据
     *
     * @param columnName 对应的列名
     * @param data       对应的数据
     */
    public MockColumn putIfAbsent(String columnName, MockColumn data) {
        if (this.row == null) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, "mock row is null, unknown error");
        }
        return this.row.putIfAbsent(columnName, data);
    }

    /**
     * 获取当前行中包含多少列数据
     *
     * @return 返回size大小
     */
    public int size() {
        if (this.row == null) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, "mock row is null, unknown error");
        }
        return this.row.size();
    }

    /**
     * 获取行数据的列集合
     *
     * @return 返回列集合
     */
    public Set<String> columns() {
        if (this.row == null) {
            return null;
        }
        return this.row.keySet();
    }
}
