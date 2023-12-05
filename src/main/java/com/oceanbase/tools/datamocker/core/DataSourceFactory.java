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

package com.oceanbase.tools.datamocker.core;

import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.model.config.DataBaseConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * {@link DataSourceFactory}
 *
 * @author yh263208
 * @date 2023-11-23 14:40
 * @since ODC_release_0.1.5_snapshot
 */
@Setter
public class DataSourceFactory {

    private final static String JDBC_DRIVER_CLASS = "com.oceanbase.jdbc.Driver";
    private final DataBaseConfig config;
    private String driverClassName;
    private int maxPoolSize;
    private int loginTimeoutSeconds;
    private Map<String, String> params;

    public DataSourceFactory(@NonNull DataBaseConfig config) {
        validate(config);
        this.config = config;
        this.params = config.getConnectParam();
    }

    public DataSource generate() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(getJdbcUrl());
        dataSource.setUsername(getUsername());
        if (StringUtils.isEmpty(this.driverClassName)) {
            dataSource.setDriverClassName(JDBC_DRIVER_CLASS);
        } else {
            dataSource.setDriverClassName(this.driverClassName);
        }
        if (StringUtils.isEmpty(this.config.getPassword())) {
            dataSource.setPassword("");
        } else {
            dataSource.setPassword(this.config.getPassword());
        }
        init(dataSource);
        return dataSource;
    }

    private void validate(DataBaseConfig config) {
        Validate.notNull(config.getUser(), "User can not be empty");
        Validate.notNull(config.getHost(), "Host can not be empty");
        Validate.notNull(config.getPort(), "Port can not be empty");
        Validate.notEmpty(config.getDefaultSchame(), "DefaultSchema can not be empty");
    }

    private void init(HikariDataSource dataSource) throws SQLException {
        dataSource.setLoginTimeout(15);
        dataSource.setMaximumPoolSize(5);
        if (this.maxPoolSize > 0) {
            dataSource.setMaximumPoolSize(this.maxPoolSize);
        }
        if (this.loginTimeoutSeconds > 0) {
            dataSource.setLoginTimeout(this.loginTimeoutSeconds);
        }
    }

    private String getJdbcUrl() {
        StringBuilder buffer = new StringBuilder("jdbc:oceanbase://");
        buffer.append(this.config.getHost())
                .append(":")
                .append(this.config.getPort())
                .append("/")
                .append(this.config.getDefaultSchame());
        if (this.params != null) {
            Set<Entry<String, String>> entrySet = this.params.entrySet();
            String paramStr = entrySet.stream()
                    .map(stringStringEntry -> stringStringEntry.getKey() + "=" + stringStringEntry.getValue())
                    .collect(Collectors.joining("&"));
            buffer.append("?").append(paramStr);
        }
        return buffer.toString();
    }

    private String getUsername() {
        StringBuilder username = new StringBuilder(this.config.getUser());
        if (StringUtils.isNotBlank(this.config.getTenant())) {
            username.append("@").append(this.config.getTenant());
        }
        if (StringUtils.isNotBlank(this.config.getCluster())) {
            username.append("#").append(this.config.getCluster());
        }
        return username.toString();
    }

}
