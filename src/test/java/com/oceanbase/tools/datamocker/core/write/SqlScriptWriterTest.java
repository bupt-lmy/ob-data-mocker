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

package com.oceanbase.tools.datamocker.core.write;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for {@link SqlScriptWriter}
 *
 * @author yh263208
 * @date 2023-11-28 20:56
 * @since ODC_release_4.2.3
 */
public class SqlScriptWriterTest {

    private final static String DEST_DIR = "test/mock";
    private final static List<String> columnList = Arrays.asList("COL1", "COL2", "COL3");

    @Before
    public void setUp() throws IOException {
        FileUtils.forceMkdir(new File(DEST_DIR));
    }

    @After
    public void clear() throws IOException {
        FileUtils.deleteDirectory(new File(DEST_DIR));
    }

    @Test
    public void write_oracleMode_writeSucceed() throws Throwable {
        DataWriter dataWriter = new SqlScriptWriter(getOutput(), 0L, OracleSqlBuilder::new, "test", "emp");
        long actual = dataWriter.write(getRows());
        FileInputStream inputStream = new FileInputStream(new File(DEST_DIR).listFiles()[0]);
        Assert.assertEquals(inputStream.available(), actual);
    }

    @Test
    public void write_mysqlMode_writeSucceed() throws Throwable {
        DataWriter dataWriter = new SqlScriptWriter(getOutput(), 0L, MySQLSqlBuilder::new, "test", "emp");
        long actual = dataWriter.write(getRows());
        FileInputStream inputStream = new FileInputStream(new File(DEST_DIR).listFiles()[0]);
        Assert.assertEquals(inputStream.available(), actual);
    }

    private SqlScriptOutput getOutput() throws IOException {
        return new SqlScriptOutput(new File(DEST_DIR), "emp", Long.MAX_VALUE);
    }

    private List<MockRowData> getRows() {
        List<MockRowData> list = new LinkedList<>();
        for (int i = 0; i < 48; i++) {
            MockRowData row = new MockRowData(48);
            for (String column : columnList) {
                row.addMockColumn(new MockColumnData<>(column, new OracleNumberType(8, 5, null, false),
                        new BigDecimal(new Random().nextInt(1000))));
            }
            list.add(row);
        }
        return list;
    }

}
