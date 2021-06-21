package com.oceanbase.tools.datamocker.core.write;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * SQL text generation primitive
 *
 * @author yh263208
 * @date 2021-01-05 20:47
 * @since OBMOCKER_snasphot_0.1.0
 */
@Slf4j
public class SqlScriptWriter extends AbstractMockWriter {
    /**
     * The written target library, if the target library is specified when the connection is established, this value can also be left blank
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
    private MockerFile manager;
    private String groupId;

    /**
     * The constructor writes a mock file, which is required
     *
     * @param manager     mock file object
     * @param dialectType dialect type
     * @param database    schema or database name
     * @param tableName   table name
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
     * The constructor writes a mock file, which is required
     *
     * @param manager mock file object
     * @param dialectType dialect type
     * @param database schema or database name
     * @param tableName table name
     * @param groupId group id
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
     * Validation primitive input parameters
     *
     * @param manager mock file
     * @param dialectType dialect type
     * @param database database or schema name
     * @param tableName table name
     * @throws MockerException An exception is thrown when verification fails
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
        String result = String.join("", sqlList);
        this.manager.write(result.getBytes(), 0, result.getBytes().length, true);
        return (long) rows.size();
    }

    @Override
    public String groupId() {
        return this.groupId;
    }
}
