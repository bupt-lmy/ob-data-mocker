package com.oceanbase.tools.datamocker.core.write.output;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

/**
 * mock数据专用的数据连接池，用于封装数据库连接。和普通的数据库连接池不同的是这里严格限定了某一时刻和DB建立的最大数量的数据库连接数量
 *
 * @author yh263208
 * @date 2021-01-04 16:04
 * @since OBMOCKER_0.1.0_snapshot
 */
@Slf4j
public class MockerDataSource implements DataSource {
    /**
     * 数据库配置对象
     */
    private final DataBaseConfig config;
    /**
     * jdbc连接URL
     */
    private final String jdbcUrl;
    /**
     * 数据库连接参数
     */
    private final Map<String, String> connectParams;
    /**
     * 驱动名称，使用OB默认的驱动
     */
    private final static String DRIVER_CLASS_NAME = "com.alipay.oceanbase.obproxy.mysql.jdbc.Driver";
    /**
     * 数据库连接池初始化时新建连接的数量
     */
    private int minPoolSize = 10;
    /**
     * 数据库连接池最大数据库连接数量
     */
    private int maxPoolSize = 20;
    /**
     * 数据库连接数量从minPoolSize增长到maxPoolSize时的增益步长
     */
    private int increaseStep = 5;
    /**
     * 数据库连接池
     */
    private final Stack<Connection> connectionPool = new Stack<>();
    /**
     * 封装正在被使用的数据库连接
     */
    private final List<Connection> connectionInUse = new LinkedList<>();
    /**
     * 数据库里连接读锁
     */
    private final ReentrantLock lock = new ReentrantLock();
    /**
     * 等待条件，当连接池中没有更多连接可用时，调用线程在这个条件上挂起
     */
    private final Condition noMoreConnection = lock.newCondition();

    /**
     * 根据数据库连接配置信息构造一个连接池
     *
     * @param config 数据库连接信息
     */
    public MockerDataSource(DataBaseConfig config, Map<String, String> connectParam) throws SQLException {
        validate(config);
        this.config = config;
        this.connectParams = connectParam;
        this.jdbcUrl = genJDBCUrl();
        initPool();
    }

    /**
     * 连接池构造函数
     *
     * @param config       数据库连接配置对象
     * @param minPoolSize  连接池的最小大小
     * @param maxPoolSize  连接池的最大大小
     * @param increaseStep 数据库连接数木增益
     */
    public MockerDataSource(DataBaseConfig config, int minPoolSize, int maxPoolSize, int increaseStep,
            Map<String, String> connectParam) throws SQLException {
        validate(config);
        this.config = config;
        if (minPoolSize <= 0 || maxPoolSize <= 0 || increaseStep <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Min pool size, max pool size or increase step can not be equal to or less than zero");
        }
        if (minPoolSize > maxPoolSize) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Min pool size can not be bigger than max pool size");
        }
        this.minPoolSize = minPoolSize;
        this.increaseStep = increaseStep;
        this.maxPoolSize = maxPoolSize;
        this.connectParams = connectParam;
        this.jdbcUrl = genJDBCUrl();
        initPool();
    }

    /**
     * 初始化线程池，首先创建一个连接查看db是否可用
     */
    private void initPool() throws SQLException {
        StringBuilder username = new StringBuilder(this.config.getUser());
        if (StringUtils.isNotBlank(this.config.getTenant())) {
            username.append("@")
                    .append(this.config.getTenant());
        }
        if (StringUtils.isNotBlank(this.config.getCluster())) {
            username.append("#")
                    .append(this.config.getCluster());
        }
        getConnection(username.toString(), this.config.getPassword()).close();
        refresh();
    }

    /**
     * 校验数据库连接对象的信息合法性
     *
     * @param config 数据库连接配置对象
     * @throws MockerException 如果校验失败则抛出异常
     */
    private void validate(DataBaseConfig config) {
        if (StringUtils.isBlank(config.getHost()) || StringUtils.isBlank(config.getUser()) || StringUtils.isBlank(config.getPassword())
            || StringUtils.isBlank(config.getDefaultSchame()) || config.getPort() == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Database's config is illegal");
        }
    }

    /**
     * 获取数据库连接jdbc url的方法
     *
     * @return 返回数据库连接URL
     */
    private String genJDBCUrl() {
        StringBuilder buffer = new StringBuilder("jdbc:oceanbase://");
        buffer.append(this.config.getHost())
                .append(":")
                .append(this.config.getPort())
                .append("/")
                .append(this.config.getDefaultSchame());
        if (this.connectParams != null) {
            Set<Entry<String, String>> entrySet = this.connectParams.entrySet();
            String paramStr = entrySet.stream().map(stringStringEntry -> stringStringEntry.getKey() + "=" + stringStringEntry.getValue())
                    .collect(Collectors.joining("&"));
            buffer.append("?").append(paramStr);
        }
        return buffer.toString();
    }

    /**
     * 类初始化阶段
     * */
    static {
        try {
            DriverManager.setLoginTimeout(15);
            Class.forName(DRIVER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            log.error("Data source initialization failed", e);
        }
    }

    /**
     * 刷新连接池，当没有连接可用时调用此方法。该方法会清理已经被使用的连接
     */
    private void refresh() {
        lock.lock();
        try {
            int liveConnectionCount = this.connectionPool.size();
            List<Integer> deleteIndex = new ArrayList<>();
            for (int i = 0; i < this.connectionInUse.size(); i++) {
                if (!this.connectionInUse.get(i).isClosed() && this.connectionInUse.get(i).isValid(getLoginTimeout())) {
                    liveConnectionCount++;
                } else if (this.connectionInUse.get(i).isClosed()) {
                    deleteIndex.add(i);
                }
            }
            for (int i = 0; i < deleteIndex.size(); i++) {
                int realIndex = deleteIndex.get(i) - i;
                connectionInUse.remove(realIndex);
            }
            StringBuilder username = new StringBuilder(this.config.getUser());
            if (StringUtils.isNotBlank(this.config.getTenant())) {
                username.append("@")
                        .append(this.config.getTenant());
            }
            if (StringUtils.isNotBlank(this.config.getCluster())) {
                username.append("#")
                        .append(this.config.getCluster());
            }
            if (liveConnectionCount < minPoolSize) {
                int interval = minPoolSize - liveConnectionCount;
                log.info(
                        "The number of surviving connections is less than the minimum number of connections, start adding connections, "
                        + "liveConnectionsCount={}, minPoolSize={}, triggeredThreadName={}",
                        liveConnectionCount, liveConnectionCount + interval, Thread.currentThread().getName());
                for (int i = 0; i < interval; i++) {
                    this.connectionPool.push(getConnection(username.toString(), this.config.getPassword()));
                }
            } else if (liveConnectionCount > maxPoolSize) {
                int count = Math.min(liveConnectionCount - maxPoolSize, connectionPool.size());
                log.info(
                        "The number of surviving connections is greater than the maximum number of connections, start to reduce the "
                        + "number of connections, liveConnectionsCount={}, maxPoolSize={}, triggeredThreadName={}",
                        liveConnectionCount, liveConnectionCount - count, Thread.currentThread().getName());
                for (int i = 0; i < count; i++) {
                    try {
                        connectionPool.pop().close();
                    } catch (SQLException e) {
                        log.error("Fail to close the connection", e);
                    }
                }
            } else {
                int realStep = Math.min(maxPoolSize - liveConnectionCount, this.increaseStep);
                log.info(
                        "The number of surviving connections is between the maximum and minimum values, and the connections begin to expand, liveConnectionsCount={}, targetPoolSize={}, triggeredThreadName={}",
                        liveConnectionCount, liveConnectionCount + realStep, Thread.currentThread().getName());
                for (int i = 0; i < realStep; i++) {
                    this.connectionPool.push(getConnection(username.toString(), this.config.getPassword()));
                }
            }
            if (connectionPool.size() > 0) {
                noMoreConnection.signalAll();
            }
        } catch (SQLException e) {
            log.error("Fail to refresh connection pool", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        lock.lock();
        try {
            if (this.connectionPool.size() == 0) {
                log.warn("There are no more connections available in the connection pool, start refreshing, triggeredThreadName={}",
                        Thread.currentThread().getName());
                refresh();
                if (this.connectionPool.size() == 0) {
                    log.warn("No connection is released from the connection pool, the thread starts to wait, threadName={}",
                            Thread.currentThread().getName());
                    noMoreConnection.await(3, TimeUnit.SECONDS);
                    return getConnection();
                }
            }
            Connection connection = this.connectionPool.pop();
            if (!connection.isValid(getLoginTimeout()) || connection.isClosed()) {
                return getConnection();
            }
            this.connectionInUse.add(connection);
            log.debug(
                    "The thread gets the database connection from the connection pool successfully, threadName={}, currentPoolSize={}, "
                    + "connectionInUseSize={}",
                    Thread.currentThread().getName(), this.connectionPool.size(), this.connectionInUse.size());
            return connection;
        } catch (InterruptedException e) {
            log.error("Thread waiting for connection pool read lock failed, threadName={}", Thread.currentThread().getName(), e);
            throw new SQLException(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 清理逻辑，用于线程池销毁时清理没有使用的连接
     */
    public boolean clear() {
        if (lock.tryLock()) {
            try {
                int count = connectionPool.size();
                log.info("The database connection pool will be closed, the free database connection needs to be closed, currentPoolSize={}",
                        count);
                int clearSize = 0;
                for (int i = 0; i < count; i++) {
                    try {
                        connectionPool.pop().close();
                        clearSize++;
                    } catch (SQLException e) {
                        log.error("Fail to close the connection", e);
                    }
                }
                log.info("Free database connection closed completely, clearConnectionCount={}, failedConnectionCount={}", clearSize,
                        count - clearSize);
                count = this.connectionInUse.size();
                log.info("The database connection in use will be closed, connectionInUseCount={}", count);
                clearSize = 0;
                int closeSize = 0;
                int inUseCount = this.connectionInUse.size();
                for (int i = 0; i < inUseCount; i++) {
                    try {
                        if (!this.connectionInUse.get(i).isClosed()) {
                            this.connectionInUse.get(i).close();
                            clearSize++;
                        } else {
                            closeSize++;
                        }
                    } catch (SQLException e) {
                        log.error("Fail to close the connection in use", e);
                    }
                }
                log.info(
                        "The database connection pool is closed successfully, clearConnectionSize={}, closedByThreadConnectionSize={}, failedConnectionCount={}",
                        clearSize, closeSize, count - clearSize - closeSize);
            } finally {
                lock.unlock();
            }
            return true;
        }
        return false;
    }

    /**
     * 独立获取一个数据库连接，该连接不在连接池中托管
     *
     * @param username 数据库连接用户名
     * @param password 数据连接密码
     * @return 返回数据库连接
     * @throws SQLException 连接建立时可能会抛出异常
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(this.jdbcUrl, username, password);
    }

    /**
     * 设置login超时时间
     */
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        DriverManager.setLoginTimeout(seconds);
    }

    /**
     * 获取login超时时间
     */
    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    /**
     * 不支持
     */
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("not support for MockerDataSourceManager#getParentLogger method");
    }

    /**
     * 不支持
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException("not support for MockerDataSourceManager#unwrap method");
    }

    /**
     * 不支持
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException("not support for MockerDataSourceManager#isWrapperFor method");
    }

    /**
     * 不支持
     */
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException("not support for MockerDataSourceManager#getLogWriter method");
    }

    /**
     * 不支持
     */
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLFeatureNotSupportedException("not support for MockerDataSourceManager#setLogWriter method");
    }
}
