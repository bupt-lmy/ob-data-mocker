package com.oceanbase.tools.datamocker.task.concurrent;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.core.read.ColumnReader;
import com.oceanbase.tools.datamocker.core.task.AbstractDataPipe;
import com.oceanbase.tools.datamocker.core.write.DataBaseWriter;
import com.oceanbase.tools.datamocker.core.write.SqlScriptWriter;
import com.oceanbase.tools.datamocker.core.write.output.MockerDataSource;
import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.datatype.oracle.OracleNumberType;
import com.oceanbase.tools.datamocker.generator.digit.NormalGenerator;
import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.enums.ScriptType;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.datamocker.task.primitive.DataBasePrimitiveTest;
import com.oceanbase.tools.datamocker.util.MockDataPipe;
import com.oceanbase.tools.datamocker.util.MockerBuffer;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * mock data buffer object test class
 *
 * @author yh263208
 * @date 2021-01-16 17:28
 * @since OBMOCKER-0.1.0-snapshot
 */
@Slf4j
public class MockBufferTest extends MockerTestBase {
    private final static String mysqlEnv = "db/mysql-env.properties";
    private final static String oracleEnv = "db/oracle-env.properties";
    private final String ddl = "CREATE TABLE \"EMP\" (\n"
            + "  \"COL1\" NUMBER(5,2) NOT NULL,\n"
            + "  \"COL2\" NUMBER(5,2) NOT NULL,\n"
            + "  \"COL3\" NUMBER(5,3) NOT NULL\n"
            + "); ";
    private DataSource dataSource;
    private MockerFile manager;

    private DataBaseConfig getDBConfig(ObModeType dialectType) throws IOException {
        DataBaseConfig config = new DataBaseConfig();
        Properties properties = new Properties();
        URL url = null;
        if (ObModeType.OB_MYSQL.equals(dialectType)) {
            url = DataBasePrimitiveTest.class.getClassLoader().getResource(mysqlEnv);
        } else if (ObModeType.OB_ORACLE.equals(dialectType)) {
            url = DataBasePrimitiveTest.class.getClassLoader().getResource(oracleEnv);
        } else {
            return null;
        }
        assert url != null;
        properties.load(new FileInputStream(url.getPath()));
        config.setDefaultSchame(properties.getProperty("schema"));
        config.setPassword(properties.getProperty("passwd"));
        config.setUser(properties.getProperty("user"));
        config.setCluster(properties.getProperty("cluster"));
        config.setTenant(properties.getProperty("tenant"));
        config.setPort(Integer.valueOf(properties.getProperty("port")));
        config.setHost(properties.getProperty("host"));
        return config;
    }

    private void initEnv(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(this.ddl)) {
                statement.executeUpdate();
            }
        }
    }

    @Before
    public void initFileManager() throws IOException, SQLException {
        DataBaseConfig oracleConfig = getDBConfig(ObModeType.OB_ORACLE);
        dataSource = new MockerDataSource(oracleConfig, 15, 25, 2, null);
        initEnv(dataSource);
        manager = new MockerFile("test/mock/mock.sql", ScriptType.SQL);
    }

    private Map<String, AbstractDataType<?, ? extends Comparable<?>>> getTableSchma() {
        List<String> columns = Arrays.asList("COL1", "COL2", "COL3");
        Map<String, AbstractDataType<?, ? extends Comparable<?>>> map = new HashMap<>();
        for (String column : columns) {
            map.putIfAbsent(column, new OracleNumberType(5, 2, null, false));
        }
        return map;
    }

    private ColumnReader<BigDecimal> getPrimitive(String columnName) {
        OracleNumberType number = new OracleNumberType(5, 2, null, false);
        BigDecimal expectAvg = new BigDecimal("12");
        NormalGenerator generator = new NormalGenerator(expectAvg.doubleValue(), 3);
        number.bind(generator);
        return new ColumnReader<>(number, columnName, null);
    }

    private void startDataGenerateTask(AbstractDataPipe<MockRowData> dataPipe, int batchSize, int maxCount) {
        Map<String, AbstractDataType<?, ? extends Comparable<?>>> map = getTableSchma();
        MockerBuffer buffer = new MockerBuffer(map, (long) batchSize);
        buffer.setConcurrent(2);
        buffer.register(dataPipe);
        List<ColumnReader<BigDecimal>> first = new ArrayList<>();
        List<ColumnReader<BigDecimal>> second = new ArrayList<>();
        List<String> keySet = new ArrayList<>(map.keySet());
        for (int i = 0; i < keySet.size(); i++) {
            if ((i + 1) % 2 == 0) {
                first.add(getPrimitive(keySet.get(i)));
            } else {
                second.add(getPrimitive(keySet.get(i)));
            }
        }
        Thread firstThread = new Thread(() -> {
            try {
                for (int i = 0; i < maxCount; i++) {
                    if (first.size() == 1) {
                        ColumnReader<BigDecimal> primitive = first.get(0);
                        buffer.write(primitive.read(), Long.MAX_VALUE, TimeUnit.SECONDS);
                    } else {
                        MockRowData mockRowData = new MockRowData(first.size());
                        for (ColumnReader<BigDecimal> primitive : first) {
                            MockColumnData<BigDecimal> mockColumn = primitive.read();
                            mockRowData.addMockColumn(mockColumn);
                        }
                        buffer.write(mockRowData, Long.MAX_VALUE, TimeUnit.SECONDS);
                    }
                }
                buffer.close(0, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        firstThread.start();
        Thread secThread = new Thread(() -> {
            try {
                for (int i = 0; i < maxCount; i++) {
                    if (second.size() == 1) {
                        ColumnReader<BigDecimal> primitive = second.get(0);
                        buffer.write(primitive.read(), Long.MAX_VALUE, TimeUnit.SECONDS);
                    } else {
                        MockRowData mockRowData = new MockRowData(first.size());
                        for (ColumnReader<BigDecimal> primitive : second) {
                            MockColumnData<BigDecimal> mockColumn = primitive.read();
                            mockRowData.addMockColumn(mockColumn);
                        }
                        buffer.write(mockRowData, Long.MAX_VALUE, TimeUnit.SECONDS);
                    }
                }
                buffer.close(0, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        secThread.start();
    }

    @Test
    public void testDataBasePrimitive() throws IOException, InterruptedException {
        AbstractDataPipe<MockRowData> dataPipe = new MockDataPipe(1);
        startDataGenerateTask(dataPipe, 256, 600);
        ObModeType dialectType = ObModeType.OB_ORACLE;
        DataBaseConfig config = getDBConfig(dialectType);
        assert config != null;
        DataBaseWriter primitive = new DataBaseWriter(dataSource, dialectType, config.getDefaultSchame(), "EMP");
        primitive.register(dataPipe);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Thread writeThread = new Thread(() -> {
                try {
                    while (true) {
                        if (primitive.write() == null) {
                            break;
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
            writeThread.start();
            threads.add(writeThread);
        }
        for (Thread t : threads) {
            t.join();
        }
    }

    @Test
    public void testScriptPrimitive() throws InterruptedException {
        AbstractDataPipe<MockRowData> dataPipe = new MockDataPipe(1);
        startDataGenerateTask(dataPipe, 256, 123);
        SqlScriptWriter primitive = new SqlScriptWriter(manager, ObModeType.OB_ORACLE, "test", "emp");
        primitive.register(dataPipe);
        List<Thread> list = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            Thread writeThread = new Thread(() -> {
                try {
                    while (true) {
                        if (primitive.write() == null) {
                            break;
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
            writeThread.start();
            list.add(writeThread);
        }
        for (Thread t : list) {
            t.join();
        }
    }

    private void closeEnv(DataSource dataSource) throws SQLException {
        String sql = "drop table emp";
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.executeUpdate();
            }
        }
    }

    @After
    public void clear() throws SQLException {
        try {
            manager.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeEnv(dataSource);
    }
}
