package com.oceanbase.tools.datamocker.constraint.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.DuplicatedJudger;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.extern.slf4j.Slf4j;

/**
 * Packaged Object for Unique Constraint, used to verify whether
 * the unique constraint of the database is violated
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
     * @param database       schema for this unique constraint
     * @param tableName      table name which is related to this unique constraint
     * @param consColumns    the name of the column to which the constraint is associated
     * @param count          the row count which is needed to be verified
     */
    public UniqueConstraint(String constraintName, String database, String tableName,
            Map<String, Map<String, Integer>> consColumns, int count) {
        super(constraintName, database, tableName, consColumns);
        if (count <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Count for unique constraint can not be equal to or smaller than zero");
        }
        this.judger = new DuplicatedJudger(count);
    }

    /**
     * Constructor for unique constraint
     *
     * @param constraintName name for unique constraint
     * @param database schema for this unique constraint
     * @param tableName table name which is related to this unique constraint
     * @param consColumns the name of the column to which the constraint is associated
     * @param rows initialize the column data, define some initial values of the unique constraint through the input of the column data
     * @param count the row count which is needed to be verified
     */
    public UniqueConstraint(String constraintName, String database, String tableName,
            Map<String, Map<String, Integer>> consColumns, List<Map<String, Pair<AbstractDataType, Object>>> rows, int count) {
        super(constraintName, database, tableName, consColumns, rows);
        if (count <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Count for unique constraint can not be equal to or smaller than zero");
        }
        if (rows != null && rows.size() != 0) {
            this.judger = new DuplicatedJudger(rows.size() + count);
            for (Map<String, Pair<AbstractDataType, Object>> row : rows) {
                String result = convert(row);
                judger.add(result);
            }
        } else {
            this.judger = new DuplicatedJudger(count);
        }
    }

    @Override
    protected void initWithRows(Map<String, Integer> columns, List<Map<String, Pair<AbstractDataType, Object>>> rows) {
        this.sortedList = sortMapByValue(columns);
    }

    @Override
    protected boolean doCheck(Map<String, Integer> columns, Map<String, Pair<AbstractDataType, Object>> value, Boolean markable) {
        String checkValue = convert(value);
        if (judger.contains(checkValue)) {
            //log.warn(String.format("value \"%s\" for columns \"%s\" can not pass the unique constraint, will be droped", checkValue,
            //        columns.keySet().stream().collect(Collectors.joining(","))));
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
     * @param row row of data
     * @return string value for this row of data
     */
    private String convert(Map<String, Pair<AbstractDataType, Object>> row) {
        List<String> list = new ArrayList<>();
        for (String column : sortedList) {
            Pair<AbstractDataType, ?> value = row.get(column);
            if (value == null) {
                throw new MockerException(MockerError.OPERATION_FAILURE,
                        String.format("Data for unique constraint have to have same column list \"%s\"",
                                sortedList.stream().collect(Collectors.joining(","))));
            }
            Object convertVal = value.getKey().convert(value.getValue());
            Object digestVal = value.getKey().toDigest(convertVal);
            list.add(value.getKey().toString(digestVal));
        }
        return list.stream().collect(Collectors.joining(","));
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
        Collections.sort(entryList, (o1, o2) -> o1.getValue().compareTo(o2.getValue()));
        for (Map.Entry<String, Integer> entry : entryList) {
            sortedList.add(entry.getKey());
        }
        return sortedList;
    }

}
