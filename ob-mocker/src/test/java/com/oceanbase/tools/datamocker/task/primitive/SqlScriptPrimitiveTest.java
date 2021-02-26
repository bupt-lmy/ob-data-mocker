package com.oceanbase.tools.datamocker.task.primitive;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.core.task.AbstractDataPipe;
import com.oceanbase.tools.datamocker.core.write.SqlScriptWriter;
import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.model.enums.DialectType;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.MockDataPipe;
import com.oceanbase.tools.datamocker.util.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * sql脚本生成原语测试类
 *
 * @author yh263208
 * @date 2021-01-05 21:10
 * @since OBMOCKER_0.1.0_snapshot
 */
public class SqlScriptPrimitiveTest extends MockerTestBase {
    @Rule
    public ExpectedException expect = ExpectedException.none();
    /**
     * 列信息
     */
    private final static List<String> columnList = Arrays.asList("COL1", "COL2", "COL3");
    /**
     * mock数据文件管理器
     */
    private MockerFile manager = null;

    @Before
    public void initFileManager() throws IOException {
        manager = new MockerFile("test/mock/mock.sql");
    }

    /**
     * 获取一个测试用的随机数据块
     *
     * @param size 数据行数
     * @return 返回数据
     */
    private List<Map<String, Pair<AbstractDataType, Object>>> getRows(int size) {
        List<Map<String, Pair<AbstractDataType, Object>>> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Map<String, Pair<AbstractDataType, Object>> row = new HashMap<>();
            for (String column : columnList) {
                row.put(column,
                        new Pair<>(new OracleNumberType(8, 5, null, false), new BigDecimal(String.valueOf(new Random().nextInt(1000)))));
            }
            list.add(row);
        }
        return list;
    }

    @Test
    public void testSqlPrimitive() throws Exception {
        List<Map<String, Pair<AbstractDataType, Object>>> list = getRows(48);
        SqlScriptWriter primitive = new SqlScriptWriter(manager, DialectType.OB_ORACLE, "test", "emp");
        AbstractDataPipe pipe = new MockDataPipe();
        primitive.register(pipe);
        pipe.write(list);
        primitive.write();
    }

    @Test
    public void testPrimitiveWithoutDataSource() {
        expect.expectMessage("file manager can not be null");
        expect.expect(MockerException.class);
        SqlScriptWriter primitive = new SqlScriptWriter(null, null, null, null);
    }

    @Test
    public void testPrimitiveWithoutDatabase() {
        expect.expectMessage("database can not be null");
        expect.expect(MockerException.class);
        SqlScriptWriter primitive = new SqlScriptWriter(manager, DialectType.OB_ORACLE, null, null);
    }

    @Test
    public void testPrimitiveWithouttable() throws IOException {
        expect.expectMessage("table name can not be null");
        expect.expect(MockerException.class);
        DialectType dialectType = DialectType.OB_ORACLE;
        SqlScriptWriter primitive = new SqlScriptWriter(manager, dialectType, "test", null);
    }

    @After
    public void clearFileManager() throws IOException {
        manager.clear();
    }
}
