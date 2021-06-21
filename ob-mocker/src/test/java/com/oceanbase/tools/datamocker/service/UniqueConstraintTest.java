package com.oceanbase.tools.datamocker.service;

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
import com.oceanbase.tools.datamocker.constraint.impl.UniqueConstraint;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.Pair;
import org.junit.Assert;
import org.junit.Test;

/**
 * Uniquely constrained test class
 *
 * @author yh263208
 * @date 2021-01-11 15:57
 * @since OBMOCKER_0.1.0_snapshot
 */
public class UniqueConstraintTest extends MockerTestBase {
    private final List<String> columnNames = Arrays.asList("COL1", "COL2", "COL3");
    private final String tableName = "EMP";
    private final String database = "SYS";
    private final String constaintName = "EMP_CONSTRAINT";

    private Map<String, Map<String, Integer>> getColumns() {
        Map<String, Map<String, Integer>> consColumns = new HashMap<>();
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            map.put(columnNames.get(i), i + 2);
        }
        consColumns.put(tableName, map);
        return consColumns;
    }

    private Map<String, Pair<AbstractDataType, Object>> getData() {
        Map<String, Pair<AbstractDataType, Object>> row = new HashMap<>();
        for (String column : columnNames) {
            BigDecimal value = BigDecimal.valueOf(new Random().nextDouble());
            row.put(column, new Pair<>(new OracleNumberType(10, 5, null, false), value));
        }
        row.put("column",
                new Pair<>(new OracleNumberType(10, 5, null, false), BigDecimal.valueOf(new Random().nextDouble())));
        return row;
    }

    @Test
    public void testUnqiueConstraint() {
        List<Map<String, Pair<AbstractDataType, Object>>> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Map<String, Pair<AbstractDataType, Object>> row = getData();
            list.add(row);
        }
        UniqueConstraint unique = new UniqueConstraint(constaintName, database, tableName, getColumns(), list, 15000);
        for (int i = 0; i < 1000; i++) {
            Map<String, Pair<AbstractDataType, Object>> row = list.get(i);
            Assert.assertFalse(unique.check(row));
        }
        Assert.assertFalse(unique.check(null));
    }

    @Test
    public void testUnqiueConstraintWithIllegalCount() {
        List<Map<String, Pair<AbstractDataType, Object>>> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Map<String, Pair<AbstractDataType, Object>> row = getData();
            list.add(row);
        }
        thrown.expect(MockerException.class);
        thrown.expectMessage("Count for unique constraint can not be equal to or smaller than zero");
        UniqueConstraint unique = new UniqueConstraint(constaintName, database, tableName, getColumns(), list, 0);
    }

    @Test
    public void testUniqueConstraintWithIllegalCount() {
        thrown.expect(MockerException.class);
        thrown.expectMessage("Count for unique constraint can not be equal to or smaller than zero");
        UniqueConstraint unique = new UniqueConstraint(constaintName, database, tableName, getColumns(), -100);
    }

    @Test
    public void testUniqueConstraintWithNullInput() {
        int size = 15000;
        UniqueConstraint unique = new UniqueConstraint(constaintName, database, tableName, getColumns(), size);
        Set<String> set = new HashSet<>();
        int counter = 0;
        for (int i = 0; i < size; i++) {
            Map<String, Pair<AbstractDataType, Object>> row = getData();
            List<String> buffer = new ArrayList<>();
            Set<String> keys = row.keySet();
            for (String key : keys) {
                Pair<AbstractDataType, ?> value = row.get(key);
                buffer.add(value.getKey().toString(value.getValue()));
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
    public void testUniqueConstraintWithIllegalColumn() {
        List<Map<String, Pair<AbstractDataType, Object>>> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Map<String, Pair<AbstractDataType, Object>> row = getData();
            list.add(row);
        }
        UniqueConstraint unique = new UniqueConstraint(constaintName, database, tableName, getColumns(), list, 15000);
        Map<String, Pair<AbstractDataType, Object>> row = list.get(0);
        row.remove("COL3");
        thrown.expectMessage("Input constraint's columns must contain init constraint's columns");
        thrown.expect(MockerException.class);
        Assert.assertNull(unique.check(row));
    }
}
