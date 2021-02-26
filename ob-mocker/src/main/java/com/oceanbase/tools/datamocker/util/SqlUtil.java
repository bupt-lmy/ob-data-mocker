package com.oceanbase.tools.datamocker.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.alipay.oceanbase.jdbc.ServerPreparedStatement;
import com.alipay.oceanbase.jdbc.extend.datatype.INTERVALDS;
import com.alipay.oceanbase.jdbc.extend.datatype.INTERVALYM;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * sql执行工具类，用于执行sql
 *
 * @author yh263208
 * @date 2021-01-26 21:59
 * @since OBMOCKER_snapshot_0.1.0
 */
public class SqlUtil {
    /**
     * 执行sql查询
     *
     * @param connection 数据库连接
     * @param sql        sql文本
     * @param params     参数
     * @param callBack   回调函数
     */
    public static void executeQuery(Connection connection, String sql, Object[] params, CallBack<ResultSet> callBack) {
        if (connection == null || sql == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "connection or sql can not be null");
            if (callBack != null) {
                callBack.onFailure(e);
            }
            return;
        }
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            if (callBack != null) {
                callBack.onFailure(e);
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params != null) {
                int length = params.length;
                for (int i = 0; i < length; i++) {
                    if (params[i] instanceof INTERVALYM || params[i] instanceof INTERVALDS) {
                        if (!(statement instanceof ServerPreparedStatement)) {
                            throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                                    "ob server have to support ps protocol for INTERVALYM or INTERVALDS");
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
                    callBack.onComplete(resultSet);
                }
            }
        } catch (Exception e) {
            if (callBack != null) {
                callBack.onFailure(e);
            }
        }
    }

    /**
     * 执行sql查询
     *
     * @param dataSource 数据库连接池
     * @param sql        sql文本
     * @param params     参数
     * @param callBack   回调函数
     */
    public static void executeQuery(DataSource dataSource, String sql, Object[] params, CallBack<ResultSet> callBack) {
        if (dataSource == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "datasource can not be null");
            if (callBack != null) {
                callBack.onFailure(e);
            }
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            executeQuery(connection, sql, params, callBack);
        } catch (Exception e) {
            if (callBack != null) {
                callBack.onFailure(e);
            }
        }
    }

    /**
     * 执行sql查询
     *
     * @param connection 数据库连接
     * @param sql        sql文本
     * @param params     参数
     * @param callBack   回调函数
     */
    public static void executeUpdate(Connection connection, String sql, Object[] params, CallBack<Integer> callBack) {
        if (connection == null || sql == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "connection or sql can not be null");
            if (callBack != null) {
                callBack.onFailure(e);
            }
            return;
        }
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            if (callBack != null) {
                callBack.onFailure(e);
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params != null) {
                int length = params.length;
                for (int i = 0; i < length; i++) {
                    if (params[i] instanceof INTERVALYM || params[i] instanceof INTERVALDS) {
                        if (!(statement instanceof ServerPreparedStatement)) {
                            throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                                    "ob server have to support ps protocol for INTERVALYM or INTERVALDS");
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
                callBack.onComplete(statement.executeUpdate());
            }
        } catch (Exception e) {
            if (callBack != null) {
                callBack.onFailure(e);
            }
        }
    }

    /**
     * 执行sql查询
     *
     * @param dataSource 数据库连接池
     * @param sql        sql文本
     * @param params     参数
     * @param callBack   回调函数
     */
    public static void executeUpdate(DataSource dataSource, String sql, Object[] params, CallBack<Integer> callBack) {
        if (dataSource == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "datasource can not be null");
            if (callBack != null) {
                callBack.onFailure(e);
            }
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            executeUpdate(connection, sql, params, callBack);
        } catch (Exception e) {
            if (callBack != null) {
                callBack.onFailure(e);
            }
        }
    }

    /**
     * 执行sql批处理更新操作
     *
     * @param connection 数据库连接
     * @param sql        sql文本
     * @param params     参数
     * @param callBack   回调函数
     */
    public static void executeBatch(Connection connection, String sql, Object[][] params, CallBack<int[]> callBack) {
        if (connection == null || sql == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "connection or sql can not be null");
            if (callBack != null) {
                callBack.onFailure(e);
            }
            return;
        }
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            if (callBack != null) {
                callBack.onFailure(e);
            }
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params == null) {
                throw new MockerException(MockerError.FAIL_TO_EXECUTE_SQL, "param can not be null for batch update");
            }
            int length = params.length;
            for (int i = 0; i < length; i++) {
                Object[] innerParams = params[i];
                if (innerParams == null) {
                    throw new MockerException(MockerError.FAIL_TO_EXECUTE_SQL, "param can not be null for batch update");
                }
                int innerLength = innerParams.length;
                for (int j = 0; j < innerLength; j++) {
                    if (innerParams[j] instanceof INTERVALYM || innerParams[j] instanceof INTERVALDS) {
                        if (!(statement instanceof ServerPreparedStatement)) {
                            throw new MockerException(MockerError.NOT_SUPPORT_FEATURE,
                                    "ob server have to support ps protocol for INTERVALYM or INTERVALDS");
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
                callBack.onComplete(result);
            }
        } catch (Exception e) {
            if (callBack != null) {
                callBack.onFailure(e);
            }
        }
    }

    /**
     * 执行sql批处理更新操作
     *
     * @param dataSource 数据库连接池
     * @param sql        sql文本
     * @param params     参数
     * @param callBack   回调函数
     */
    public static void executeBatch(DataSource dataSource, String sql, Object[][] params, CallBack<int[]> callBack) {
        if (dataSource == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "datasource can not be null");
            if (callBack != null) {
                callBack.onFailure(e);
            }
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            executeBatch(connection, sql, params, callBack);
        } catch (Exception e) {
            if (callBack != null) {
                callBack.onFailure(e);
            }
        }
    }

    /**
     * 回调函数
     *
     * @author yh263208
     * @date 2021-01-27 10:16
     * @since OBMOCKER_0.1.0_snapshot
     */
    public interface CallBack<T> {
        /**
         * sql执行完成后的回调函数
         *
         * @param result 返回回调结果
         * @throws Exception 可能会抛出异常
         */
        void onComplete(T result) throws Exception;

        /**
         * 程序异常时的回调函数
         *
         * @param e 上层抛出的异常
         */
        void onFailure(Throwable e);
    }
}
