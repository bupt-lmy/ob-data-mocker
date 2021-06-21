package com.oceanbase.tools.datamocker.core.write;

import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.core.task.AbstractCallBack;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.DbObjectNameUtil;
import com.oceanbase.tools.datamocker.util.DigestUtil;
import com.oceanbase.tools.datamocker.util.Pair;
import com.oceanbase.tools.datamocker.util.SqlUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * The database write writer is used to write data directly to the database
 *
 * @author yh263208
 * @date 2021-01-04 11:04
 * @since OBMOCKER_0.1.0_snapshot
 */
@Slf4j
public class DataBaseWriter extends AbstractMockWriter {
    /**
     * Get a database connection pool, use the connection pool to get database connections for data
     * writing
     */
    private final DataSource dataSource;
    /**
     * The written target library, if the target library is specified when the connection is
     * established, this value can also be left blank
     */
    private final String database;
    /**
     * The target table to be written, this parameter must be passed, specify the incoming target table
     */
    private final String tableName;
    /**
     * The dialect mode of OB, the default is oracle mode
     */
    private ObModeType dialectType = ObModeType.OB_ORACLE;
    private final String groupId;

    /**
     * The constructor writes a data source, which is required
     *
     * @param dataSource datasource
     * @param dialectType dialect type
     * @param database schema or database name
     * @param tableName table name
     */
    public DataBaseWriter(DataSource dataSource, ObModeType dialectType, String database, String tableName) {
        validate(dataSource, dialectType, database, tableName);
        this.dataSource = dataSource;
        this.database = database;
        this.tableName = tableName;
        try {
            this.groupId = DigestUtil.getToken(this.dialectType.name() + this.database + this.tableName);
        } catch (NoSuchAlgorithmException e) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, e.getMessage());
        }
    }

    /**
     * The constructor writes a data source, which is required
     *
     * @param dataSource datasource
     * @param dialectType dialect type
     * @param database schema or database name
     * @param tableName table name
     * @param groupId group Id for Database writer
     */
    public DataBaseWriter(DataSource dataSource, ObModeType dialectType, String database,
            String tableName, String groupId) {
        validate(dataSource, dialectType, database, tableName);
        this.dataSource = dataSource;
        this.database = database;
        this.tableName = tableName;
        if (groupId == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Group id can not be null");
        }
        this.groupId = groupId;
    }

    /**
     * Verify that the input to the constructor is legal
     *
     * @param dataSource datasource, can not be null
     * @param database database name or schema name
     * @param tableName table name
     * @param dialectType dialect type
     * @throws MockerException An exception is thrown if verification fails
     */
    private void validate(DataSource dataSource, ObModeType dialectType, String database, String tableName) {
        if (dataSource == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Datasource can not be null");
            log.error("JDBC writer is missing data source", e);
            throw e;
        }
        if (database == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Database can not be null");
            log.error("JDBC writer is missing schema name", e);
            throw e;
        }
        if (tableName == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Table name can not be null");
            log.error("JDBC writer is missing table name", e);
            throw e;
        }
        if (dialectType != null) {
            if (!ObModeType.OB_ORACLE.equals(dialectType) && !ObModeType.OB_MYSQL.equals(dialectType)) {
                throw new MockerException(MockerError.INVALID_OB_MODE);
            }
            this.dialectType = dialectType;
        }
    }

    /**
     * Pre-check method, mainly used to check whether the target table exists, if it does not exist,
     * throw an exception
     *
     * @throws SQLException Throw a table or database does not exist exception
     */
    private void preCheck() throws Throwable {
        String descSql;
        if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
            descSql = String.format("select count(*) from \"%s\".\"%s\"",
                    DbObjectNameUtil.doubleCharToEscape(database, '"'),
                    DbObjectNameUtil.doubleCharToEscape(tableName, '"'));
        } else if (ObModeType.OB_MYSQL.equals(this.dialectType)) {
            descSql =
                    String.format("select count(*) from `%s`.`%s`", DbObjectNameUtil.doubleCharToEscape(database, '`'),
                            DbObjectNameUtil.doubleCharToEscape(tableName, '`'));
        } else {
            throw new MockerException(MockerError.INVALID_OB_MODE);
        }
        SqlUtil.executeQuery(dataSource, descSql, null, new AbstractCallBack<ResultSet>() {
            @Override
            public void doOnSuccess(ResultSet result) {}

            @Override
            public void doOnFailure(ResultSet result, Throwable e) {
                throw new MockerException(e);
            }
        });
    }

    @Override
    protected Long doWrite(List<Map<String, Pair<AbstractDataType, Object>>> rows) throws Throwable {
        preCheck();
        Map<String, ?> firstRow = rows.get(0);
        Set<String> columnSet = firstRow.keySet();
        List<String> columnList = new ArrayList<>(columnSet);
        StringBuffer sqlBuffer = null;
        if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
            sqlBuffer = new StringBuffer(
                    String.format("insert into \"%s\".\"%s\"(", DbObjectNameUtil.doubleCharToEscape(database, '"'),
                            DbObjectNameUtil.doubleCharToEscape(tableName, '"')));
        } else if (ObModeType.OB_MYSQL.equals(this.dialectType)) {
            sqlBuffer = new StringBuffer(
                    String.format("insert into `%s`.`%s`(", DbObjectNameUtil.doubleCharToEscape(database, '`'),
                            DbObjectNameUtil.doubleCharToEscape(tableName, '`')));
        }
        int columnLength = columnList.size();
        for (int i = 0; i < columnLength; i++) {
            String columnName = columnList.get(i);
            if (i == columnLength - 1) {
                if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
                    sqlBuffer.append(
                            String.format("\"%s\") values (", DbObjectNameUtil.doubleCharToEscape(columnName, '"')));
                } else if (ObModeType.OB_MYSQL.equals(this.dialectType)) {
                    sqlBuffer.append(
                            String.format("`%s`) values (", DbObjectNameUtil.doubleCharToEscape(columnName, '`')));
                }
                for (int j = 0; j < columnLength; j++) {
                    if (j == columnLength - 1) {
                        sqlBuffer.append("?) ");
                    } else {
                        sqlBuffer.append("?,");
                    }
                }
            } else {
                if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
                    sqlBuffer.append(String.format("\"%s\", ", DbObjectNameUtil.doubleCharToEscape(columnName, '"')));
                } else if (ObModeType.OB_MYSQL.equals(this.dialectType)) {
                    sqlBuffer.append(String.format("`%s`, ", DbObjectNameUtil.doubleCharToEscape(columnName, '`')));
                }
            }
        }
        int rowLength = rows.size();
        Object[][] params = new Object[rows.size()][];
        for (int j = 0; j < rowLength; j++) {
            Map<String, Pair<AbstractDataType, Object>> row = rows.get(j);
            Object[] innerParam = new Object[columnLength];
            for (int i = 0; i < columnLength; i++) {
                String columnName = columnList.get(i);
                innerParam[i] = row.get(columnName).getValue();
            }
            params[j] = innerParam;
        }
        List<Long> returnVal = new ArrayList<>();
        SqlUtil.executeBatch(dataSource, sqlBuffer.toString(), params, new AbstractCallBack<int[]>() {
            @Override
            public void doOnSuccess(int[] result) {
                if (result != null) {
                    log.info("JDBC writer writes a batch successfully, effectRow={}", result.length);
                    returnVal.add((long) result.length);
                } else {
                    log.warn("JDBC writer has finished writing, but no data has been written");
                }
            }

            @Override
            public void doOnFailure(int[] result, Throwable e) throws Throwable {
                log.error("JDBC writer failed to write", e);
                throw e;
            }
        });
        return returnVal.get(0);
    }

    @Override
    public String groupId() {
        return this.groupId;
    }
}
