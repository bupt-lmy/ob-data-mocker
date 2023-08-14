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

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import org.apache.commons.lang.Validate;

/**
 * Abstract constraint object, used to describe a constraint in database
 *
 * @author yh263208
 * @date 2021-01-12 10:56
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class AbstractConstraint {
    /**
     * Name for constraint
     */
    private final String constraintName;
    /**
     * Schema name which is associated with constraint
     */
    private final String database;
    /**
     * Table name which is associated with constraint
     */
    private final String tableName;
    /**
     * The name of the column to which the constraint is associated. The database has a position
     * description for the column to which the constraint is associated. That is, the column to which
     * the constraint is associated is in the position of the constraint, and the column to which a
     * constraint is associated may not only be in one table Here, the Key of the outer Map represents
     * the table name, used to indicate which table the constraint-related column is in, and the inner
     * Map is used to indicate the position of the constraint-related column in a table in the
     * constraint, Key represents the column name, and value represents The position of the constraint
     * is meaningful for constraint checking, because if a unique constraint is established on two
     * columns, then the positional relationship between the two columns in the constraint is necessary,
     * because AB and BA are obviously in compliance with the constraint even if They just swapped
     * positions
     */
    private final Map<String, Map<String, Integer>> tableName2ConstrantColumns;

    protected AbstractConstraint(String constraintName, String database, String tableName,
            Map<String, Map<String, Integer>> tableName2ConstrantColumns) {
        this.constraintName = constraintName;
        this.database = database;
        Map<String, Integer> columns = validateConsColumns(tableName, tableName2ConstrantColumns);
        this.tableName = tableName;
        this.tableName2ConstrantColumns = tableName2ConstrantColumns;
        initWithRows(columns, null);
    }

    protected AbstractConstraint(String constraintName, String database, String tableName,
            Map<String, Map<String, Integer>> tableName2ConstrantColumns, List<MockRowData> rows) {
        this.constraintName = constraintName;
        this.database = database;
        Map<String, Integer> columns = validateConsColumns(tableName, tableName2ConstrantColumns);
        this.tableName = tableName;
        this.tableName2ConstrantColumns = tableName2ConstrantColumns;
        initWithRows(columns, rows);
    }

    private Map<String, Integer> validateConsColumns(String table, Map<String, Map<String, Integer>> consColumns) {
        Validate.notNull(table, "Table name for AbstractConstraint can not be null");
        Validate.notEmpty(consColumns, "Constraint columns can not be null or empty");
        Map<String, Integer> columns = consColumns.get(table);
        if (columns == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    String.format("Constraint columns for table \"%s\" can not be null or empty", table));
        }
        return columns;
    }

    public boolean check(MockRowData mockRowData) {
        if (mockRowData == null || mockRowData.columnNum() == 0) {
            return false;
        }
        Map<String, Integer> columns = validateConsColumns(tableName, this.tableName2ConstrantColumns);
        validateInput(columns, mockRowData);
        return doCheck(columns, mockRowData, false);
    }

    /**
     * Mark method, used to mark a row of data. If you call this method which means that this row of
     * data will be effective
     *
     * @param value row of data
     * @return marked row of data
     */
    public boolean mark(MockRowData value) {
        if (value == null || value.columnNum() == 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Value can not be null for mark method");
        }
        Map<String, Integer> columns = validateConsColumns(tableName, this.tableName2ConstrantColumns);
        return doCheck(columns, value, true);
    }

    /**
     * Verify if the input data is legal
     *
     * @param columns Column info list
     * @param mockRowData input data
     * @throws MockerException exception will be thrown when error occured
     */
    private void validateInput(Map<String, Integer> columns, MockRowData mockRowData) {
        Set<String> initCons = columns.keySet();
        Set<String> valueCons = mockRowData.columnNames();
        for (String column : initCons) {
            if (!valueCons.contains(column)) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        String.format("Input constraint's columns must contain init constraint's columns, [%s]!=[%s]",
                                String.join(",", initCons),
                                String.join(",", valueCons)));
            }
        }
    }

    public Map<String, Map<String, Integer>> columns() {
        return this.tableName2ConstrantColumns;
    }

    public String name() {
        return this.constraintName;
    }

    abstract protected void initWithRows(Map<String, Integer> columns, List<MockRowData> rows);

    abstract protected boolean doCheck(Map<String, Integer> columns, MockRowData value, Boolean markable);
}
