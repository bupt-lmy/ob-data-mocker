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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for {@link UniqueConstraint}
 *
 * @author yh263208
 * @date 2023-11-27 21:14
 * @since ODC_release_4.2.3
 */
public class UniqueConstraintTest extends MockerTestBase {

    private final List<String> columnNames = Arrays.asList("COL1", "COL2", "COL3");
    private final String tableName = "EMP";
    private static final String constaintName = "EMP_CONSTRAINT";

    @Test
    public void check_noDuplicatedData_assertFalse() {
        UniqueConstraint unique = new UniqueConstraint(constaintName, tableName, getColumns(), 15000);
        List<MockRowData> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            MockRowData row = getData();
            unique.mark(row);
            list.add(row);
        }
        for (int i = 0; i < 1000; i++) {
            MockRowData row = list.get(i);
            Assert.assertFalse(unique.check(row));
        }
        Assert.assertFalse(unique.check(null));
    }

    @Test
    public void check_notPositiveCount_expThrown() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Count for UniqueConstraint can not be negative");
        new UniqueConstraint(constaintName, tableName, getColumns(), 0);
    }

    @Test
    public void check_minusCount_expThrown() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Count for UniqueConstraint can not be negative");
        new UniqueConstraint(constaintName, tableName, getColumns(), -100);
    }

    @Test
    public void check_conflictRate_minusDot12() {
        int size = 15000;
        UniqueConstraint unique = new UniqueConstraint(constaintName, tableName, getColumns(), size);
        Set<String> set = new HashSet<>();
        int counter = 0;
        for (int i = 0; i < size; i++) {
            MockRowData row = getData();
            List<String> buffer = new ArrayList<>();
            Set<String> keys = row.columnNames();
            for (String key : keys) {
                MockColumnData<?> value = row.getMockColumn(key);
                buffer.add(value.getColumnValueString());
            }
            String tmp = String.join(",", buffer);
            if (set.contains(tmp) == unique.check(row)) {
                counter++;
            }
            set.add(tmp);
        }
        Assert.assertTrue(counter / (size * 1.0) < 0.12);
    }

    @Test
    public void check_illegalColumn_expThrown() {
        List<MockRowData> list = new ArrayList<>();
        UniqueConstraint unique = new UniqueConstraint(constaintName, tableName, getColumns(), 15000);
        for (int i = 0; i < 1000; i++) {
            MockRowData row = getData();
            list.add(row);
            unique.mark(row);
        }
        MockRowData row = list.get(0);
        row.remove("COL3");
        thrown.expectMessage("Input constraint's columns must contain init constraint's columns");
        thrown.expect(MockerException.class);
        unique.check(row);
    }

    private Map<String, Map<String, Integer>> getColumns() {
        Map<String, Map<String, Integer>> consColumns = new HashMap<>();
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            map.put(columnNames.get(i), i + 2);
        }
        consColumns.put(tableName, map);
        return consColumns;
    }

    private MockRowData getData() {
        MockRowData mockRowData = new MockRowData();
        for (String column : columnNames) {
            BigDecimal value = BigDecimal.valueOf(new Random().nextDouble());
            mockRowData.addMockColumn(new MockColumnData<>(column, new OracleNumberType(10, 5, null, false), value));
        }
        mockRowData.addMockColumn(new MockColumnData<>("column", new OracleNumberType(10, 5, null, false),
                BigDecimal.valueOf(new Random().nextDouble())));
        return mockRowData;
    }

}
