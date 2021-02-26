package com.oceanbase.tools.datamocker.core.write;

import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.enums.DialectType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.DigestUtil;
import com.oceanbase.tools.datamocker.util.Pair;
import com.oceanbase.tools.datamocker.util.SqlUtil;
import com.oceanbase.tools.datamocker.util.SqlUtil.CallBack;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据库写出writer，用于将数据直接写出到数据库中
 *
 * @author yh263208
 * @date 2021-01-04 11:04
 * @since OBMOCKER_0.1.0_snapshot
 */
@Slf4j
public class DataBaseWriter extends AbstractMockWriter {
    /**
     * 获取一个数据库连接池，使用该连接池获取数据库连接进行数据写入
     */
    private DataSource dataSource;
    /**
     * 写入的目标库，如果建连接的时候指定了目标库该值也可以不填写
     */
    private String database;
    /**
     * 写入的目标表，该参数必传，指定传入的目标表
     */
    private String tableName;
    /**
     * OB的方言模式，默认为oracle模式
     */
    private DialectType dialectType = DialectType.OB_ORACLE;
    /**
     * 分组ID
     */
    private String groupId;

    /**
     * 构造函数写入一个数据源，该数据源是必须的
     *
     * @param dataSource  数据源
     * @param dialectType 方言类型
     * @param database    数据库名
     * @param tableName   表名
     */
    public DataBaseWriter(DataSource dataSource, DialectType dialectType, String database, String tableName) {
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
     * 构造函数写入一个数据源，该数据源是必须的
     *
     * @param dataSource  数据源
     * @param dialectType 方言类型
     * @param database    数据库名
     * @param tableName   表名
     * @param groupId     分组信息
     */
    public DataBaseWriter(DataSource dataSource, DialectType dialectType, String database,
            String tableName, String groupId) {
        validate(dataSource, dialectType, database, tableName);
        this.dataSource = dataSource;
        this.database = database;
        this.tableName = tableName;
        if (groupId == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "group id can not be null");
        }
        this.groupId = groupId;
    }

    /**
     * 验证构造函数的输入是否合法
     *
     * @param dataSource  数据源
     * @param database    目标写入的数据库
     * @param tableName   表名
     * @param dialectType 方言类型
     * @throws MockerException 验证失败则抛出异常
     */
    private void validate(DataSource dataSource, DialectType dialectType, String database, String tableName) {
        if (dataSource == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "data source can not be null");
            log.error("data source for DB writeIn writer is necessary", e);
            throw e;
        }
        if (database == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "database can not be null");
            log.error("database for DB writeIn writer is necessary", e);
            throw e;
        }
        if (tableName == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "table name can not be null");
            log.error("table name for DB writeIn writer is necessary", e);
            throw e;
        }
        if (dialectType != null) {
            if (!DialectType.OB_ORACLE.equals(dialectType) && !DialectType.OB_MYSQL.equals(dialectType)) {
                throw new MockerException(MockerError.INVALID_OB_MODE);
            }
            this.dialectType = dialectType;
        }
    }

    /**
     * 预检查方法，主要用于检查目标表是否存在，如果不存在则抛出异常
     *
     * @throws SQLException 抛出表或数据库不存在异常
     */
    private void preCheck() {
        String descSql;
        if (DialectType.OB_ORACLE.equals(this.dialectType)) {
            descSql = String.format("select count(*) from %s.\"%s\"", database, tableName);
        } else if (DialectType.OB_MYSQL.equals(this.dialectType)) {
            descSql = String.format("select count(*) from `%s`.`%s`", database, tableName);
        } else {
            throw new MockerException(MockerError.INVALID_OB_MODE);
        }
        SqlUtil.executeQuery(dataSource, descSql, null, new CallBack<ResultSet>() {
            @Override
            public void onComplete(ResultSet result) {}

            @Override
            public void onFailure(Throwable e) {
                throw new MockerException(e);
            }
        });
    }

    @Override
    protected Long doWrite(List<Map<String, Pair<AbstractDataType, Object>>> rows) throws Exception {
        preCheck();
        Map<String, ?> firstRow = rows.get(0);
        Set<String> columnSet = firstRow.keySet();
        List<String> columnList = new ArrayList<>(columnSet);
        StringBuffer sqlBuffer = null;
        if (DialectType.OB_ORACLE.equals(this.dialectType)) {
            sqlBuffer = new StringBuffer(String.format("insert into %s.\"%s\"(", database, tableName));
        } else if (DialectType.OB_MYSQL.equals(this.dialectType)) {
            sqlBuffer = new StringBuffer(String.format("insert into `%s`.`%s`(", database, tableName));
        }
        int columnLength = columnList.size();
        for (int i = 0; i < columnLength; i++) {
            String columnName = columnList.get(i);
            if (i == columnLength - 1) {
                if (DialectType.OB_ORACLE.equals(this.dialectType)) {
                    sqlBuffer.append(String.format("\"%s\") values (", columnName));
                } else if (DialectType.OB_MYSQL.equals(this.dialectType)) {
                    sqlBuffer.append(String.format("`%s`) values (", columnName));
                }
                for (int j = 0; j < columnLength; j++) {
                    if (j == columnLength - 1) {
                        sqlBuffer.append("?) ");
                    } else {
                        sqlBuffer.append("?,");
                    }
                }
            } else {
                if (DialectType.OB_ORACLE.equals(this.dialectType)) {
                    sqlBuffer.append(String.format("\"%s\", ", columnName));
                } else if (DialectType.OB_MYSQL.equals(this.dialectType)) {
                    sqlBuffer.append(String.format("`%s`, ", columnName));
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
        SqlUtil.executeBatch(dataSource, sqlBuffer.toString(), params, new CallBack<int[]>() {
            @Override
            public void onComplete(int[] result) {
                if (result != null) {
                    log.info("data base writer has writed a batch, effect row is {}", result.length);
                    returnVal.add((long) result.length);
                } else {
                    log.warn("database writer write process has been executed, nothing wrote");
                }
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("some errors happen when write data", e);
                throw new MockerException(e);
            }
        });
        return returnVal.get(0);
    }

    @Override
    public String groupId() {
        return this.groupId;
    }
}
