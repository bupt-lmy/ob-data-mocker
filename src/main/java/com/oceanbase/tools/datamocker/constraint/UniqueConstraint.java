/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oceanbase.tools.datamocker.constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.datamocker.util.DuplicatedJudger;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.Validate;

/**
 * {@link UniqueConstraint}
 *
 * @author yh263208
 * @date 2023-11-27 14:56
 * @since ODC_release_4.2.3
 */
@Slf4j
public class UniqueConstraint implements Constraint {

    private final String constraintName;
    private final String tableName;
    private final DuplicatedJudger judger;
    private final Map<String, Map<String, Integer>> tableName2ConstraintColumns;
    private final List<String> sortedList;

    public UniqueConstraint(@NonNull String constraintName, @NonNull String tableName,
            @NonNull Map<String, Map<String, Integer>> consColumns, int count) {
        Validate.isTrue(count > 0, "Count for UniqueConstraint can not be negative");
        this.constraintName = constraintName;
        this.tableName = tableName;
        this.tableName2ConstraintColumns = consColumns;
        this.judger = new DuplicatedJudger(count);
        this.sortedList = sortMapByValue(validateConsColumns(tableName, consColumns));
    }

    /**
     * Mark method, used to mark a row of data. If you call this method which means that this row of
     * data will be effective
     *
     * @param value row of data
     * @return marked row of data
     */
    @Override
    public boolean mark(MockRowData value) {
        if (value == null || value.columnNum() == 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Value can not be null for mark method");
        }
        validateConsColumns(tableName, this.tableName2ConstraintColumns);
        return doCheck(value, true);
    }

    @Override
    public boolean check(MockRowData rowData) {
        if (rowData == null || rowData.columnNum() == 0) {
            return false;
        }
        Map<String, Integer> columns = validateConsColumns(tableName, this.tableName2ConstraintColumns);
        validateInput(columns, rowData);
        return doCheck(rowData, false);
    }

    @Override
    public String name() {
        return this.constraintName;
    }

    @Override
    public Map<String, Map<String, Integer>> columns() {
        return this.tableName2ConstraintColumns;
    }

    protected boolean doCheck(MockRowData value, Boolean markable) {
        String checkValue = convertFromRowDataToStringListData(value);
        if (judger.contains(checkValue)) {
            // log.warn(String.format("value \"%s\" for columns \"%s\" can not pass the unique constraint, will
            // be droped", checkValue,
            // columns.keySet().stream().collect(Collectors.joining(","))));
            return false;
        }
        if (markable) {
            if (!judger.add(checkValue)) {
                log.warn("Fail to add row to DuplicatedJudger, row={}", checkValue);
            }
        }
        return true;
    }

    private void validateInput(Map<String, Integer> columns, MockRowData mockRowData) {
        Set<String> initCons = columns.keySet();
        Set<String> valueCons = mockRowData.columnNames();
        if (initCons.stream().anyMatch(s -> !valueCons.contains(s))) {
            String errMsg =
                    String.format("Input constraint's columns must contain init constraint's columns, [%s]!=[%s]",
                            String.join(",", initCons), String.join(",", valueCons));
            throw new MockerException(MockerError.PARAMETER_ERROR, errMsg);
        }
    }

    /**
     * Sort method, which is used to sort the map by every row's position stored in input map
     *
     * @param map intput map
     * @return sorted list of column name
     */
    private static List<String> sortMapByValue(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
        entryList.sort(Entry.comparingByValue());
        return entryList.stream().map(Entry::getKey).collect(Collectors.toList());
    }

    private static Map<String, Integer> validateConsColumns(String table,
            Map<String, Map<String, Integer>> consColumns) {
        Map<String, Integer> columns = consColumns.get(table);
        if (columns == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    String.format("Constraint columns for table \"%s\" can not be null or empty", table));
        }
        return columns;
    }

    /**
     * Convert method, which is to convert a data to string value, used to verify
     *
     * @param mockRowData row of data
     * @return string value for this row of data
     */
    private String convertFromRowDataToStringListData(MockRowData mockRowData) {
        return sortedList.stream().map(s -> {
            MockColumnData<?> mockColumn = mockRowData.getMockColumn(s);
            if (mockColumn == null) {
                throw new MockerException(MockerError.OPERATION_FAILURE,
                        "Data for unique constraint have to have same column list \"%s\""
                                + String.join(",", sortedList));
            }
            return mockColumn.toDigestString();
        }).collect(Collectors.joining(","));
    }

}
