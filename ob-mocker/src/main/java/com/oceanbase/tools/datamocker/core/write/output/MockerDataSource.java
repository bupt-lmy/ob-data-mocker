/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.tools.datamocker.core.write.output;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.apache.commons.lang.Validate;

/**
 * A data connection pool dedicated to mock data is used to encapsulate database connections. Unlike
 * ordinary database connection pools, the maximum number of database connections that can be
 * established with DB at a certain time is strictly limited here
 *
 * @author yh263208
 * @date 2021-01-04 16:04
 * @since OBMOCKER_0.1.0_snapshot
 */
@Slf4j
public class MockerDataSource implements DataSource {
    private final DataBaseConfig config;
    private final String jdbcUrl;
    /**
     * Database connection parameters
     */
    private final Map<String, String> connectParams;
    /**
     * Driver name, use OB default driver
     */
    private final static String DRIVER_CLASS_NAME = "com.alipay.oceanbase.obproxy.mysql.jdbc.Driver";
    /**
     * The number of new connections when the database connection pool is initialized
     */
    private int minPoolSize = 10;
    /**
     * Maximum number of database connections in the database connection pool
     */
    private int maxPoolSize = 20;
    /**
     * The gain step size when the number of database connections increases from minPoolSize to
     * maxPoolSize
     */
    private int increaseStep = 5;
    /**
     * Database connection pool
     */
    private final Stack<MockerConnection> connectionPool = new Stack<>();
    /**
     * Encapsulate the database connection being used
     */
    private final List<MockerConnection> connectionInUse = new LinkedList<>();
    /**
     * Connection read lock in the database
     */
    private final ReentrantLock lock = new ReentrantLock();
    /**
     * Waiting for the condition, when there are no more connections available in the connection pool,
     * the calling thread hangs on this condition
     */
    private final Condition noMoreConnection = lock.newCondition();
    /**
     * Counter for Connection
     */
    private final AtomicInteger connectionCounter = new AtomicInteger(0);

    /**
     * Packaged Mock Connection, used to override the close method of the connection object to achieve
     * connection reuse
     *
     * @author yh263208
     * @date 2021-06-25 20:30
     * @since OB_MOCKER_0.1.2
     */
    private static class MockerConnection extends ConnectionWrapper {
        /**
         * Connection Pool
         */
        private final MockerDataSource dataSource;
        /**
         * Connection Id to identify a connection
         */
        private final int connectionId;
        private boolean ifReuse = false;

        /**
         * Default Constructor
         *
         * @param dataSource connection pool for this connection
         * @param connection connection object
         */
        public MockerConnection(MockerDataSource dataSource, Connection connection, int connectionId, boolean ifReuse)
                throws SQLException {
            super(connection);
            Validate.notNull(dataSource, "DataSource can not be null for MockerConnection");
            if (connection.isClosed() || !connection.isValid(DriverManager.getLoginTimeout())) {
                throw new MockerException("Connection' status is illegal");
            }
            this.connectionId = connectionId;
            this.dataSource = dataSource;
            this.ifReuse = ifReuse;
        }

        /**
         * Real connection method
         *
         * @exception SQLException exception will be thrown when close operation failed
         */
        public void closeConnection() throws SQLException {
            log.debug("Close Connection from the pool, connectionId={}", connectionId);
            super.close();
        }

        @Override
        public void close() throws SQLException {
            if (!this.ifReuse) {
                super.close();
                return;
            }
            if (dataSource.lock.tryLock()) {
                try {
                    Iterator<MockerConnection> iter = dataSource.connectionInUse.iterator();
                    while (iter.hasNext()) {
                        MockerConnection mockerConnection = iter.next();
                        if (mockerConnection.equals(this)) {
                            if (mockerConnection.isValid(DriverManager.getLoginTimeout())) {
                                log.debug("Reuse the connection, connectionId={}", connectionId);
                                dataSource.connectionPool.push(mockerConnection);
                            } else {
                                super.close();
                            }
                            iter.remove();
                            return;
                        }
                    }
                } finally {
                    dataSource.lock.unlock();
                }
            } else {
                super.close();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MockerConnection that = (MockerConnection) o;
            return this.connectionId == that.connectionId;
        }

        @Override
        public int hashCode() {
            return connectionId + "".hashCode();
        }

    }

    /**
     * Construct a connection pool according to the database connection configuration information
     *
     * @param config Database connection information
     * @param connectParam map between parameter name and parameter value
     */
    public MockerDataSource(DataBaseConfig config, Map<String, String> connectParam) throws SQLException {
        validateJdbcConfig(config);
        this.config = config;
        this.connectParams = connectParam;
        this.jdbcUrl = generateJdbcUrl();
        initPool();
    }

    /**
     * Constructor for MockerDataSource
     *
     * @param config Database connection configuration object
     * @param minPoolSize The minimum size of the connection pool
     * @param maxPoolSize The maximum size of the connection pool
     * @param increaseStep Database connection gain
     */
    public MockerDataSource(DataBaseConfig config, int minPoolSize, int maxPoolSize, int increaseStep,
            Map<String, String> connectParam) throws SQLException {
        validateJdbcConfig(config);
        this.config = config;
        if (minPoolSize <= 0 || maxPoolSize <= 0 || increaseStep <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Min pool size, max pool size or increase step can not be equal to or less than zero");
        }
        if (minPoolSize > maxPoolSize) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Min pool size can not be bigger than max pool size");
        }
        this.minPoolSize = minPoolSize;
        this.increaseStep = increaseStep;
        this.maxPoolSize = maxPoolSize;
        this.connectParams = connectParam;
        this.jdbcUrl = generateJdbcUrl();
        initPool();
    }

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
        ((MockerConnection) getConnection(username.toString(), this.config.getPassword())).closeConnection();
        refresh();
    }

    /**
     * Verify the validity of the information of the database connection object
     *
     * @param config Database connection configuration object
     * @throws MockerException If the verification fails, an exception is thrown
     */
    private void validateJdbcConfig(DataBaseConfig config) {
        Validate.notNull(config, "DataBase config can not be null for MockerDataSource#validate");
        Validate.notEmpty(config.getHost(), "Host can not be blank for MockerDataSource#validate");
        Validate.notEmpty(config.getUser(), "User can not be blank for MockerDataSource#validate");
        Validate.notNull(config.getPassword(), "Password can not be null for MockerDataSource#validate");
        Validate.notEmpty(config.getDefaultSchame(),
                "DefaultSchemaName can not be blank for MockerDataSource#validate");
        Validate.notNull(config.getPort(), "Port can not be blank for MockerDataSource#validate");
    }

    private String generateJdbcUrl() {
        StringBuilder buffer = new StringBuilder("jdbc:oceanbase://");
        buffer.append(this.config.getHost())
                .append(":")
                .append(this.config.getPort())
                .append("/")
                .append(this.config.getDefaultSchame());
        if (this.connectParams != null) {
            Set<Entry<String, String>> entrySet = this.connectParams.entrySet();
            String paramStr = entrySet.stream()
                    .map(stringStringEntry -> stringStringEntry.getKey() + "=" + stringStringEntry.getValue())
                    .collect(Collectors.joining("&"));
            buffer.append("?").append(paramStr);
        }
        return buffer.toString();
    }

    static {
        try {
            DriverManager.setLoginTimeout(15);
            Class.forName(DRIVER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            log.error("Data source initialization failed", e);
        }
    }

    /**
     * Refresh the connection pool and call this method when no connection is available. This method
     * will clean up the connections that have been used
     */
    private void refresh() {
        lock.lock();
        try {
            int liveConnectionCount = this.connectionPool.size();
            Iterator<MockerConnection> iter = this.connectionInUse.iterator();
            while (iter.hasNext()) {
                MockerConnection mockerConnection = iter.next();
                if (!mockerConnection.isClosed() && mockerConnection.isValid(getLoginTimeout())) {
                    liveConnectionCount++;
                } else {
                    log.debug("Remove dead connection from using pool, connectionId={}", mockerConnection.connectionId);
                    iter.remove();
                }
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
                        "The number of surviving connections is less than the minimum number of connections, start adding connections, liveConnectionsCount={}, minPoolSize={}, triggeredThreadName={}",
                        liveConnectionCount, liveConnectionCount + interval, Thread.currentThread().getName());
                for (int i = 0; i < interval; i++) {
                    this.connectionPool
                            .push((MockerConnection) getConnection(username.toString(), this.config.getPassword()));
                }
            } else if (liveConnectionCount > maxPoolSize) {
                int count = Math.min(liveConnectionCount - maxPoolSize, connectionPool.size());
                log.info(
                        "The number of surviving connections is greater than the maximum number of connections, start to reduce the number of connections, liveConnectionsCount={}, maxPoolSize={}, triggeredThreadName={}",
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
                    this.connectionPool
                            .push((MockerConnection) getConnection(username.toString(), this.config.getPassword()));
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
                log.warn(
                        "There are no more connections available in the connection pool, start refreshing, triggeredThreadName={}",
                        Thread.currentThread().getName());
                refresh();
                if (this.connectionPool.size() == 0) {
                    log.warn(
                            "No connection is released from the connection pool, the thread starts to wait, threadName={}",
                            Thread.currentThread().getName());
                    noMoreConnection.await(3, TimeUnit.SECONDS);
                    return getConnection();
                }
            }
            MockerConnection connection = this.connectionPool.pop();
            log.debug("Pop connection from pool, connectionId={}", connection.connectionId);
            if (!connection.isValid(getLoginTimeout()) || connection.isClosed()) {
                return getConnection();
            }
            this.connectionInUse.add(connection);
            log.debug(
                    "The thread gets the database connection from the connection pool successfully, connectionId={}, threadName={}, currentPoolSize={}, connectionInUseSize={}",
                    connection.connectionId, Thread.currentThread().getName(), this.connectionPool.size(),
                    this.connectionInUse.size());
            return connection;
        } catch (InterruptedException e) {
            log.error("Thread waiting for connection pool read lock failed, threadName={}",
                    Thread.currentThread().getName(), e);
            throw new SQLException(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Cleanup logic, used to clean up unused connections when the thread pool is destroyed
     */
    public boolean clear() {
        if (lock.tryLock()) {
            try {
                int count = connectionPool.size();
                log.info(
                        "The database connection pool will be closed, the free database connection needs to be closed, currentPoolSize={}",
                        count);
                int clearSize = 0;
                for (int i = 0; i < count; i++) {
                    try {
                        connectionPool.pop().closeConnection();
                        clearSize++;
                    } catch (SQLException e) {
                        log.error("Fail to close the connection", e);
                    }
                }
                log.info(
                        "Free database connection closed completely, clearConnectionCount={}, failedConnectionCount={}",
                        clearSize, count - clearSize);
                clearSize = 0;
                int closeSize = 0;
                int inUseCount = this.connectionInUse.size();
                log.info("The database connection in use will be closed, connectionInUseCount={}", inUseCount);
                for (int i = 0; i < inUseCount; i++) {
                    try {
                        if (!this.connectionInUse.get(i).isClosed()) {
                            this.connectionInUse.get(i).closeConnection();
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
                        clearSize, closeSize, inUseCount - clearSize - closeSize);
            } finally {
                lock.unlock();
            }
            return true;
        }
        return false;
    }

    /**
     * Obtain a database connection independently, the connection is not hosted in the connection pool
     *
     * @param username Database connection user name
     * @param password Data connection password
     * @return Return to database connection
     * @throws SQLException An exception may be thrown when the connection is established
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        int id = connectionCounter.incrementAndGet();
        log.debug("Create connection, connectionId={} ", id);
        return new MockerConnection(this, DriverManager.getConnection(this.jdbcUrl, username, password), id, true);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        DriverManager.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("not support for MockerDataSourceManager#getParentLogger method");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException("not support for MockerDataSourceManager#unwrap method");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException("not support for MockerDataSourceManager#isWrapperFor method");
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException("not support for MockerDataSourceManager#getLogWriter method");
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLFeatureNotSupportedException("not support for MockerDataSourceManager#setLogWriter method");
    }
}
