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
package com.oceanbase.tools.datamocker.task.primitive;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.core.task.AbstractDataPipe;
import com.oceanbase.tools.datamocker.core.write.SqlScriptWriter;
import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.enums.ScriptType;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.datamocker.util.MockDataPipe;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * sql script generates primitive test class
 *
 * @author yh263208
 * @date 2021-01-05 21:10
 * @since OBMOCKER_0.1.0_snapshot
 */
public class SqlScriptPrimitiveTest extends MockerTestBase {
    @Rule
    public ExpectedException expect = ExpectedException.none();
    private final static List<String> columnList = Arrays.asList("COL1", "COL2", "COL3");
    private MockerFile manager = null;

    @Before
    public void initFileManager() throws IOException {
        manager = new MockerFile("test/mock/mock.sql", ScriptType.SQL);
    }

    private List<MockRowData> getRows(int size) {
        List<MockRowData> list = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            MockRowData row = new MockRowData(size);
            for (String column : columnList) {
                row.addMockColumn(new MockColumnData<>(column, new OracleNumberType(8, 5, null, false),
                        new BigDecimal(new Random().nextInt(1000))));
            }
            list.add(row);
        }
        return list;
    }

    @Test
    public void testSqlPrimitive() throws Throwable {
        List<MockRowData> list = getRows(48);
        SqlScriptWriter primitive = new SqlScriptWriter(manager, ObModeType.OB_ORACLE, "test", "emp");
        AbstractDataPipe<List<MockRowData>> pipe = new MockDataPipe(1);
        primitive.register(pipe);
        pipe.write(list);
        primitive.write();
    }

    @Test
    public void testPrimitiveWithoutDataSource() {
        expect.expectMessage("File manager can not be null for SqlScriptWriter#validateParam");
        expect.expect(IllegalArgumentException.class);
        SqlScriptWriter primitive = new SqlScriptWriter(null, null, null, null);
    }

    @Test
    public void testPrimitiveWithoutDatabase() {
        expect.expectMessage("DataBase can not be null for SqlScriptWriter#validateParam");
        expect.expect(IllegalArgumentException.class);
        SqlScriptWriter primitive = new SqlScriptWriter(manager, ObModeType.OB_ORACLE, null, null);
    }

    @Test
    public void testPrimitiveWithouttable() {
        expect.expectMessage("TableName can not be null for SqlScriptWriter#validateParam");
        expect.expect(IllegalArgumentException.class);
        ObModeType dialectType = ObModeType.OB_ORACLE;
        SqlScriptWriter primitive = new SqlScriptWriter(manager, dialectType, "test", null);
    }

    @After
    public void clearFileManager() throws IOException {
        manager.clear();
    }
}
