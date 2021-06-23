package com.oceanbase.tools.datamocker.constraint.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.datamocker.util.DuplicatedJudger;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.Validate;

/**
 * Packaged Object for Unique Constraint, used to verify whether the unique constraint of the
 * database is violated
 *
 * @author yh263208
 * @date 2020-12-31 20:47
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class UniqueConstraint extends AbstractConstraint {
    /**
     * Judger used to verify whether a piece of data appears multiple times
     */
    private final DuplicatedJudger judger;
    /**
     * Sorted column list which is associated with unique constraint
     */
    private List<String> sortedList;

    /**
     * Constructor for UniqueConstraint
     *
     * @param constraintName name for unique constraint
     * @param database schema for this unique constraint
     * @param tableName table name which is related to this unique constraint
     * @param consColumns the name of the column to which the constraint is associated
     * @param count the row count which is needed to be verified
     */
    public UniqueConstraint(String constraintName, String database, String tableName,
            Map<String, Map<String, Integer>> consColumns, int count) {
        super(constraintName, database, tableName, consColumns);
        Validate.isTrue(count > 0, "Count for UniqueConstraint can not be negative");
        this.judger = new DuplicatedJudger(count);
    }

    /**
     * Constructor for unique constraint
     *
     * @param constraintName name for unique constraint
     * @param database schema for this unique constraint
     * @param tableName table name which is related to this unique constraint
     * @param consColumns the name of the column to which the constraint is associated
     * @param rows initialize the column data, define some initial values of the unique constraint
     *        through the input of the column data
     * @param count the row count which is needed to be verified
     */
    public UniqueConstraint(String constraintName, String database, String tableName,
            Map<String, Map<String, Integer>> consColumns, List<MockRowData> rows, int count) {
        super(constraintName, database, tableName, consColumns, rows);
        Validate.isTrue(count > 0, "Count for UniqueConstraint can not be negative");
        if (rows != null && rows.size() != 0) {
            this.judger = new DuplicatedJudger(rows.size() + count);
            for (MockRowData row : rows) {
                String result = convert(row);
                judger.add(result);
            }
        } else {
            this.judger = new DuplicatedJudger(count);
        }
    }

    @Override
    protected void initWithRows(Map<String, Integer> columns, List<MockRowData> rows) {
        this.sortedList = sortMapByValue(columns);
    }

    @Override
    protected boolean doCheck(Map<String, Integer> columns, MockRowData value, Boolean markable) {
        String checkValue = convert(value);
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

    /**
     * Convert method, which is to convert a data to string value, used to verify
     *
     * @param mockRowData row of data
     * @return string value for this row of data
     */
    private String convert(MockRowData mockRowData) {
        String columnList = String.join(",", sortedList);
        return sortedList.stream().map(s -> {
            MockColumnData<?> mockColumn = mockRowData.getMockColumn(s);
            if (mockColumn == null) {
                throw new MockerException(MockerError.OPERATION_FAILURE,
                        String.format("Data for unique constraint have to have same column list \"%s\"", columnList));
            }
            return mockColumn.toDisgestString();
        }).collect(Collectors.joining(","));
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
        List<String> sortedList = new ArrayList<>();
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
        entryList.sort(Entry.comparingByValue());
        for (Map.Entry<String, Integer> entry : entryList) {
            sortedList.add(entry.getKey());
        }
        return sortedList;
    }

}
