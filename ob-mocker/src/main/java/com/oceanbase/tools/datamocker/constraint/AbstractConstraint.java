package com.oceanbase.tools.datamocker.constraint;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.Pair;

/**
 * 抽象约束，用于描述数据库约束，对其进行抽象
 *
 * @author yh263208
 * @date 2021-01-12 10:56
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class AbstractConstraint {
    /**
     * 约束的名字
     */
    private String constraintName;
    /**
     * 约束关联的数据库，在oracle模式中为owner字段
     */
    private String database;
    /**
     * 约束关联到的表名
     */
    private String tableName;
    /**
     * 约束关联到的列名，数据库中对于约束关联到的列有position的描述，即该约束关联到的列处于约束的第几个位置上，且一个约束关联到的列可能不仅仅处于一个表中
     * 在这里，外层Map的Key代表表名，用于说明约束相关的列处于哪一张表中，内层的Map用于表明一张表中的约束关联列在约束中的位置，Key代表列名，value代表约束
     * 所处的位置，该位置对于约束校验有意义，因为假如一个唯一约束建立在两列上，那么两列在约束中的位置关系就是必要的，因为AB和BA很明显是符合约束的即使他们
     * 仅仅是交换了位置
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
            throw new MockerException(MockerError.PARAMETER_ERROR, String.format("Table name for constraint can not be null", table));
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
     * 标记方法，用于约束标记。具体作用在于标记一行数据
     *
     * @param value 数据
     * @return 返回标记的数据
     */
    public boolean mark(Map<String, Pair<AbstractDataType, Object>> value) {
        if (value == null || value.size() == 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Value can not be null for mark method");
        }
        Map<String, Integer> columns = validateConsColumns(tableName, this.tableName2ConstrantColumns);
        return doCheck(columns, value, true);
    }

    /**
     * 验证输入是否合法
     *
     * @param columns             列信息集合
     * @param columnName2DataPair 输入数据
     * @throws MockerException 验证失败则抛出异常
     */
    private void validateInput(Map<String, Integer> columns, Map<String, Pair<AbstractDataType, Object>> columnName2DataPair) {
        Set<String> initCons = columns.keySet();
        Set<String> valueCons = columnName2DataPair.keySet();
        for (String column : initCons) {
            if (!valueCons.contains(column)) {
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        String.format("Input constraint's columns must contain init constraint's columns, [%s]!=[%s]",
                                initCons.stream().collect(Collectors.joining(",")),
                                valueCons.stream().collect(Collectors.joining(","))));
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
