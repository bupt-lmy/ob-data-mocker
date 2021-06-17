package com.oceanbase.tools.datamocker.constraint;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import com.oceanbase.tools.datamocker.util.DbObjectNameUtil;
import com.oceanbase.tools.datamocker.util.Pair;
import com.oceanbase.tools.datamocker.util.SerializeUtil;
import com.oceanbase.tools.datamocker.util.SqlUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * oracle模式下约束对象的工厂类，用于根据配置实例化出约束对象
 *
 * @author yh263208
 * @date 2021-01-11 17:18
 * @since OBMOCKER-snapshot-0.1.0
 */
public abstract class ConstraintFactory {
    /**
     * oracle模式下唯一约束的查询SQL
     */
    private static final String ORACLE_UNIQUE_CONSTRAINT_SQL
            = "select o.* from (select * from all_constraints where "
              + "constraint_type='U') s left join all_cons_columns o on s"
              + ".OWNER=o.OWNER and s.CONSTRAINT_NAME=o.CONSTRAINT_NAME and "
              + "s.TABLE_NAME=o.TABLE_NAME where s.OWNER=? and s"
              + ".TABLE_NAME=?;";
    /**
     * mysql模式下唯一约束的查询SQL
     */
    private static final String MYSQL_UNIQUE_CONSTRAINT_SQL
            = "select CONSTRAINT_SCHEMA as OWNER, CONSTRAINT_NAME,"
              + "TABLE_NAME,COLUMN_NAME,ORDINAL_POSITION as POSITION from "
              + "information_schema.key_column_usage where "
              + "CONSTRAINT_NAME<>'PRIMARY' and CONSTRAINT_SCHEMA=? and "
              + "TABLE_NAME=?; ";
    /**
     * oracle模式下主键约束的查询SQL
     */
    private static final String ORACLE_PRIMARY_CONSTRAINT_SQL
            = "select o.* from (select * from all_constraints where "
              + "constraint_type='P') s left join all_cons_columns o on s"
              + ".OWNER=o.OWNER and s.CONSTRAINT_NAME=o.CONSTRAINT_NAME and "
              + "s.TABLE_NAME=o.TABLE_NAME where s.OWNER=? and s"
              + ".TABLE_NAME=?;";
    /**
     * mysql模式下主键约束的查询SQL
     */
    private static final String MYSQL_PRIMARY_CONSTRAINT_SQL
            = "select CONSTRAINT_SCHEMA as OWNER, CONSTRAINT_NAME,"
              + "TABLE_NAME,COLUMN_NAME,ORDINAL_POSITION as POSITION from "
              + "information_schema.key_column_usage where "
              + "CONSTRAINT_NAME='PRIMARY' and CONSTRAINT_SCHEMA=? and "
              + "TABLE_NAME=?; ";
    /**
     * oracle模式下检查约束的查询SQL
     */
    private static final String ORACLE_CHECK_CONSTRAINT_SQL
            = "select o.* from (select * from all_constraints where "
              + "constraint_type='C') s left join all_cons_columns o on s"
              + ".OWNER=o.OWNER and s.CONSTRAINT_NAME=o.CONSTRAINT_NAME and "
              + "s.TABLE_NAME=o.TABLE_NAME where s.OWNER=? and s"
              + ".TABLE_NAME=?;";
    /**
     * oracle模式下外键约束的查询SQL
     */
    private static final String ORACLE_FOREIGN_CONSTRAINT_SQL
            = "select o.* from (select * from all_constraints where "
              + "constraint_type='R') s left join all_cons_columns o on s"
              + ".OWNER=o.OWNER and s.CONSTRAINT_NAME=o.CONSTRAINT_NAME and "
              + "s.TABLE_NAME=o.TABLE_NAME where s.OWNER=? and s"
              + ".TABLE_NAME=?;";
    /**
     * 实例映射表
     */
    private static final Map<String, ConstraintFactory> FACTORYNAME_2_FACTORYINSTANCE = new HashMap<>();

    /**
     * 唯一约束
     */
    private static final ConstraintFactory UNIQUE_CONSTRAINT = new ConstraintFactory() {
        @Override
        public List<AbstractConstraint> make(DataSource dataSource, ObModeType dialectType, String database, String tableName,
                Map<String, AbstractDataType> columnName2DataType, int totalCount) throws Throwable {
            List<AbstractConstraint> constraints;
            if (ObModeType.OB_ORACLE.equals(dialectType)) {
                constraints = getConstraints(dataSource, ORACLE_UNIQUE_CONSTRAINT_SQL, database, tableName, columnName2DataType, totalCount,
                        ObModeType.OB_ORACLE,
                        new OracleValidation());
            } else if (ObModeType.OB_MYSQL.equals(dialectType)) {
                constraints = getConstraints(dataSource, MYSQL_UNIQUE_CONSTRAINT_SQL, database, tableName, columnName2DataType, totalCount,
                        ObModeType.OB_MYSQL,
                        new MysqlValidation());
            } else {
                throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                        String.format("\"%s\" mode is not support yet", dialectType == null ? "null" : dialectType.name()));
            }
            validateConstraints(constraints, tableName, columnName2DataType, totalCount);
            return constraints;
        }
    };
    /**
     * 主键约束
     */
    private static final ConstraintFactory PRIMARY_CONSTRAINT = new ConstraintFactory() {
        @Override
        public List<AbstractConstraint> make(DataSource dataSource, ObModeType dialectType, String database, String tableName,
                Map<String, AbstractDataType> columnName2DataType, int totalCount) throws Throwable {
            List<AbstractConstraint> constraints;
            if (ObModeType.OB_ORACLE.equals(dialectType)) {
                constraints = getConstraints(dataSource, ORACLE_PRIMARY_CONSTRAINT_SQL, database, tableName, columnName2DataType,
                        totalCount, ObModeType.OB_ORACLE, null);
            } else if (ObModeType.OB_MYSQL.equals(dialectType)) {
                constraints = getConstraints(dataSource, MYSQL_PRIMARY_CONSTRAINT_SQL, database, tableName, columnName2DataType,
                        totalCount, ObModeType.OB_MYSQL, null);
            } else {
                throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                        String.format("\"%s\" mode is not support yet", dialectType == null ? "null" : dialectType.name()));
            }
            validateConstraints(constraints, tableName, columnName2DataType, totalCount);
            return constraints;
        }
    };
    /**
     * 检查约束，暂不支持，监测到检查约束直接报错
     */
    private static final ConstraintFactory CHECK_CONSTRAINT = new ConstraintFactory() {
        @Override
        public List<AbstractConstraint> make(DataSource dataSource, ObModeType dialectType, String database, String tableName,
                Map<String, AbstractDataType> columnName2DataType, int totalCount) throws Throwable {
            String[] params = new String[] {database, tableName};
            if (ObModeType.OB_ORACLE.equals(dialectType)) {
                SqlUtil.executeQuery(dataSource, ORACLE_CHECK_CONSTRAINT_SQL, params, new AbstractCallBack<ResultSet>() {
                    @Override
                    public void doOnSuccess(ResultSet result) throws Throwable {
                        List<ConstraintColumn> cols = SerializeUtil.getList(result, ConstraintColumn.class);
                        if (cols.size() != 0) {
                            /**
                             * oracle模式不支持检查约束的模拟数据
                             * */
                            throw new MockerException(MockerError.NOT_SUPPORT_FEATURE, "Check constraint is not support yet");
                        }
                    }

                    @Override
                    public void doOnFailure(ResultSet result, Throwable e) {
                        throw new MockerException(e);
                    }
                });
            } else if (ObModeType.OB_MYSQL.equals(dialectType)) {
                // mysql模式目前不支持检查约束，在这里直接返回null
                return null;
            } else {
                throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                        String.format("\"%s\" mode is not support yet", dialectType == null ? "null" : dialectType.name()));
            }
            return null;
        }
    };
    /**
     * 外键约束，暂不支持，如果表中含有外键约束直接报错
     */
    private static final ConstraintFactory FOREIGN_CONSTRAINT = new ConstraintFactory() {
        @Override
        public List<AbstractConstraint> make(DataSource dataSource, ObModeType dialectType, String database, String tableName,
                Map<String, AbstractDataType> columnName2DataType, int totalCount) throws Throwable {
            if (ObModeType.OB_ORACLE.equals(dialectType)) {
                String[] params = new String[] {database, tableName};
                SqlUtil.executeQuery(dataSource, ORACLE_FOREIGN_CONSTRAINT_SQL, params, new AbstractCallBack<ResultSet>() {
                    @Override
                    public void doOnSuccess(ResultSet result) throws Throwable {
                        List<ConstraintColumn> cols = SerializeUtil.getList(result, ConstraintColumn.class);
                        if (cols.size() != 0) {
                            // oracle模式不支持带有外键的表的模拟数据，直接抛错
                            throw new MockerException(MockerError.NOT_SUPPORT_FEATURE, "Foreign constraint is not support yet");
                        }
                    }

                    @Override
                    public void doOnFailure(ResultSet result, Throwable e) {
                        throw new MockerException(e);
                    }
                });
            } else if (ObModeType.OB_MYSQL.equals(dialectType)) {
                // mysql模式目前无法从内部表中查询出检查约束，在这里直接返回null
                return null;
            } else {
                throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                        String.format("\"%s\" mode is not support yet", dialectType == null ? "null" : dialectType.name()));
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
     * 获取一个约束工厂对象
     *
     * @param dataSource          数据源
     * @param dialectType         方言类型
     * @param database            数据库名
     * @param tableName           表名
     * @param columnName2DataType 表结构
     * @param totalCount          一共要产生的数据量
     */
    abstract public List<AbstractConstraint> make(DataSource dataSource, ObModeType dialectType, String database, String tableName,
            Map<String, AbstractDataType> columnName2DataType, int totalCount) throws Throwable;

    /**
     * 校验约束对象
     *
     * @param constraints         约束集合
     * @param tableName           表名
     * @param columnName2DataType 表的schema
     * @param totalCount          要生成的数量
     * @throws MockerException 校验失败抛出异常
     */
    private static void validateConstraints(List<AbstractConstraint> constraints, String tableName,
            Map<String, AbstractDataType> columnName2DataType,
            int totalCount) {
        for (AbstractConstraint constraint : constraints) {
            Map<String, Integer> cols = constraint.columns().get(tableName);
            Set<String> colSet = cols.keySet();
            Long limitCount = 1L;
            for (String col : colSet) {
                AbstractDataType dataType = columnName2DataType.get(col);
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
                throw new MockerException(MockerError.PARAMETER_ERROR,
                        String.format("The given data generator can only generate %d unique data for cols {%s}, but the goal is %d",
                                limitCount.intValue(), colSet.stream().collect(Collectors.joining(", ")), totalCount));
            }
        }
    }

    protected static List<AbstractConstraint> getConstraints(DataSource dataSource, String sql, String database,
            String tableName, Map<String, AbstractDataType> columnName2DataType, int totalCount, ObModeType obModeType,
            Validation validation) throws Throwable {
        List<AbstractConstraint> constraints = new ArrayList<>();
        SqlUtil.executeQuery(dataSource, sql, new String[] {database, tableName}, new AbstractCallBack<ResultSet>() {
            @Override
            public void doOnSuccess(ResultSet result) throws Throwable {
                List<ConstraintColumn> cols = SerializeUtil.getList(result, ConstraintColumn.class);
                Map<String, List<ConstraintColumn>> returnVal = reduce(cols);
                Set<Entry<String, List<ConstraintColumn>>> entrySet = returnVal.entrySet();
                Iterator<Entry<String, List<ConstraintColumn>>> iter = entrySet.iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, List<ConstraintColumn>> entry = iter.next();
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
                            throw new MockerException(MockerError.ILLEGAL_RETURN_VALUE, "Position's type is not support");
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
     * 获取一个表的大小
     *
     * @param dataSource 数据源
     * @param table      表名
     * @return 返回已经存在的列
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
     * 获取一个表已经存在的列
     *
     * @param dataSource          数据源
     * @param colsList            约束关联列集合
     * @param table               表名
     * @param columnName2DataType 表结构
     * @return 返回已经存在的列
     */
    private static void initConstraint(DataSource dataSource, List<ConstraintColumn> colsList, String table,
            Map<String, AbstractDataType> columnName2DataType, AbstractConstraint constraint, ObModeType obModeType) throws Throwable {
        Validate.notNull(obModeType, "OBModeType can not be null for ConstraintFactory#initConstraint");
        String columnStr = colsList.stream().map(column -> {
            if (ObModeType.OB_ORACLE.equals(obModeType)) {
                return "\"" + DbObjectNameUtil.doubleCharToEscape(column.getColumnName(), '"') + "\"";
            }
            return "`" + DbObjectNameUtil.doubleCharToEscape(column.getColumnName(), '`') + "`";
        }).collect(Collectors.joining(","));
        String querySql = String.format("select %s from \"%s\"; ", columnStr, DbObjectNameUtil.doubleCharToEscape(table, '"'));
        if (ObModeType.OB_MYSQL.equals(obModeType)) {
            querySql = String.format("select %s from `%s`; ", columnStr, DbObjectNameUtil.doubleCharToEscape(table, '`'));
        }
        SqlUtil.executeQuery(dataSource, querySql, null, new AbstractCallBack<ResultSet>() {
            @Override
            public void doOnSuccess(ResultSet result) throws Throwable {
                ResultSetMetaData metaData = result.getMetaData();
                int count = metaData.getColumnCount();
                while (result.next()) {
                    Map<String, Pair<AbstractDataType, Object>> row = new HashMap<>();
                    for (int i = 0; i < count; i++) {
                        String columnName = metaData.getColumnLabel(i + 1);
                        AbstractDataType dataType = columnName2DataType.get(columnName);
                        if (dataType == null) {
                            throw new MockerException(MockerError.ILLEGAL_RETURN_VALUE,
                                    String.format("Data type for column \"%s.%s\" can not be null", table, columnName));
                        }
                        Object value = result.getObject(i + 1);
                        row.putIfAbsent(columnName, new Pair<>(dataType, value));
                    }
                    constraint.check(row);
                    constraint.mark(row);
                }
            }

            @Override
            public void doOnFailure(ResultSet result, Throwable e) {
                throw new MockerException(e);
            }
        });
    }

    /**
     * 归约方法，将多个约束列和具体的约束名绑定
     *
     * @param cols 约束关联列集合
     * @return 返回约束名和约束关联列的映射表
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
     * 根据名称获取一个类型工厂实例
     *
     * @param factoryName 工厂实例名称
     * @return 返回工厂实例对象
     */
    public static ConstraintFactory getInstance(String factoryName) {
        ConstraintFactory returnVal = FACTORYNAME_2_FACTORYINSTANCE.get(factoryName);
        if (returnVal == null) {
            throw new MockerException(MockerError.UNKNOWN_DATA_TYPE);
        }
        return returnVal;
    }

    /**
     * 获取所有的工厂类实例
     *
     * @return 返回所有的工厂类实例
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
 * 验证接口，主要是用于验证约束是否关联了虚拟列。验证失败则抛出异常
 *
 * @author yh263208
 * @date 2021-01-27 15:36
 * @since OBMOCKER_snapshot_0.1.0
 */
interface Validation {
    /**
     * 验证方法，通过该方法判断约束是否是mock能够处理的类型，目前仅针对主键约束和唯一约束。唯一约束可以在虚拟列上定义，因此本检查
     * 主要作用在虚拟列上，检测约束是否包含了虚拟列
     *
     * @param dataSource 数据源
     * @param column     约束关联到的列
     * @return 返回校验结果
     */
    void validate(DataSource dataSource, ConstraintColumn column) throws Throwable;
}

/**
 * mysql模式下的校验逻辑
 *
 * @author yh263208
 * @date 2021-01-11 20:02
 * @since OBMOCKER_snaoshot_0.1.0
 */
class MysqlValidation implements Validation {

    /**
     * mysql模式下检测是否有虚拟列的sql
     */
    private static final String MYSQL_VALIDATE_SQL
            = "select TABLE_SCHEMA as OWNER,TABLE_NAME,COLUMN_NAME,DATA_TYPE,NUMERIC_PRECISION as DATA_PRECISION,NUMERIC_SCALE as "
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
                            String.format("Virtual column \"%s.%s\" for constraint is not support yet", tableCol.getTableName(),
                                    tableCol.getColumnName()));
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
 * oracle模式下约束列的校验逻辑
 *
 * @author yh263208
 * @date 2021-01-11 20:03
 * @since OBMOCKER_snapshot_0.1.0
 */
class OracleValidation implements Validation {
    /**
     * oracle模式下验证是否有虚拟列的sql
     */
    private static final String ORACLE_VALIDATE_SQL = "select * from all_tab_cols where owner=? and table_name=? and column_name=?";

    @Override
    public void validate(DataSource dataSource, ConstraintColumn cols) throws Throwable {
        String[] params = new String[] {cols.getOwner(), cols.getTableName(), cols.getColumnName()};
        SqlUtil.executeQuery(dataSource, ORACLE_VALIDATE_SQL, params, new AbstractCallBack<ResultSet>() {
            @Override
            public void doOnSuccess(ResultSet result) throws Throwable {
                TableColumn tableCol = SerializeUtil.getObject(result, TableColumn.class);
                if ("YES".equals(tableCol.getVirtualColumn())) {
                    throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                            String.format("Virtual column \"%s.%s\" for constraint is not support yet", tableCol.getTableName(),
                                    tableCol.getColumnName()));
                }
            }

            @Override
            public void doOnFailure(ResultSet result, Throwable e) {
                throw new MockerException(e);
            }
        });
    }
}
