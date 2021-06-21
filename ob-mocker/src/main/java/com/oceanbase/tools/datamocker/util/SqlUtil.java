package com.oceanbase.tools.datamocker.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.alipay.oceanbase.jdbc.ServerPreparedStatement;
import com.alipay.oceanbase.jdbc.extend.datatype.INTERVALDS;
import com.alipay.oceanbase.jdbc.extend.datatype.INTERVALYM;

import com.oceanbase.tools.datamocker.core.task.AbstractCallBack;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.extern.slf4j.Slf4j;

/**
 * sql execution tool class, used to execute sql
 *
 * @author yh263208
 * @date 2021-01-26 21:59
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class SqlUtil {
    /**
     * Execute sql query
     *
     * @param connection Database Connectivity
     * @param sql sql text
     * @param params parameter
     * @param callBack Callback method
     */
    public static void executeQuery(Connection connection, String sql, Object[] params,
            AbstractCallBack<ResultSet> callBack)
            throws Throwable {
        if (connection == null || sql == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Connection or SQL can not be null");
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
            return;
        }
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params != null) {
                int length = params.length;
                for (int i = 0; i < length; i++) {
                    if (params[i] instanceof INTERVALYM || params[i] instanceof INTERVALDS) {
                        if (!(statement instanceof ServerPreparedStatement)) {
                            throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                                    "OceanBase have to support PS protocol for INTERVALYM or INTERVALDS");
                        }
                        if (params[i] instanceof INTERVALYM) {
                            ((ServerPreparedStatement) statement).setINTERVALYM(i + 1, (INTERVALYM) params[i]);
                        } else {
                            ((ServerPreparedStatement) statement).setINTERVALDS(i + 1, (INTERVALDS) params[i]);
                        }
                    } else {
                        statement.setObject(i + 1, params[i]);
                    }
                }
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (callBack != null) {
                    try {
                        callBack.onSuccess(resultSet);
                    } catch (Throwable e) {
                        log.error("Some errors happened when executeQuery call back method executed", e);
                        throw e;
                    }
                }
            }
        } catch (Throwable e) {
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
        }
    }

    /**
     * Execute sql query
     *
     * @param dataSource Database connection pool
     * @param sql sql text
     * @param params parameter
     * @param callBack callback method
     */
    public static void executeQuery(DataSource dataSource, String sql, Object[] params,
            AbstractCallBack<ResultSet> callBack)
            throws Throwable {
        if (dataSource == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Datasource can not be null");
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            executeQuery(connection, sql, params, callBack);
        } catch (Throwable e) {
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
        }
    }

    /**
     * Execute sql query
     *
     * @param connection Database connection
     * @param sql sql text
     * @param params parameter
     * @param callBack callback method
     */
    public static void executeUpdate(Connection connection, String sql, Object[] params,
            AbstractCallBack<Integer> callBack)
            throws Throwable {
        if (connection == null || sql == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Connection or SQL can not be null");
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
            return;
        }
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params != null) {
                int length = params.length;
                for (int i = 0; i < length; i++) {
                    if (params[i] instanceof INTERVALYM || params[i] instanceof INTERVALDS) {
                        if (!(statement instanceof ServerPreparedStatement)) {
                            throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                                    "OceanBase have to support ps protocol for INTERVALYM or INTERVALDS");
                        }
                        if (params[i] instanceof INTERVALYM) {
                            ((ServerPreparedStatement) statement).setINTERVALYM(i + 1, (INTERVALYM) params[i]);
                        } else {
                            ((ServerPreparedStatement) statement).setINTERVALDS(i + 1, (INTERVALDS) params[i]);
                        }
                    } else {
                        statement.setObject(i + 1, params[i]);
                    }
                }
            }
            if (callBack != null) {
                try {
                    callBack.onSuccess(statement.executeUpdate());
                } catch (Throwable e) {
                    log.error("Some errors happened when executeUpdate onSuccess call back method executed", e);
                    throw e;
                }
            }
        } catch (Throwable e) {
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
        }
    }

    /**
     * Execute sql query
     *
     * @param dataSource Database connection pool
     * @param sql sql text
     * @param params parameter
     * @param callBack callback method
     */
    public static void executeUpdate(DataSource dataSource, String sql, Object[] params,
            AbstractCallBack<Integer> callBack)
            throws Throwable {
        if (dataSource == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Datasource can not be null");
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            executeUpdate(connection, sql, params, callBack);
        } catch (Throwable e) {
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
        }
    }

    /**
     * Execute sql query
     *
     * @param connection Database connection
     * @param sql sql text
     * @param params parameter
     * @param callBack callback method
     */
    public static void executeBatch(Connection connection, String sql, Object[][] params,
            AbstractCallBack<int[]> callBack)
            throws Throwable {
        if (connection == null || sql == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Connection or sql can not be null");
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
            return;
        }
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params == null) {
                throw new MockerException(MockerError.FAIL_TO_EXECUTE_SQL, "Param can not be null for batch update");
            }
            int length = params.length;
            for (int i = 0; i < length; i++) {
                Object[] innerParams = params[i];
                if (innerParams == null) {
                    throw new MockerException(MockerError.FAIL_TO_EXECUTE_SQL,
                            "Param can not be null for batch update");
                }
                int innerLength = innerParams.length;
                for (int j = 0; j < innerLength; j++) {
                    if (innerParams[j] instanceof INTERVALYM || innerParams[j] instanceof INTERVALDS) {
                        if (!(statement instanceof ServerPreparedStatement)) {
                            throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                                    "OceanBase have to support PS protocol for INTERVALYM or INTERVALDS");
                        }
                        if (innerParams[j] instanceof INTERVALYM) {
                            ((ServerPreparedStatement) statement).setINTERVALYM(j + 1, (INTERVALYM) innerParams[j]);
                        } else {
                            ((ServerPreparedStatement) statement).setINTERVALDS(j + 1, (INTERVALDS) innerParams[j]);
                        }
                    } else {
                        statement.setObject(j + 1, innerParams[j]);
                    }
                }
                statement.addBatch();
            }
            int[] result = statement.executeBatch();
            connection.commit();
            statement.clearBatch();
            statement.clearParameters();
            if (callBack != null) {
                try {
                    callBack.onSuccess(result);
                } catch (Throwable e) {
                    log.error("Some errors happened when executeBatch onSuccess call back method executed", e);
                    throw e;
                }
            }
        } catch (Throwable e) {
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
        }
    }

    /**
     * Perform batch operations
     *
     * @param dataSource Database connection pool
     * @param sql sql text
     * @param params parameter
     * @param callBack callback method
     */
    public static void executeBatch(DataSource dataSource, String sql, Object[][] params,
            AbstractCallBack<int[]> callBack)
            throws Throwable {
        if (dataSource == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Datasource can not be null");
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            executeBatch(connection, sql, params, callBack);
        } catch (Throwable e) {
            if (callBack != null) {
                callBack.onFailure(null, e);
            }
        }
    }
}
