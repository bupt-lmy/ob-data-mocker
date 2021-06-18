package com.oceanbase.tools.datamocker.core.write;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.DbObjectNameUtil;
import com.oceanbase.tools.datamocker.util.DigestUtil;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.extern.slf4j.Slf4j;

/**
 * sql文本生成原语
 *
 * @author yh263208
 * @date 2021-01-05 20:47
 * @since OBMOCKER_snasphot_0.1.0
 */
@Slf4j
public class SqlScriptWriter extends AbstractMockWriter {
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
    private ObModeType dialectType = ObModeType.OB_ORACLE;
    /**
     * 文件管理器
     */
    private MockerFile manager = null;
    /**
     * 该原语的分组ID
     */
    private String groupId = null;

    /**
     * 构造函数写入一个数据源，该数据源是必须的
     *
     * @param manager     文件管理器对象
     * @param dialectType 方言类型
     * @param database    数据库名
     * @param tableName   表名
     */
    public SqlScriptWriter(MockerFile manager, ObModeType dialectType, String database,
            String tableName) {
        validateParam(manager, dialectType, database, tableName);
        this.database = database;
        this.tableName = tableName;
        this.manager = manager;
        try {
            this.groupId = DigestUtil.getToken(manager.getFile().getAbsolutePath());
        } catch (NoSuchAlgorithmException e) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, e.getMessage());
        }
    }

    /**
     * 构造函数写入一个数据源，该数据源是必须的
     *
     * @param manager     文件管理器对象
     * @param dialectType 方言类型
     * @param database    数据库名
     * @param tableName   表名
     * @param groupId     分组ID
     */
    public SqlScriptWriter(MockerFile manager, ObModeType dialectType, String database,
            String tableName, String groupId) {
        validateParam(manager, dialectType, database, tableName);
        this.database = database;
        this.tableName = tableName;
        this.manager = manager;
        if (groupId == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Group id can not be null");
        }
        this.groupId = groupId;
    }

    /**
     * 验证原语的输入参数
     *
     * @param manager     文件管理器
     * @param dialectType 方言类型
     * @param database    数据库名或schema名
     * @param tableName   表名
     * @throws MockerException 验证失败抛出异常
     */
    private void validateParam(MockerFile manager, ObModeType dialectType, String database,
            String tableName) {
        if (manager == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "File manager can not be null");
            log.error("SQL script writer is missing file manager", e);
            throw e;
        }
        if (database == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Database can not be null");
            log.error("SQL script writer is missing schema name", e);
            throw e;
        }
        if (tableName == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Table name can not be null");
            log.error("SQL script writer is missing table name", e);
            throw e;
        }
        if (dialectType != null) {
            if (!ObModeType.OB_ORACLE.equals(dialectType) && !ObModeType.OB_MYSQL.equals(dialectType)) {
                throw new MockerException(MockerError.INVALID_OB_MODE);
            }
            this.dialectType = dialectType;
        }
    }

    @Override
    protected Long doWrite(List<Map<String, Pair<AbstractDataType, Object>>> rows) throws IOException {
        Map<String, ?> firstRow = rows.get(0);
        Set<String> columnSet = firstRow.keySet();
        List<String> columnList = new ArrayList<>(columnSet);
        StringBuffer sqlBuffer = null;
        if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
            sqlBuffer = new StringBuffer(String.format("insert into \"%s\".\"%s\"(", DbObjectNameUtil.doubleCharToEscape(database, '"'),
                    DbObjectNameUtil.doubleCharToEscape(tableName, '"')));
        } else if (ObModeType.OB_MYSQL.equals(this.dialectType)) {
            sqlBuffer = new StringBuffer(String.format("insert into `%s`.`%s`(", DbObjectNameUtil.doubleCharToEscape(database, '`'),
                    DbObjectNameUtil.doubleCharToEscape(tableName, '`')));
        }
        int columnLength = columnList.size();
        for (int i = 0; i < columnLength; i++) {
            String columnName = columnList.get(i);
            if (i == columnLength - 1) {
                if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
                    sqlBuffer.append(String.format("\"%s\") values (", DbObjectNameUtil.doubleCharToEscape(columnName, '"')));
                } else if (ObModeType.OB_MYSQL.equals(this.dialectType)) {
                    sqlBuffer.append(String.format("`%s`) values (", DbObjectNameUtil.doubleCharToEscape(columnName, '`')));
                }
            } else {
                if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
                    sqlBuffer.append(String.format("\"%s\", ", DbObjectNameUtil.doubleCharToEscape(columnName, '"')));
                } else if (ObModeType.OB_MYSQL.equals(this.dialectType)) {
                    sqlBuffer.append(String.format("`%s`, ", DbObjectNameUtil.doubleCharToEscape(columnName, '`')));
                }
            }
        }
        String prefix = sqlBuffer.toString();
        List<String> sqlList = new ArrayList<>();
        for (Map<String, Pair<AbstractDataType, Object>> row : rows) {
            StringBuffer buffer = new StringBuffer(prefix);
            for (int i = 0; i < columnLength; i++) {
                String columnName = columnList.get(i);
                Pair<AbstractDataType, Object> pair = row.get(columnName);
                String value = pair.getKey().toString(pair.getValue());
                if (value == null) {
                    throw new MockerException(MockerError.ILLEGAL_RETURN_VALUE,
                            String.format("Value for column \"%s\" is null", columnName));
                }
                if (i == columnLength - 1) {
                    buffer.append(String.format("%s); ", value));
                } else {
                    buffer.append(String.format("%s,", value));
                }
            }
            sqlList.add(buffer.append("\n").toString());
        }
        String result = sqlList.stream().collect(Collectors.joining());
        this.manager.write(result.getBytes(), 0, result.getBytes().length, true);
        return (long) rows.size();
    }

    @Override
    public String groupId() {
        return this.groupId;
    }
}
