package com.oceanbase.tools.datamocker.constraint;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.Pair;

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
    private String constraintName;
    /**
     * Schema name which is associated with constraint
     */
    private String database;
    /**
     * Table name which is associated with constraint
     */
    private String tableName;
    /**
     * The name of the column to which the constraint is associated. The database has a position
     * description for the column to which the constraint is associated. That is, the column to
     * which the constraint is associated is in the position of the constraint, and the column to
     * which a constraint is associated may not only be in one table Here, the Key of the outer Map
     * represents the table name, used to indicate which table the constraint-related column is in,
     * and the inner Map is used to indicate the position of the constraint-related column in a
     * table in the constraint, Key represents the column name, and value represents The position
     * of the constraint is meaningful for constraint checking, because if a unique constraint
     * is established on two columns, then the positional relationship between the two columns
     * in the constraint is necessary, because AB and BA are obviously in compliance with the
     * constraint even if They just swapped positions
     */
    private Map<String, Map<String, Integer>> tableName2ConstrantColumns;

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
            Map<String, Map<String, Integer>> tableName2ConstrantColumns, List<Map<String, Pair<AbstractDataType, Object>>> rows) {
        this.constraintName = constraintName;
        this.database = database;
        Map<String, Integer> columns = validateConsColumns(tableName, tableName2ConstrantColumns);
        this.tableName = tableName;
        this.tableName2ConstrantColumns = tableName2ConstrantColumns;
        initWithRows(columns, rows);
    }

    private Map<String, Integer> validateConsColumns(String table, Map<String, Map<String, Integer>> consColumns) {
        if (table == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Table name for constraint can not be null");
        }
        if (consColumns == null || consColumns.size() == 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Constraint columns can not be null or empty");
        }
        Map<String, Integer> columns = consColumns.get(table);
        if (columns == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    String.format("Constraint columns for table \"%s\" can not be null or empty", table));
        }
        return columns;
    }

    public boolean check(Map<String, Pair<AbstractDataType, Object>> columnName2DataPair) {
        if (columnName2DataPair == null || columnName2DataPair.size() == 0) {
            return false;
        }
        Map<String, Integer> columns = validateConsColumns(tableName, this.tableName2ConstrantColumns);
        validateInput(columns, columnName2DataPair);
        return doCheck(columns, columnName2DataPair, false);
    }

    /**
     * Mark method, used to mark a row of data. If you call this method which means
     * that this row of data will be effective
     *
     * @param value row of data
     * @return marked row of data
     */
    public boolean mark(Map<String, Pair<AbstractDataType, Object>> value) {
        if (value == null || value.size() == 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Value can not be null for mark method");
        }
        Map<String, Integer> columns = validateConsColumns(tableName, this.tableName2ConstrantColumns);
        return doCheck(columns, value, true);
    }

    /**
     * Verify if the input data is legal
     *
     * @param columns             Column info list
     * @param columnName2DataPair input data
     * @throws MockerException exception will be thrown when error occured
     */
    private void validateInput(Map<String, Integer> columns, Map<String, Pair<AbstractDataType, Object>> columnName2DataPair) {
        Set<String> initCons = columns.keySet();
        Set<String> valueCons = columnName2DataPair.keySet();
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

    abstract protected void initWithRows(Map<String, Integer> columns, List<Map<String, Pair<AbstractDataType, Object>>> rows);

    abstract protected boolean doCheck(Map<String, Integer> columns, Map<String, Pair<AbstractDataType, Object>> value,
            Boolean markable);
}
