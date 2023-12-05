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

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.constraint.Constraint;
import com.oceanbase.tools.datamocker.constraint.ConstraintBuilder;
import com.oceanbase.tools.datamocker.core.Dispatcher;
import com.oceanbase.tools.datamocker.core.read.ColumnReader;
import com.oceanbase.tools.datamocker.core.task.AbstractDataPipe;
import com.oceanbase.tools.datamocker.core.DataSourceFactory;
import com.oceanbase.tools.datamocker.core.task.TableTaskInfo;
import com.oceanbase.tools.datamocker.core.task.TableTaskMetaData;
import com.oceanbase.tools.datamocker.core.write.AbstractMockWriter;
import com.oceanbase.tools.datamocker.core.write.JdbcWriter;
import com.oceanbase.tools.datamocker.core.write.SqlScriptWriter;
import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.config.MockColumnConfig;
import com.oceanbase.tools.datamocker.model.config.MockTableConfig;
import com.oceanbase.tools.datamocker.model.config.MockTaskConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.enums.ScriptType;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.datamocker.schedule.AbstractScheduler;
import com.oceanbase.tools.datamocker.schedule.impl.DefaultScheduler;
import com.oceanbase.tools.datamocker.util.MockDataPipe;
import com.oceanbase.tools.datamocker.util.MockerBuffer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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
    private final Map<String, DataSource> taskId2DataSource;
    private final Map<String, List<MockerFile>> taskId2MockerFiles;

    public ObMockerFactory(@NonNull MockTaskConfig taskConfig) {
        this.taskConfig = taskConfig;
        this.taskId2DataSource = new HashMap<>();
        this.taskId2MockerFiles = new HashMap<>();
    }

    public ObDataMocker create() {
        return create(new DefaultScheduler(this.taskConfig.getMaxConnectionSize()));
    }

    public ObDataMocker create(@NonNull AbstractScheduler scheduler) {
        DataSource dataSource = null;
        try {
            String taskId = UUID.randomUUID().toString().toUpperCase();
            MDC.put("mocktask.workspace", taskId);
            DataSourceFactory factory = new DataSourceFactory(taskConfig.getDbConfig());
            factory.setDriverClassName(taskConfig.getDriverClassName());
            dataSource = factory.generate();
            Dispatcher<TableTaskInfo> dispatcher = generate(this.taskConfig, dataSource, taskId);
            return new ObDataMocker(dispatcher, scheduler);
        } catch (IOException | SQLException throwables) {
            throw new IllegalArgumentException(throwables);
        } finally {
            if (dataSource instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) dataSource).close();
                } catch (Exception e) {
                    // eat exception
                }
            }
        }
    }

    private Dispatcher<TableTaskInfo> generate(MockTaskConfig taskConfig, DataSource ds, String taskId)
            throws SQLException, IOException {
        String taskName = taskConfig.getTaskName() == null ? getTaskName() : taskConfig.getTaskName();
        List<MockTableConfig> tableConfigs = taskConfig.getTables();
        Validate.notEmpty(tableConfigs, "TaskConfig can not be empty for ObMockerFactory");
        ObModeType obModeType = taskConfig.getDialectType();
        Dispatcher<TableTaskInfo> dispatcher = new Dispatcher<>(tableConfigs.size(), taskName, taskId);
        for (int i = 0; i < tableConfigs.size(); i++) {
            MockTableConfig tableConfig = tableConfigs.get(i);
            List<Constraint> constraints = getConstraints(tableConfig, ds, taskConfig.getDialectType());
            List<ColumnReader<?>> columnReaders = getColumnReader(tableConfig, constraints);
            Map<String, AbstractDataType<?, ? extends Comparable<?>>> tableSchema = getTableSchema(tableConfig);
            MockerBuffer buffer = new MockerBuffer(tableSchema, tableConfig.getMaxBatchSize());
            TableTaskMetaData metaData = new TableTaskMetaData(tableSchema, tableConfig, obModeType, taskId, 0, i);
            DataSource dataSource = getDataSource(metaData.getTableTaskId());
            List<MockerFile> managers = getFileManager(metaData.getTableTaskId(), tableConfig);
            List<AbstractMockWriter> dataWriter = getDataWriter(tableConfig, buffer, managers, dataSource);
            TableTaskInfo bean =
                    new TableTaskInfo(columnReaders, dataWriter, constraints, buffer, dataSource, managers, metaData);
            dispatcher.setObj(i, bean);
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

    private DataSource getDataSource(String tableTaskId) throws SQLException {
        if (tableTaskId == null) {
            return null;
        }
        DataSource dataSource = this.taskId2DataSource.get(tableTaskId);
        if (dataSource != null) {
            return dataSource;
        }
        Map<String, String> realParam = new HashMap<>();
        realParam.put("autoReconnect", "true");
        realParam.put("rewriteBatchedStatements", "true");
        realParam.put("emulateUnsupportedPstmts", "false");
        // realParam.put("useServerPrepStmts", "true");
        Map<String, String> param = this.taskConfig.getDbConfig().getConnectParam();
        if (param != null) {
            Set<Entry<String, String>> entries = param.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                realParam.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        DataSourceFactory factory = new DataSourceFactory(this.taskConfig.getDbConfig());
        factory.setDriverClassName(taskConfig.getDriverClassName());
        factory.setParams(realParam);
        factory.setMaxPoolSize(taskConfig.getMaxConnectionSize());
        dataSource = factory.generate();
        this.taskId2DataSource.putIfAbsent(tableTaskId, dataSource);
        return dataSource;
    }

    /**
     * Get file manager collection
     *
     * @param tableTaskId table task id
     * @param tableConfig config for table task
     * @return list of file manager
     * @exception IOException Throw an exception when the file operation fails
     */
    private List<MockerFile> getFileManager(String tableTaskId, MockTableConfig tableConfig) throws IOException {
        List<MockerFile> returnVal = taskId2MockerFiles.get(tableTaskId);
        if (returnVal == null) {
            returnVal = new LinkedList<>();
            taskId2MockerFiles.put(tableTaskId, returnVal);
            for (ScriptType scriptType : tableConfig.getScriptType()) {
                String location = tableConfig.dataWriteLocation(scriptType);
                MockerFile fileManager = new MockerFile(location, scriptType, true);
                returnVal.add(fileManager);
            }
        }
        return returnVal;
    }

    /**
     * Get data writer
     *
     * @param tableConfig table task config
     * @param buffer buffer which is bound to writer
     * @param managers list of file managers
     * @param dataSource datasource
     * @return list of mock writer
     */
    private List<AbstractMockWriter> getDataWriter(MockTableConfig tableConfig, MockerBuffer buffer,
            List<MockerFile> managers, DataSource dataSource) {
        Validate.notNull(managers, "Mocker file manager list can not be null");
        List<AbstractMockWriter> dataWriters = new LinkedList<>();
        for (MockerFile manager : managers) {
            SqlScriptWriter writer = new SqlScriptWriter(manager, this.taskConfig.getDialectType(),
                    tableConfig.getSchemaName(), tableConfig.getTableName());
            dataWriters.add(writer);
        }
        if (dataSource == null) {
            return dataWriters;
        }
        JdbcWriter writer = new JdbcWriter(dataSource, this.taskConfig.getDialectType(),
                tableConfig.getSchemaName(), tableConfig.getTableName());
        dataWriters.add(writer);
        Map<String, AbstractDataPipe<List<MockRowData>>> groupId2DataPipe = new HashMap<>();
        for (AbstractMockWriter item : dataWriters) {
            AbstractDataPipe<List<MockRowData>> dataPipe = groupId2DataPipe.computeIfAbsent(item.groupId(),
                    s -> new MockDataPipe(tableConfig.getMaxRetainedCount()));
            item.register(dataPipe);
            buffer.register(dataPipe);
        }
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

    private String getTaskName() {
        TimeZone timeZone = TimeZone.getDefault();
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSS");
        dateFormat.setTimeZone(timeZone);
        return "datamock_" + dateFormat.format(date);
    }

}
