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
package com.oceanbase.tools.datamocker;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;

import com.oceanbase.tools.datamocker.model.config.DataBaseConfig;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * Test base class, used to introduce some configuration items
 *
 * @author yh263208
 * @date 2020-12-08 15:32
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MockerTestBase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    protected static DataBaseConfig getOracleConfig() {
        DataBaseConfig config = new DataBaseConfig();
        Keys keys = new Keys();
        config.setHost(keys.getValue(Keys.OB_ORACLE_HOST_KEY));
        config.setPort(Integer.valueOf(keys.getValue(Keys.OB_ORACLE_PORT_KEY)));
        config.setUser(keys.getValue(Keys.OB_ORACLE_USER_KEY));
        config.setPassword(keys.getValue(Keys.OB_ORACLE_PASSWD_KEY));
        config.setTenant(keys.getValue(Keys.OB_ORACLE_TENANT_KEY));
        config.setDefaultSchame(keys.getValue(Keys.OB_ORACLE_SCHEMA_KEY));
        config.setCluster(keys.getValue(Keys.OB_ORACLE_CLUSTER_KEY));
        return config;
    }

    protected static DataBaseConfig getMySqlConfig() {
        DataBaseConfig config = new DataBaseConfig();
        Keys keys = new Keys();
        config.setHost(keys.getValue(Keys.OB_MYSQL_HOST_KEY));
        config.setPort(Integer.valueOf(keys.getValue(Keys.OB_MYSQL_PORT_KEY)));
        config.setUser(keys.getValue(Keys.OB_MYSQL_USER_KEY));
        config.setPassword(keys.getValue(Keys.OB_MYSQL_PASSWD_KEY));
        config.setTenant(keys.getValue(Keys.OB_MYSQL_TENANT_KEY));
        config.setDefaultSchame(keys.getValue(Keys.OB_MYSQL_SCHEMA_KEY));
        config.setCluster(keys.getValue(Keys.OB_MYSQL_CLUSTER_KEY));
        return config;
    }

    private static class Keys {

        private static final String OB_ORACLE_HOST_KEY = "OB_ORACLE_HOST";
        private static final String OB_ORACLE_PORT_KEY = "OB_ORACLE_PORT";
        private static final String OB_ORACLE_USER_KEY = "OB_ORACLE_USER";
        private static final String OB_ORACLE_PASSWD_KEY = "OB_ORACLE_PASSWD";
        private static final String OB_ORACLE_TENANT_KEY = "OB_ORACLE_TENANT";
        private static final String OB_ORACLE_SCHEMA_KEY = "OB_ORACLE_SCHEMA";
        private static final String OB_ORACLE_CLUSTER_KEY = "OB_ORACLE_CLUSTER";

        private static final String OB_MYSQL_HOST_KEY = "OB_MYSQL_HOST";
        private static final String OB_MYSQL_PORT_KEY = "OB_MYSQL_PORT";
        private static final String OB_MYSQL_USER_KEY = "OB_MYSQL_USER";
        private static final String OB_MYSQL_PASSWD_KEY = "OB_MYSQL_PASSWD";
        private static final String OB_MYSQL_TENANT_KEY = "OB_MYSQL_TENANT";
        private static final String OB_MYSQL_SCHEMA_KEY = "OB_MYSQL_SCHEMA";
        private static final String OB_MYSQL_CLUSTER_KEY = "OB_MYSQL_CLUSTER";

        private final Properties envProperties;

        public Keys() {
            envProperties = getEnvProperties();
        }

        public String getValue(String key) {
            String secretKey = getSystemProperty(key);
            if (StringUtils.isNotBlank(secretKey)) {
                return secretKey;
            }
            secretKey = getSystemProperty("ACI_VAR_" + key);
            if (StringUtils.isNotBlank(secretKey)) {
                return secretKey;
            }
            throw new RuntimeException("environment variable '" + key + "' is not set");
        }

        private Properties getEnvProperties() {
            Properties properties = new Properties();
            try {
                URL location = MockerTestBase.class.getProtectionDomain().getCodeSource().getLocation();
                File file = Paths.get(location.toURI()).getParent().getParent().resolve(".env").toFile();
                if (file.exists()) {
                    try (FileInputStream inputStream = new FileInputStream(file)) {
                        properties.load(inputStream);
                    }
                }
            } catch (Exception e) {
                // eat exception
            }
            return properties;
        }

        private String getSystemProperty(String key) {
            String property = System.getProperty(key);
            if (StringUtils.isNotBlank(property)) {
                return property;
            }
            property = System.getenv(key);
            if (StringUtils.isNotBlank(property)) {
                return property;
            }
            property = envProperties.getProperty(key);
            if (StringUtils.isNotBlank(property)) {
                return property;
            }
            return null;
        }
    }

}
