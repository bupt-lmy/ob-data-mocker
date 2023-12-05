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

package com.oceanbase.tools.datamocker.constraint;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.core.DataSourceFactory;
import com.oceanbase.tools.datamocker.model.config.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link ConstraintFactoryTest}
 *
 * @author yh263208
 * @date 2023-11-27 20:32
 * @since ODC_release_4.2.3
 */
public class ConstraintFactoryTest extends MockerTestBase {

    private static final String ddlMysql = "CREATE TABLE `emp` (\n"
            + "  `col` decimal(10,0) NOT NULL,\n"
            + "  `col1` decimal(10,0) DEFAULT NULL,\n"
            + "  `col2` decimal(10,0) DEFAULT NULL,\n"
            + "  PRIMARY KEY (`col`),\n"
            + "  UNIQUE KEY `Hello` (`col1`, `col2`) BLOCK_SIZE 16384 GLOBAL\n"
            + ")";
    private static final String ddlWithVirtualColumnMysql = "CREATE TABLE `emp1` (\n"
            + "  `col` decimal(10,0) NOT NULL,\n"
            + "  `col1` decimal(10,0) DEFAULT NULL,\n"
            + "  `col2` decimal(10,0) DEFAULT NULL,\n"
            + "  `col4` decimal(10,0) GENERATED ALWAYS AS ((`col2` + `col1`)) VIRTUAL,\n"
            + "  PRIMARY KEY (`col`),\n"
            + "  UNIQUE KEY `Hello` (`col1`, `col2`) BLOCK_SIZE 16384 GLOBAL,\n"
            + "  UNIQUE KEY `hello2` (`col4`) BLOCK_SIZE 16384 GLOBAL\n"
            + ") ";
    private static final String ddlOracle = "CREATE TABLE \"EMP\" (\n"
            + "  \"COL\" NUMBER(5,2) NOT NULL,\n"
            + "  \"COL2\" NUMBER(5,2) NOT NULL,\n"
            + "  \"COL3\" NUMBER(4,2) NOT NULL,\n"
            + "  CONSTRAINT \"EMP_OBPK_1610357443362979\" PRIMARY KEY (\"COL\"),\n"
            + "  CONSTRAINT \"EMP_OBUNIQUE_1610357443363981\" UNIQUE (\"COL2\", \"COL3\"),\n"
            + "CONSTRAINT \"EMP_OBCHECK_1610453879284344\" CHECK ((\"COL3\" < 12))\n"
            + ") ";
    private static final String ddlWithVirtualColumnOracle = "CREATE TABLE \"EMP1\" (\n"
            + "  \"COL\" NUMBER(5,2) NOT NULL,\n"
            + "  \"COL2\" NUMBER(5,2) NOT NULL,\n"
            + "  \"COL3\" NUMBER(4,2) NOT NULL,\n"
            + "  \"COL4\" NUMBER(5,3) GENERATED ALWAYS AS ((\"COL2\" + \"COL3\")) VIRTUAL,\n"
            + "  CONSTRAINT \"EMP_OBPK\" PRIMARY KEY (\"COL\"),\n"
            + "  CONSTRAINT \"EMP_OBUNIQUE_1231\" UNIQUE (\"COL2\", \"COL3\"),\n"
            + "  CONSTRAINT \"EMP_OBUNIQUE_12343\" UNIQUE (\"COL4\")\n,"
            + "CONSTRAINT \"EMP1_OBFK_1610454320318209\" FOREIGN KEY (\"COL2\") REFERENCES "
            + "\"SYS\".\"EMP\"(\"COL\")\n"
            + ");";
    private DataSource oracleDatasource = null;
    private DataSource mysqlDatasource = null;

    @Before
    public void initEnv() throws SQLException {
        if (oracleDatasource == null) {
            DataBaseConfig config = getDBConfig(ObModeType.OB_ORACLE);
            oracleDatasource = new DataSourceFactory(config).generate();
        }
        if (mysqlDatasource == null) {
            DataBaseConfig config = getDBConfig(ObModeType.OB_MYSQL);
            mysqlDatasource = new DataSourceFactory(config).generate();
        }
        try (Connection connection = oracleDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(ddlOracle);
                statement.execute("insert into emp(col,col2,col3) values(12.1,12.44,11.67);");
                statement.execute(ddlWithVirtualColumnOracle);
                statement.execute("insert into emp1(col,col2,col3) values(12.1,12.1,15.67);");
            }
        }
        try (Connection connection = mysqlDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(ddlMysql);
                statement.execute(ddlWithVirtualColumnMysql);
                statement.execute("insert into emp1(col,col1,col2) values(12.1,12.44,15.67);");
            }
        }
    }

    private DataBaseConfig getDBConfig(ObModeType dialectType) {
        return dialectType == ObModeType.OB_MYSQL ? getMySqlConfig() : getOracleConfig();
    }

    @Test
    public void generate_oracleModeUk_generateSucceed() {
        List<Constraint> constraints = new OBOracleUKConstraintFactory(oracleDatasource, "SYS", "EMP", 1500).generate();
        Assert.assertEquals(1, constraints.size());
    }

    @Test
    public void generate_oracleModePk_generateSucceed() {
        List<Constraint> constraints = new OBOraclePKConstraintFactory(oracleDatasource, "SYS", "EMP", 1500).generate();
        Assert.assertEquals(1, constraints.size());
    }

    @Test
    public void generate_oracleModeCk_generateSucceed() {
        thrown.expectMessage("Check constraint is not support yet");
        thrown.expect(MockerException.class);
        new OBOracleCheckConstraintFactory(oracleDatasource, "SYS", "EMP").generate();
    }

    @Test
    public void generate_oracleModeFk_generateSucceed() {
        thrown.expect(MockerException.class);
        thrown.expectMessage("Foreign constraint is not support yet");
        new OBOracleForeignConstraintFactory(oracleDatasource, "SYS", "EMP1").generate();
    }

    @Test
    public void generate_oracleModeVirtualUk_generateSucceed() {
        thrown.expect(MockerException.class);
        thrown.expectMessage("Virtual column \"EMP1.COL4\" for constraint is not support yet");
        new OBOracleUKConstraintFactory(oracleDatasource, "SYS", "EMP1", 1500).generate();
    }

    @Test
    public void generate_mysqlModeUk_generateSucceed() {
        List<Constraint> constraints = new OBMysqlUKConstraintFactory(mysqlDatasource, "test", "emp", 1500).generate();
        Assert.assertEquals(1, constraints.size());
    }

    @Test
    public void generate_mysqlModePk_generateSucceed() {
        List<Constraint> constraints = new OBMysqlPKConstraintFactory(mysqlDatasource, "test", "emp1", 1500).generate();
        Assert.assertEquals(1, constraints.size());
    }

    @Test
    public void generate_mysqlModeVirtualUk_generateSucceed() throws Throwable {
        thrown.expect(MockerException.class);
        thrown.expectMessage("Virtual column \"emp1.col4\" for constraint is not support yet");
        new OBMysqlUKConstraintFactory(mysqlDatasource, "test", "emp1", 1500).generate();
    }

    @After
    public void clearEnv() throws Exception {
        try (Connection connection = oracleDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("drop table emp1");
                statement.execute("drop table emp");
            }
        }
        try (Connection connection = mysqlDatasource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("drop table emp1");
                statement.execute("drop table emp");
            }
        }
        if (oracleDatasource instanceof AutoCloseable) {
            ((AutoCloseable) oracleDatasource).close();
        }
        if (mysqlDatasource instanceof AutoCloseable) {
            ((AutoCloseable) mysqlDatasource).close();
        }
    }

}
