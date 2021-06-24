package com.oceanbase.tools.datamocker.core.write;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.datamocker.util.DbObjectNameUtil;
import com.oceanbase.tools.datamocker.util.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.Validate;

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
    private final ObModeType dialectType;
    private final MockerFile manager;
    private final String groupId;

    /**
     * The constructor writes a mock file, which is required
     *
     * @param manager mock file object
     * @param dialectType dialect type
     * @param database schema or database name
     * @param tableName table name
     */
    public SqlScriptWriter(MockerFile manager, ObModeType dialectType, String database,
            String tableName) {
        validateParam(manager, dialectType, database, tableName);
        this.database = database;
        this.tableName = tableName;
        this.manager = manager;
        this.dialectType = dialectType;
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
    public SqlScriptWriter(MockerFile manager, ObModeType dialectType, String database, String tableName,
            String groupId) {
        validateParam(manager, dialectType, database, tableName);
        this.database = database;
        this.tableName = tableName;
        this.manager = manager;
        this.dialectType = dialectType;
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
    private void validateParam(MockerFile manager, ObModeType dialectType, String database, String tableName) {
        Validate.notNull(manager, "File manager can not be null for SqlScriptWriter#validateParam");
        Validate.notNull(database, "DataBase can not be null for SqlScriptWriter#validateParam");
        Validate.notNull(tableName, "TableName can not be null for SqlScriptWriter#validateParam");
        if (!ObModeType.OB_ORACLE.equals(dialectType) && !ObModeType.OB_MYSQL.equals(dialectType)) {
            throw new MockerException(MockerError.INVALID_OB_MODE);
        }
    }

    @Override
    protected Long doWrite(List<MockRowData> rows) throws IOException {
        MockRowData firstRow = rows.get(0);
        Set<String> columnSet = firstRow.columnNames();
        List<String> columnList = new ArrayList<>(columnSet);
        StringBuffer sqlBuffer;
        if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
            sqlBuffer = new StringBuffer(
                    String.format("insert into \"%s\".\"%s\"(", DbObjectNameUtil.doubleCharToEscape(database, '"'),
                            DbObjectNameUtil.doubleCharToEscape(tableName, '"')));
        } else if (ObModeType.OB_MYSQL.equals(this.dialectType)) {
            sqlBuffer = new StringBuffer(
                    String.format("insert into `%s`.`%s`(", DbObjectNameUtil.doubleCharToEscape(database, '`'),
                            DbObjectNameUtil.doubleCharToEscape(tableName, '`')));
        } else {
            throw new MockerException(MockerError.INVALID_OB_MODE);
        }
        int columnLength = columnList.size();
        for (int i = 0; i < columnLength; i++) {
            String columnName = columnList.get(i);
            if (i == columnLength - 1) {
                if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
                    sqlBuffer.append(
                            String.format("\"%s\") values (", DbObjectNameUtil.doubleCharToEscape(columnName, '"')));
                } else {
                    sqlBuffer.append(
                            String.format("`%s`) values (", DbObjectNameUtil.doubleCharToEscape(columnName, '`')));
                }
            } else {
                if (ObModeType.OB_ORACLE.equals(this.dialectType)) {
                    sqlBuffer.append(String.format("\"%s\", ", DbObjectNameUtil.doubleCharToEscape(columnName, '"')));
                } else {
                    sqlBuffer.append(String.format("`%s`, ", DbObjectNameUtil.doubleCharToEscape(columnName, '`')));
                }
            }
        }
        String prefix = sqlBuffer.toString();
        List<String> sqlList = new ArrayList<>();
        for (MockRowData row : rows) {
            StringBuilder buffer = new StringBuilder(prefix);
            for (int i = 0; i < columnLength; i++) {
                String columnName = columnList.get(i);
                MockColumnData<?> mockColumn = row.getMockColumn(columnName);
                String value = mockColumn.getColumnValueString();
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
