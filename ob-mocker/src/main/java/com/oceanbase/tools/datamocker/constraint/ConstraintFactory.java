package com.oceanbase.tools.datamocker.constraint;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.constraint.impl.UniqueConstraint;
import com.oceanbase.tools.datamocker.core.task.AbstractCallBack;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.dbobject.ConstraintColumn;
import com.oceanbase.tools.datamocker.model.dbobject.TableColumn;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.datamocker.util.DbObjectNameUtil;
import com.oceanbase.tools.datamocker.util.SerializeUtil;
import com.oceanbase.tools.datamocker.util.SqlUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * Factory class for AbstractConstraint, used to generate the factory object to make constraint
 * object
 *
 * @author yh263208
 * @date 2021-01-11 17:18
 * @since OBMOCKER-snapshot-0.1.0
 */
public abstract class ConstraintFactory {
    /**
     * Sql to query unique constraint for oracle mode
     */
    private static final String ORACLE_UNIQUE_CONSTRAINT_SQL = "select o.* from (select * from all_constraints where "
            + "constraint_type='U') s left join all_cons_columns o on s"
            + ".OWNER=o.OWNER and s.CONSTRAINT_NAME=o.CONSTRAINT_NAME and "
            + "s.TABLE_NAME=o.TABLE_NAME where s.OWNER=? and s"
            + ".TABLE_NAME=?;";
    /**
     * Sql to query unqique constraint for mysql mode
     */
    private static final String MYSQL_UNIQUE_CONSTRAINT_SQL = "select CONSTRAINT_SCHEMA as OWNER, CONSTRAINT_NAME,"
            + "TABLE_NAME,COLUMN_NAME,ORDINAL_POSITION as POSITION from "
            + "information_schema.key_column_usage where "
            + "CONSTRAINT_NAME<>'PRIMARY' and CONSTRAINT_SCHEMA=? and "
            + "TABLE_NAME=?; ";
    /**
     * Sql to query primary constraint for oracle mode
     */
    private static final String ORACLE_PRIMARY_CONSTRAINT_SQL = "select o.* from (select * from all_constraints where "
            + "constraint_type='P') s left join all_cons_columns o on s"
            + ".OWNER=o.OWNER and s.CONSTRAINT_NAME=o.CONSTRAINT_NAME and "
            + "s.TABLE_NAME=o.TABLE_NAME where s.OWNER=? and s"
            + ".TABLE_NAME=?;";
    /**
     * Sql to query primary constraint for mysql mode
     */
    private static final String MYSQL_PRIMARY_CONSTRAINT_SQL = "select CONSTRAINT_SCHEMA as OWNER, CONSTRAINT_NAME,"
            + "TABLE_NAME,COLUMN_NAME,ORDINAL_POSITION as POSITION from "
            + "information_schema.key_column_usage where "
            + "CONSTRAINT_NAME='PRIMARY' and CONSTRAINT_SCHEMA=? and "
            + "TABLE_NAME=?; ";
    /**
     * Sql to query check constraint for oracle mode
     */
    private static final String ORACLE_CHECK_CONSTRAINT_SQL = "select o.* from (select * from all_constraints where "
            + "constraint_type='C') s left join all_cons_columns o on s"
            + ".OWNER=o.OWNER and s.CONSTRAINT_NAME=o.CONSTRAINT_NAME and "
            + "s.TABLE_NAME=o.TABLE_NAME where s.OWNER=? and s"
            + ".TABLE_NAME=?;";
    /**
     * Sql to query foreign constraint for mysql mode
     */
    private static final String ORACLE_FOREIGN_CONSTRAINT_SQL = "select o.* from (select * from all_constraints where "
            + "constraint_type='R') s left join all_cons_columns o on s"
            + ".OWNER=o.OWNER and s.CONSTRAINT_NAME=o.CONSTRAINT_NAME and "
            + "s.TABLE_NAME=o.TABLE_NAME where s.OWNER=? and s"
            + ".TABLE_NAME=?;";
    /**
     * Map between constraint factory name and constraint factory object
     */
    private static final Map<String, ConstraintFactory> FACTORYNAME_2_FACTORYINSTANCE = new HashMap<>();

    /**
     * Unique constraint factory
     */
    private static final ConstraintFactory UNIQUE_CONSTRAINT = new ConstraintFactory() {
        @Override
        public List<AbstractConstraint> make(DataSource dataSource, ObModeType dialectType, String database,
                String tableName, Map<String, AbstractDataType<?, ? extends Comparable<?>>> columnName2DataType,
                int totalCount) throws Throwable {
            List<AbstractConstraint> constraints;
            if (ObModeType.OB_ORACLE.equals(dialectType)) {
                constraints = getConstraints(dataSource, ORACLE_UNIQUE_CONSTRAINT_SQL, database, tableName,
                        columnName2DataType, totalCount, ObModeType.OB_ORACLE, new OracleValidation());
            } else if (ObModeType.OB_MYSQL.equals(dialectType)) {
                constraints = getConstraints(dataSource, MYSQL_UNIQUE_CONSTRAINT_SQL, database, tableName,
                        columnName2DataType, totalCount, ObModeType.OB_MYSQL, new MysqlValidation());
            } else {
                throw new MockerException(MockerError.INVALID_OB_MODE);
            }
            validateConstraints(constraints, tableName, columnName2DataType, totalCount);
            return constraints;
        }
    };
    /**
     * Primary constraint factory
     */
    private static final ConstraintFactory PRIMARY_CONSTRAINT = new ConstraintFactory() {
        @Override
        public List<AbstractConstraint> make(DataSource dataSource, ObModeType dialectType, String database,
                String tableName, Map<String, AbstractDataType<?, ? extends Comparable<?>>> columnName2DataType,
                int totalCount) throws Throwable {
            List<AbstractConstraint> constraints;
            if (ObModeType.OB_ORACLE.equals(dialectType)) {
                constraints = getConstraints(dataSource, ORACLE_PRIMARY_CONSTRAINT_SQL, database, tableName,
                        columnName2DataType, totalCount, ObModeType.OB_ORACLE, null);
            } else if (ObModeType.OB_MYSQL.equals(dialectType)) {
                constraints = getConstraints(dataSource, MYSQL_PRIMARY_CONSTRAINT_SQL, database, tableName,
                        columnName2DataType, totalCount, ObModeType.OB_MYSQL, null);
            } else {
                throw new MockerException(MockerError.INVALID_OB_MODE);
            }
            validateConstraints(constraints, tableName, columnName2DataType, totalCount);
            return constraints;
        }
    };
    /**
     * Check constraint factory, but this kind of constraint is not supported yet. If this kind of
     * constraint exist, exception will be thrown
     */
    private static final ConstraintFactory CHECK_CONSTRAINT = new ConstraintFactory() {
        @Override
        public List<AbstractConstraint> make(DataSource dataSource, ObModeType dialectType, String database,
                String tableName, Map<String, AbstractDataType<?, ? extends Comparable<?>>> columnName2DataType,
                int totalCount) throws Throwable {
            String[] params = new String[] {database, tableName};
            if (ObModeType.OB_ORACLE.equals(dialectType)) {
                SqlUtil.executeQuery(dataSource, ORACLE_CHECK_CONSTRAINT_SQL, params,
                        new AbstractCallBack<ResultSet>() {
                            @Override
                            public void doOnSuccess(ResultSet result) throws Throwable {
                                List<ConstraintColumn> cols = SerializeUtil.getList(result, ConstraintColumn.class);
                                if (cols.size() != 0) {
                                    throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                                            "Check constraint is not support yet");
                                }
                            }

                            @Override
                            public void doOnFailure(ResultSet result, Throwable e) {
                                throw new MockerException(e);
                            }
                        });
            } else if (ObModeType.OB_MYSQL.equals(dialectType)) {
                // OB-Mysql does not support query check constraint
                return null;
            } else {
                throw new MockerException(MockerError.INVALID_OB_MODE);
            }
            return null;
        }
    };
    /**
     * Foreign constraint factory, but this kind of constraint is not supported yet. If this kind of
     * constraint exist, exception will be thrown
     */
    private static final ConstraintFactory FOREIGN_CONSTRAINT = new ConstraintFactory() {
        @Override
        public List<AbstractConstraint> make(DataSource dataSource, ObModeType dialectType, String database,
                String tableName, Map<String, AbstractDataType<?, ? extends Comparable<?>>> columnName2DataType,
                int totalCount) throws Throwable {
            if (ObModeType.OB_ORACLE.equals(dialectType)) {
                String[] params = new String[] {database, tableName};
                SqlUtil.executeQuery(dataSource, ORACLE_FOREIGN_CONSTRAINT_SQL, params,
                        new AbstractCallBack<ResultSet>() {
                            @Override
                            public void doOnSuccess(ResultSet result) throws Throwable {
                                List<ConstraintColumn> cols = SerializeUtil.getList(result, ConstraintColumn.class);
                                if (cols.size() != 0) {
                                    throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                                            "Foreign constraint is not support yet");
                                }
                            }

                            @Override
                            public void doOnFailure(ResultSet result, Throwable e) {
                                throw new MockerException(e);
                            }
                        });
            } else if (ObModeType.OB_MYSQL.equals(dialectType)) {
                // OB-Mysql does not support query foreign constraint
                return null;
            } else {
                throw new MockerException(MockerError.INVALID_OB_MODE);
            }
            return null;
        }
    };

    static {
        try {
            Field[] fields = ConstraintFactory.class.getDeclaredFields();
            for (Field field : fields) {
                Object instance = field.get(ConstraintFactory.class);
                if (Modifier.isStatic(field.getModifiers()) && instance instanceof ConstraintFactory) {
                    FACTORYNAME_2_FACTORYINSTANCE.putIfAbsent(field.getName(), (ConstraintFactory) instance);
                }
            }
        } catch (IllegalAccessException e) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, e.getMessage());
        }
    }

    /**
     * New Instance of a Constraint Factory
     *
     * @param dataSource datasource for constraint factory
     * @param dialectType ob mode enum(oracle, mysql)
     * @param database schema name for constraint
     * @param tableName table name for constraint
     * @param columnName2DataType table schema(map between column name column type)
     * @param totalCount data count
     */
    abstract public List<AbstractConstraint> make(DataSource dataSource, ObModeType dialectType, String database,
            String tableName, Map<String, AbstractDataType<?, ? extends Comparable<?>>> columnName2DataType,
            int totalCount) throws Throwable;

    /**
     * Verify method for constraint object eg. If a unique constraint restricts at most n different
     * pieces of data can be generated, but the input requires more than n pieces of data to be
     * generated, an error will be reported
     *
     * @param constraints list of constraint
     * @param tableName table name which is associated with constraint
     * @param columnName2DataType table schema
     * @param totalCount total count which is needed to generate
     * @throws MockerException exception will be thrown when error occured
     */
    private static void validateConstraints(List<AbstractConstraint> constraints, String tableName,
            Map<String, AbstractDataType<?, ? extends Comparable<?>>> columnName2DataType, int totalCount) {
        for (AbstractConstraint constraint : constraints) {
            Map<String, Integer> cols = constraint.columns().get(tableName);
            Set<String> colSet = cols.keySet();
            Long limitCount = 1L;
            for (String col : colSet) {
                AbstractDataType<?, ? extends Comparable<?>> dataType = columnName2DataType.get(col);
                Long typeLimit = dataType.distinctLimit();
                if (typeLimit > totalCount) {
                    limitCount = (long) totalCount;
                    break;
                }
                limitCount *= dataType.distinctLimit();
                if (limitCount > totalCount) {
                    break;
                }
            }
            if (limitCount < totalCount) {
                throw new MockerException(MockerError.PARAMETER_ERROR, String.format(
                        "The given data generator can only generate %d unique data for cols {%s}, but the goal is %d",
                        limitCount.intValue(), String.join(", ", colSet), totalCount));
            }
        }
    }

    protected static List<AbstractConstraint> getConstraints(DataSource dataSource, String sql, String database,
            String tableName, Map<String, AbstractDataType<?, ? extends Comparable<?>>> columnName2DataType,
            int totalCount, ObModeType obModeType, Validation validation) throws Throwable {
        List<AbstractConstraint> constraints = new ArrayList<>();
        SqlUtil.executeQuery(dataSource, sql, new String[] {database, tableName}, new AbstractCallBack<ResultSet>() {
            @Override
            public void doOnSuccess(ResultSet result) throws Throwable {
                List<ConstraintColumn> cols = SerializeUtil.getList(result, ConstraintColumn.class);
                Map<String, List<ConstraintColumn>> returnVal = reduce(cols);
                Set<Entry<String, List<ConstraintColumn>>> entrySet = returnVal.entrySet();
                for (Entry<String, List<ConstraintColumn>> entry : entrySet) {
                    List<ConstraintColumn> colsList = entry.getValue();
                    Map<String, Map<String, Integer>> columnMap = new HashMap<>();
                    for (ConstraintColumn item : colsList) {
                        if (validation != null) {
                            validation.validate(dataSource, item);
                        }
                        Map<String, Integer> map = columnMap.getOrDefault(item.getTableName(), new HashMap<>());
                        if (item.getPosition() instanceof BigDecimal) {
                            BigDecimal position = (BigDecimal) item.getPosition();
                            map.putIfAbsent(item.getColumnName(), position.intValue());
                        } else if (item.getPosition() instanceof Long) {
                            Long position = (Long) item.getPosition();
                            map.putIfAbsent(item.getColumnName(), position.intValue());
                        } else {
                            throw new MockerException(MockerError.ILLEGAL_RETURN_VALUE,
                                    "Position's type is not support");
                        }
                        columnMap.put(item.getTableName(), map);
                    }
                    Integer existCount = getTableRowCount(dataSource, tableName, obModeType);
                    AbstractConstraint constraint = new UniqueConstraint(entry.getKey(), database, tableName, columnMap,
                            totalCount + existCount);
                    initConstraint(dataSource, colsList, tableName, columnName2DataType, constraint, obModeType);
                    constraints.add(constraint);
                }
            }

            @Override
            public void doOnFailure(ResultSet result, Throwable e) {
                throw new MockerException(e);
            }
        });
        return constraints;
    }

    /**
     * Get total record count for a certain table
     *
     * @param dataSource datasource
     * @param table table name
     * @return total count for this table
     */
    private static Integer getTableRowCount(DataSource dataSource, String table, ObModeType modeType) throws Throwable {
        Validate.notNull(modeType, "ObModeType can not be null for ConstraintFactory#getTableRowCount");
        String sql = String.format("select count(*) from \"%s\"; ", DbObjectNameUtil.doubleCharToEscape(table, '"'));
        if (ObModeType.OB_MYSQL.equals(modeType)) {
            sql = String.format("select count(*) from `%s`; ", DbObjectNameUtil.doubleCharToEscape(table, '`'));
        }
        List<Integer> returnVal = new ArrayList<>();
        SqlUtil.executeQuery(dataSource, sql, null, new AbstractCallBack<ResultSet>() {
            @Override
            public void doOnSuccess(ResultSet result) throws Throwable {
                while (result.next()) {
                    returnVal.add(result.getInt(1));
                }
            }

            @Override
            public void doOnFailure(ResultSet result, Throwable e) {
                throw new MockerException(e);
            }
        });
        return returnVal.get(0);
    }

    /**
     * Init the constraints with the row that already exists in table
     *
     * @param dataSource data source
     * @param colsList column list which is associated with constraint
     * @param table table name
     * @param columnName2DataType table schema
     */
    private static void initConstraint(DataSource dataSource, List<ConstraintColumn> colsList, String table,
            Map<String, AbstractDataType<?, ? extends Comparable<?>>> columnName2DataType,
            AbstractConstraint constraint, ObModeType obModeType) throws Throwable {
        Validate.notNull(obModeType, "OBModeType can not be null for ConstraintFactory#initConstraint");
        String columnStr = colsList.stream().map(column -> {
            if (ObModeType.OB_ORACLE.equals(obModeType)) {
                return "\"" + DbObjectNameUtil.doubleCharToEscape(column.getColumnName(), '"') + "\"";
            }
            return "`" + DbObjectNameUtil.doubleCharToEscape(column.getColumnName(), '`') + "`";
        }).collect(Collectors.joining(","));
        String querySql =
                String.format("select %s from \"%s\"; ", columnStr, DbObjectNameUtil.doubleCharToEscape(table, '"'));
        if (ObModeType.OB_MYSQL.equals(obModeType)) {
            querySql =
                    String.format("select %s from `%s`; ", columnStr, DbObjectNameUtil.doubleCharToEscape(table, '`'));
        }
        SqlUtil.executeQuery(dataSource, querySql, null, new AbstractCallBack<ResultSet>() {
            @Override
            public void doOnSuccess(ResultSet result) throws Throwable {
                ResultSetMetaData metaData = result.getMetaData();
                int count = metaData.getColumnCount();
                while (result.next()) {
                    MockRowData mockRowData = new MockRowData();
                    for (int i = 0; i < count; i++) {
                        String columnName = metaData.getColumnLabel(i + 1);
                        AbstractDataType<?, ? extends Comparable<?>> dataType = columnName2DataType.get(columnName);
                        if (dataType == null) {
                            throw new MockerException(MockerError.ILLEGAL_RETURN_VALUE,
                                    String.format("Data type for column \"%s.%s\" can not be null", table, columnName));
                        }
                        Object value = result.getObject(i + 1);
                        mockRowData.addMockColumn(dataType.toMockColumn(columnName, value));
                    }
                    constraint.check(mockRowData);
                    constraint.mark(mockRowData);
                }
            }

            @Override
            public void doOnFailure(ResultSet result, Throwable e) {
                throw new MockerException(e);
            }
        });
    }

    /**
     * Reduce method, used to bind specific constraint columns and constraint names
     *
     * @param cols column list
     * @return map between constraint name and column associated with this constraint
     */
    private static Map<String, List<ConstraintColumn>> reduce(List<ConstraintColumn> cols) {
        Map<String, List<ConstraintColumn>> returnVal = new HashMap<>();
        for (ConstraintColumn col : cols) {
            List<ConstraintColumn> list = returnVal.getOrDefault(col.getConstraintName(), new ArrayList<>());
            list.add(col);
            returnVal.put(col.getConstraintName(), list);
        }
        return returnVal;
    }

    /**
     * Get a constraint factory instance by name
     *
     * @param factoryName name of constraint factory
     * @return Instance for constraint factory
     */
    public static ConstraintFactory getInstance(String factoryName) {
        ConstraintFactory returnVal = FACTORYNAME_2_FACTORYINSTANCE.get(factoryName);
        if (returnVal == null) {
            throw new MockerException(MockerError.UNKNOWN_DATA_TYPE);
        }
        return returnVal;
    }

    /**
     * Get all constraitn factory
     *
     * @return All constraint factory
     */
    public static List<ConstraintFactory> listInstances() {
        List<ConstraintFactory> returnVal = new ArrayList<>();
        Set<Entry<String, ConstraintFactory>> entries = FACTORYNAME_2_FACTORYINSTANCE.entrySet();
        for (Map.Entry<String, ConstraintFactory> entry : entries) {
            returnVal.add(entry.getValue());
        }
        return returnVal;
    }
}


/**
 * Verify interface, used to verify if the constraint is associated with virtual column
 *
 * @author yh263208
 * @date 2021-01-27 15:36
 * @since OBMOCKER_snapshot_0.1.0
 */
interface Validation {
    /**
     * The verification method is used to determine whether the constraint is a type that the mock can
     * handle. Currently, it only targets primary key constraints and unique constraints. Unique
     * constraints can be defined on virtual columns, so this check is mainly used on virtual columns to
     * check whether the constraints include virtual columns
     *
     * @param dataSource datasource
     * @param column column which is associated this constraint
     */
    void validate(DataSource dataSource, ConstraintColumn column) throws Throwable;
}


/**
 * Interface implementation for mysql mode
 *
 * @author yh263208
 * @date 2021-01-11 20:02
 * @since OBMOCKER_snaoshot_0.1.0
 */
class MysqlValidation implements Validation {
    /**
     * Query sql for verifing if there exists a virtual column
     */
    private static final String MYSQL_VALIDATE_SQL =
            "select TABLE_SCHEMA as OWNER,TABLE_NAME,COLUMN_NAME,DATA_TYPE,NUMERIC_PRECISION as DATA_PRECISION,NUMERIC_SCALE as "
                    + "DATA_SCALE,GENERATION_EXPRESSION from information_schema.columns where TABLE_SCHEMA=? and TABLE_NAME=? and COLUMN_NAME=?";

    @Override
    public void validate(DataSource dataSource, ConstraintColumn cols) throws Throwable {
        String[] params = new String[] {cols.getOwner(), cols.getTableName(), cols.getColumnName()};
        SqlUtil.executeQuery(dataSource, MYSQL_VALIDATE_SQL, params, new AbstractCallBack<ResultSet>() {
            @Override
            public void doOnSuccess(ResultSet result) throws Throwable {
                TableColumn tableCol = SerializeUtil.getObject(result, TableColumn.class);
                if (StringUtils.isNotBlank(tableCol.getExpression())) {
                    throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                            String.format("Virtual column \"%s.%s\" for constraint is not support yet",
                                    tableCol.getTableName(), tableCol.getColumnName()));
                }
            }

            @Override
            public void doOnFailure(ResultSet result, Throwable e) {
                throw new MockerException(e);
            }
        });
    }
}


/**
 * Interface implementation for oracle mode
 *
 * @author yh263208
 * @date 2021-01-11 20:03
 * @since OBMOCKER_snapshot_0.1.0
 */
class OracleValidation implements Validation {
    /**
     * Query sql for verifing if there exists a virtual column
     */
    private static final String ORACLE_VALIDATE_SQL =
            "select * from all_tab_cols where owner=? and table_name=? and column_name=?";

    @Override
    public void validate(DataSource dataSource, ConstraintColumn cols) throws Throwable {
        String[] params = new String[] {cols.getOwner(), cols.getTableName(), cols.getColumnName()};
        SqlUtil.executeQuery(dataSource, ORACLE_VALIDATE_SQL, params, new AbstractCallBack<ResultSet>() {
            @Override
            public void doOnSuccess(ResultSet result) throws Throwable {
                TableColumn tableCol = SerializeUtil.getObject(result, TableColumn.class);
                if ("YES".equals(tableCol.getVirtualColumn())) {
                    throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                            String.format("Virtual column \"%s.%s\" for constraint is not support yet",
                                    tableCol.getTableName(), tableCol.getColumnName()));
                }
            }

            @Override
            public void doOnFailure(ResultSet result, Throwable e) {
                throw new MockerException(e);
            }
        });
    }
}
