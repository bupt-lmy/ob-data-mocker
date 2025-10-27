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
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.constraint.Constraint;
import com.oceanbase.tools.datamocker.constraint.ConstraintBuilder;
import com.oceanbase.tools.datamocker.core.DataSourceFactory;
import com.oceanbase.tools.datamocker.core.Dispatcher;
import com.oceanbase.tools.datamocker.core.read.ColumnReader;
import com.oceanbase.tools.datamocker.core.task.TableTaskInfo;
import com.oceanbase.tools.datamocker.core.task.TableTaskMetaData;
import com.oceanbase.tools.datamocker.core.write.DataWriter;
import com.oceanbase.tools.datamocker.core.write.JdbcWriter;
import com.oceanbase.tools.datamocker.core.write.SqlScriptOutput;
import com.oceanbase.tools.datamocker.core.write.SqlScriptWriter;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.generator.DeepseekSqlGeneratorAdapter;
import com.oceanbase.tools.datamocker.model.config.LlmConfig;
import com.oceanbase.tools.datamocker.model.config.MockColumnConfig;
import com.oceanbase.tools.datamocker.model.config.MockTableConfig;
import com.oceanbase.tools.datamocker.model.config.MockTaskConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.schedule.AbstractScheduler;
import com.oceanbase.tools.datamocker.schedule.DefaultScheduler;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.MDC;

/**
 * The implementation class of the simple dispatcher factory, used to generate a simple table
 * generation task dispatcher. This is a simple dispatcher factory class for the first phase of mock
 * data. It can only be used to process table generation tasks that do not contain foreign key
 * constraints, do not contain check constraints, and unique constraints do not contain virtual
 * columns.
 *
 * @author yh263208
 * @date 2021-01-11 21:41
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class ObMockerFactory {

    private final MockTaskConfig taskConfig;

    public ObMockerFactory(@NonNull MockTaskConfig taskConfig) {
        this.taskConfig = taskConfig;
    }

    public ObDataMocker create() {
        return create(new DefaultScheduler());
    }

    public ObDataMocker create(@NonNull AbstractScheduler scheduler) {
        // ======= 新增：LLM 选路（在工厂内判断）=======
        LlmConfig llm = (taskConfig == null) ? null : taskConfig.getLlmConfig();
        if (llm != null && Boolean.TRUE.equals(llm.getEnabled())) {
            return createLlmBasedMocker(scheduler, llm);
        }
        // ======= 原有分支（老路径）=======
        DataSource dataSource = null;
        try {
            String logDir = this.taskConfig.getLogDir();
            MDC.put("mocktask.workspace", logDir);
            DataSourceFactory factory = new DataSourceFactory(taskConfig.getDbConfig());
            factory.setDriverClassName(taskConfig.getDriverClassName());
            factory.setProtocolName(taskConfig.getProtocolName());
            dataSource = factory.generate();
            Dispatcher<TableTaskInfo> dispatcher = generate(this.taskConfig, dataSource, logDir);
            return new ObDataMocker(dispatcher, scheduler);
        } catch (IOException | SQLException throwables) {
            throw new IllegalArgumentException(throwables);
        } finally {
            if (dataSource instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) dataSource).close();
                } catch (Exception e) { /* ignore */ }
            }
        }
    }
    /**
     * LLM 分支：先生成 SQL 文件，再返回一个“空 Dispatcher”的 ObDataMocker。
     * 调用 start() 后会立即完成，得到一个“完成型” MockContext。
     */
    private ObDataMocker createLlmBasedMocker(@NonNull AbstractScheduler scheduler, @NonNull LlmConfig llm) {
        String logDir = this.taskConfig.getLogDir();
        MDC.put("mocktask.workspace", logDir);
        try {
            // 1) 先做 LLM 生成（直接写盘）
            String taskConfigPath = req(llm.getTaskConfigPath(), "llmMode.taskConfigPath");
            String apiKey = System.getenv("MODEL_API_KEY");//从环境变量中读取模型APIKEY
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException("API Key not found. Please set the DEEPSEEK_API_KEY environment variable.");
            }
            String endpoint = (llm.getEndpoint() == null || llm.getEndpoint().isEmpty())
                    ? "https://api.deepseek.com/chat/completions"
                    : llm.getEndpoint();
            int timeout = llm.getTimeoutSeconds() == null ? 300 : llm.getTimeoutSeconds();

            File generated = DeepseekSqlGeneratorAdapter.generateFromTaskConfig(
                    taskConfigPath, apiKey, endpoint, timeout
            );
            log.info("LLM SQL generated at {}", generated.getAbsolutePath());

            // 2) 构造空 Dispatcher（宽度=0），交给调度器生成一个“完成型”上下文
            Dispatcher<TableTaskInfo> emptyDispatcher = new Dispatcher<>(logDir);

            // 3) 返回 ObDataMocker（start() 立即完成，得到标准 MockContext）
            return new ObDataMocker(emptyDispatcher, scheduler);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to generate SQL via LLM: " + e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }

    private static String req(String v, String name) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required field: " + name);
        }
        return v;
    }

    private Dispatcher<TableTaskInfo> generate(MockTaskConfig taskConfig, DataSource ds, String logDir)
            throws SQLException, IOException {
        List<MockTableConfig> tableConfigs = taskConfig.getTables();
        Validate.notEmpty(tableConfigs, "TaskConfig can not be empty");
        ObModeType obModeType = taskConfig.getDialectType();
        Dispatcher<TableTaskInfo> dispatcher = new Dispatcher<>(tableConfigs.size(), logDir);
        for (int i = 0; i < tableConfigs.size(); i++) {
            MockTableConfig tableConfig = tableConfigs.get(i);
            List<Constraint> constraints = getConstraints(tableConfig, ds, obModeType);
            SqlScriptOutput output = getOutput(tableConfig);
            dispatcher.setObj(i, new TableTaskInfo(getDataWriters(output, tableConfig),
                    getColumnReader(tableConfig, constraints),
                    getDataSourceFactory(tableConfig), constraints,
                    new TableTaskMetaData(getTableSchema(tableConfig), tableConfig, logDir),
                    output, getSqlBuilder(obModeType)));
        }
        return dispatcher;
    }

    /**
     * Get the table structure
     *
     * @param tableConfig table configuration
     * @return schema for a certain table
     */
    private Map<String, AbstractDataType<?, ? extends Comparable<?>>> getTableSchema(MockTableConfig tableConfig) {
        Map<String, AbstractDataType<?, ? extends Comparable<?>>> columnName2DataType = new HashMap<>();
        for (MockColumnConfig columnConfig : tableConfig.getColumns()) {
            columnName2DataType.putIfAbsent(columnConfig.getColumnName(), columnConfig.getDataType());
        }
        return columnName2DataType;
    }

    private List<Constraint> getConstraints(MockTableConfig tableConfig, DataSource dataSource,
            ObModeType dialectType) {
        return ConstraintBuilder.builder()
                .dataSource(dataSource)
                .obModeType(dialectType)
                .tableName(tableConfig.getTableName())
                .schema(tableConfig.getSchemaName())
                .totalMockCount(tableConfig.getTotalCount().intValue())
                .columnName2DataType(getTableSchema(tableConfig))
                .build().getConstraints();
    }

    private DataSourceFactory getDataSourceFactory(MockTableConfig tableConfig) {
        Map<String, String> realParam = new HashMap<>();
        realParam.put("autoReconnect", "true");
        realParam.put("rewriteBatchedStatements", "true");
        realParam.put("emulateUnsupportedPstmts", "false");
        // realParam.put("useServerPrepStmts", "true");
        Map<String, String> param = this.taskConfig.getDbConfig().getConnectParam();
        if (param != null) {
            Set<Entry<String, String>> entries = param.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                realParam.put(entry.getKey(), entry.getValue());
            }
        }
        DataSourceFactory factory = new DataSourceFactory(this.taskConfig.getDbConfig());
        factory.setDriverClassName(taskConfig.getDriverClassName());
        factory.setProtocolName(taskConfig.getProtocolName());
        factory.setConnectionInitSql(taskConfig.getConnectionInitSql());
        factory.setParams(realParam);
        factory.setTimeoutMillis(tableConfig.getTimeoutMillis());
        factory.setMaxPoolSize(taskConfig.getMaxConnectionSize());
        return factory;
    }

    private SqlScriptOutput getOutput(MockTableConfig tableConfig) throws IOException {
        File file = new File(tableConfig.getOutputDir());
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("Failed to delete a dir " + file.getAbsolutePath());
            }
            FileUtils.forceMkdir(file);
        } else {
            FileUtils.forceMkdir(file);
        }
        if (!file.isDirectory()) {
            throw new IllegalArgumentException("Location is not a dir, " + file.getAbsolutePath());
        }
        return new SqlScriptOutput(file, tableConfig.getTableName(), tableConfig.getMaxSingleFileSizeInBytes());
    }

    private List<DataWriter> getDataWriters(SqlScriptOutput output,
            MockTableConfig tableConfig) throws SQLException {
        List<DataWriter> dataWriters = new LinkedList<>();
        dataWriters.add(new SqlScriptWriter(output, tableConfig.getMaxFileOutputSizeInBytes(),
                getSqlBuilder(this.taskConfig.getDialectType()),
                tableConfig.getSchemaName(), tableConfig.getTableName()));
        dataWriters.add(new JdbcWriter(getDataSourceFactory(tableConfig),
                getSqlBuilder(this.taskConfig.getDialectType()),
                tableConfig.getConcurrent(),
                tableConfig.getSchemaName(), tableConfig.getTableName()));
        return dataWriters;
    }

    /**
     * Get all column data reader of a table generation task
     *
     * @param tableConfig table task config
     * @param constraints constraint list
     * @return list of column reader
     */
    private List<ColumnReader<?>> getColumnReader(MockTableConfig tableConfig,
            List<Constraint> constraints) {
        List<MockColumnConfig> columnConfigs = tableConfig.getColumns();
        List<Set<String>> colGroupList = new ArrayList<>();
        for (Constraint constraint : constraints) {
            Map<String, Integer> cols = constraint.columns().get(tableConfig.getTableName());
            Set<String> keySet = cols.keySet();
            colGroupList.add(new HashSet<>(keySet));
        }
        int length = colGroupList.size();
        for (int i = 0; i < length; i++) {
            for (int j = i + 1; j < length; j++) {
                Set<String> tmpSet = new HashSet<>(colGroupList.get(i));
                tmpSet.retainAll(colGroupList.get(j));
                if (tmpSet.size() != 0) {
                    colGroupList.get(i).addAll(colGroupList.get(j));
                    colGroupList.remove(j);
                    length--;
                    j--;
                }
            }
        }
        Map<String, Set<String>> groupMap = new HashMap<>();
        for (Set<String> colSet : colGroupList) {
            groupMap.put(UUID.randomUUID().toString(), colSet);
        }
        List<ColumnReader<?>> returnValue = new LinkedList<>();
        for (MockColumnConfig columnConfig : columnConfigs) {
            AbstractDataType<?, ? extends Comparable<?>> dataType = columnConfig.getDataType();
            String columnName = columnConfig.getColumnName();
            Set<Map.Entry<String, Set<String>>> entrySet = groupMap.entrySet();
            ColumnReader<?> reader = null;
            for (Map.Entry<String, Set<String>> item : entrySet) {
                String groupId = item.getKey();
                if (item.getValue().contains(columnName)) {
                    reader = new ColumnReader<>(dataType, columnName, groupId);
                    break;
                }
            }
            if (reader == null) {
                reader = new ColumnReader<>(dataType, columnName, UUID.randomUUID().toString());
            }
            returnValue.add(reader);
        }
        return returnValue;
    }

    private Supplier<SqlBuilder> getSqlBuilder(ObModeType obModeType) {
        switch (obModeType) {
            case OB_ORACLE:
                return OracleSqlBuilder::new;
            case OB_MYSQL:
                return MySQLSqlBuilder::new;
            default:
                throw new UnsupportedOperationException("Unknown dialect type, " + obModeType);
        }
    }

}
