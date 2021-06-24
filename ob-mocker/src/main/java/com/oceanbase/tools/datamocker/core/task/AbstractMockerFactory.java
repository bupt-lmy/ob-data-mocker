package com.oceanbase.tools.datamocker.core.task;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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

import com.oceanbase.tools.datamocker.ObDataMocker;
import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.constraint.ConstraintFactory;
import com.oceanbase.tools.datamocker.core.Dispatcher;
import com.oceanbase.tools.datamocker.core.read.ColumnReader;
import com.oceanbase.tools.datamocker.core.write.AbstractMockWriter;
import com.oceanbase.tools.datamocker.core.write.JdbcWriter;
import com.oceanbase.tools.datamocker.core.write.SqlScriptWriter;
import com.oceanbase.tools.datamocker.core.write.output.MockerDataSource;
import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.config.AbstractColumnConfig;
import com.oceanbase.tools.datamocker.model.config.AbstractTableConfig;
import com.oceanbase.tools.datamocker.model.config.AbstractTaskConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.enums.ScriptType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.datamocker.schedule.AbstractScheduler;
import com.oceanbase.tools.datamocker.schedule.impl.DefaultScheduler;
import com.oceanbase.tools.datamocker.util.DbObjectNameUtil;
import com.oceanbase.tools.datamocker.util.MockDataPipe;
import com.oceanbase.tools.datamocker.util.MockerBuffer;
import com.oceanbase.tools.datamocker.util.SqlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.MDC;

/**
 * Abstract data simulator, used to create a new data simulator
 *
 * @author yh263208
 * @date 2021-02-03 20:10
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public abstract class AbstractMockerFactory {
    /**
     * Abstract task configuration
     */
    private final AbstractTaskConfig taskConfig;
    /**
     * The internal data source of the factory type, which is used to verify the existence of tables,
     * check constraints and other information
     */
    private MockerDataSource innerDatasource = null;
    /**
     * The data source is a business data source
     */
    private final Map<String, DataSource> taskId2DataSource;
    /**
     * Mock data file manager collection
     */
    private final Map<String, List<MockerFile>> taskId2MockerFiles;

    public AbstractMockerFactory(AbstractTaskConfig taskConfig) {
        Validate.notNull(taskConfig, "TaskConfig can not be null for AbstractMockerFactory");
        this.taskConfig = taskConfig;
        this.taskId2DataSource = new HashMap<>();
        this.taskId2MockerFiles = new HashMap<>();
    }

    /**
     * Verify the validity of the database configuration object package
     *
     * @param dbConfig configuration for database
     * @return verify result
     */
    private boolean validateDbConfig(DataBaseConfig dbConfig) {
        if (dbConfig == null) {
            return false;
        }
        return !StringUtils.isBlank(dbConfig.getUser()) && !StringUtils.isBlank(dbConfig.getTenant())
                && !StringUtils.isBlank(dbConfig.getHost());
    }

    /**
     * Get simulated data object
     */
    public ObDataMocker create() {
        return create(new DefaultScheduler(this.taskConfig.maxConnection()));
    }

    /**
     * Get Mock data object
     *
     * @param scheduler Scheduler object, used for thread resource scheduling
     */
    public ObDataMocker create(AbstractScheduler scheduler) {
        Validate.notNull(scheduler, "Scheduler can not be null for AbstractMockerFactory#create");
        try {
            String taskId = UUID.randomUUID().toString().toUpperCase();
            MDC.put("mocktask.workspace", taskId);
            if (validateDbConfig(taskConfig.dbConfig())) {
                if (this.innerDatasource == null) {
                    this.innerDatasource = new MockerDataSource(taskConfig.dbConfig(), 3, 5, 3, null);
                }
            }
            for (AbstractTableConfig tableConfig : this.taskConfig.tasks()) {
                validateTableByJdbc(tableConfig.schemaName(), tableConfig.tableName());
            }
            Dispatcher<TableTaskInfo> dispatcher = generate(this.taskConfig, taskId);
            this.innerDatasource.clear();
            return new ObDataMocker(dispatcher, scheduler);
        } catch (Throwable e) {
            throw new MockerException(e);
        }
    }

    /**
     * The implementer himself defines the logic of the dispatcher object
     *
     * @param task Task configuration
     * @param taskId Task Id
     * @return Returns the dispatcher object
     */
    abstract protected Dispatcher<TableTaskInfo> generate(AbstractTaskConfig task, String taskId) throws Throwable;

    /**
     * Verify the existence of the table
     *
     * @param table table name
     * @param schema The database or schema
     * @throws MockerException Throw an exception when the table existence check fails
     */
    protected void validateTableByJdbc(String schema, String table) throws Throwable {
        if (this.innerDatasource == null) {
            return;
        }
        String sql;
        if (ObModeType.OB_ORACLE.equals(this.taskConfig.obDialectType())) {
            sql = String.format("select count(*) from \"%s\".\"%s\"", DbObjectNameUtil.doubleCharToEscape(schema, '"'),
                    DbObjectNameUtil.doubleCharToEscape(table, '"'));
        } else if (ObModeType.OB_MYSQL.equals(this.taskConfig.obDialectType())) {
            sql = String.format("select count(*) from `%s`.`%s`", DbObjectNameUtil.doubleCharToEscape(schema, '`'),
                    DbObjectNameUtil.doubleCharToEscape(table, '`'));
        } else {
            throw new MockerException(MockerError.INVALID_OB_MODE);
        }
        SqlUtil.executeQuery(this.innerDatasource, sql, null, new AbstractCallBack<ResultSet>() {
            @Override
            public void doOnSuccess(ResultSet result) {
                log.info("Verify the existence of the database table successfully, schema={}, table={}", schema, table);
            }

            @Override
            public void doOnFailure(ResultSet result, Throwable e) {
                log.error("Fail to verify the existence of database table, schema={}, table={}", schema, table, e);
                throw new MockerException(MockerError.OPERATION_FAILURE, e.getMessage());
            }
        });
    }

    /**
     * Get the table structure
     *
     * @param tableConfig table configuration
     * @return schema for a certain table
     */
    protected Map<String, AbstractDataType<?, ? extends Comparable<?>>> getTableSchema(
            AbstractTableConfig tableConfig) {
        Map<String, AbstractDataType<?, ? extends Comparable<?>>> columnName2DataType = new HashMap<>();
        for (AbstractColumnConfig columnConfig : tableConfig.columns()) {
            columnName2DataType.putIfAbsent(columnConfig.columnName(), columnConfig.columnType());
        }
        return columnName2DataType;
    }

    /**
     * Get the constraint object related to the table
     *
     * @param tableConfig table configuration
     * @return list of constraint
     */
    protected List<AbstractConstraint> getConstraints(AbstractTableConfig tableConfig, ObModeType dialectType)
            throws Throwable {
        if (tableConfig.constraints() != null) {
            return tableConfig.constraints();
        } else if (this.innerDatasource == null) {
            return Collections.emptyList();
        }
        Map<String, AbstractDataType<?, ? extends Comparable<?>>> columnName2DataType = getTableSchema(tableConfig);
        List<ConstraintFactory> factories = ConstraintFactory.listInstances();
        List<AbstractConstraint> returnVal = new ArrayList<>();
        for (ConstraintFactory factory : factories) {
            List<AbstractConstraint> customConstraint =
                    factory.make(this.innerDatasource, dialectType, tableConfig.schemaName(), tableConfig.tableName(),
                            columnName2DataType, tableConfig.maxCount().intValue());
            if (customConstraint != null) {
                returnVal.addAll(customConstraint);
            }
        }
        return returnVal;
    }

    /**
     * Get data source
     *
     * @param tableTaskId table task Id
     * @return data source
     * @exception SQLException An exception is thrown if the connection establishment fails
     */
    protected synchronized DataSource getDataSource(String tableTaskId) throws SQLException {
        if (tableTaskId == null) {
            return null;
        }
        DataSource dataSource = this.taskId2DataSource.get(tableTaskId);
        if (dataSource != null) {
            return dataSource;
        }
        if (!validateDbConfig(this.taskConfig.dbConfig())) {
            return null;
        }
        Map<String, String> realParam = new HashMap<>();
        realParam.put("autoReconnect", "true");
        realParam.put("rewriteBatchedStatements", "true");
        realParam.put("emulateUnsupportedPstmts", "false");
        // realParam.put("useServerPrepStmts", "true");
        Map<String, String> param = this.taskConfig.dbConfig().getConnectParam();
        if (param != null) {
            Set<Entry<String, String>> entries = param.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                realParam.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        dataSource = new MockerDataSource(this.taskConfig.dbConfig(), taskConfig.minConnection(),
                taskConfig.maxConnection(), taskConfig.connectionIncreasementStep(), realParam);
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
    protected synchronized List<MockerFile> getFileManager(String tableTaskId, AbstractTableConfig tableConfig)
            throws IOException {
        Validate.notEmpty(tableTaskId, "Table task id can not be null");
        Validate.notNull(tableConfig, "Table config can not be null");
        List<MockerFile> returnVal = taskId2MockerFiles.get(tableTaskId);
        if (returnVal == null) {
            returnVal = new LinkedList<>();
            taskId2MockerFiles.put(tableTaskId, returnVal);
            for (ScriptType scriptType : tableConfig.scriptType()) {
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
    protected List<AbstractMockWriter> getDataWriter(AbstractTableConfig tableConfig, MockerBuffer buffer,
            List<MockerFile> managers, DataSource dataSource) {
        Validate.notNull(managers, "Mocker file manager list can not be null");
        List<AbstractMockWriter> dataWriters = new LinkedList<>();
        for (MockerFile manager : managers) {
            SqlScriptWriter writer = new SqlScriptWriter(manager, this.taskConfig.obDialectType(),
                    tableConfig.schemaName(), tableConfig.tableName());
            dataWriters.add(writer);
        }
        if (dataSource == null) {
            return dataWriters;
        }
        JdbcWriter writer = new JdbcWriter(dataSource, this.taskConfig.obDialectType(),
                tableConfig.schemaName(), tableConfig.tableName());
        dataWriters.add(writer);
        Map<String, AbstractDataPipe<MockRowData>> groupId2DataPipe = new HashMap<>();
        for (AbstractMockWriter item : dataWriters) {
            AbstractDataPipe<MockRowData> dataPipe = groupId2DataPipe.computeIfAbsent(item.groupId(),
                    s -> new MockDataPipe(tableConfig.maxRetainedCount()));
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
    protected List<ColumnReader<?>> getColumnReader(AbstractTableConfig tableConfig,
            List<AbstractConstraint> constraints) {
        List<? extends AbstractColumnConfig> columnConfigs = tableConfig.columns();
        List<Set<String>> colGroupList = new ArrayList<>();
        for (AbstractConstraint constraint : constraints) {
            Map<String, Integer> cols = constraint.columns().get(tableConfig.tableName());
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
        for (AbstractColumnConfig columnConfig : columnConfigs) {
            AbstractDataType<?, ? extends Comparable<?>> dataType = columnConfig.columnType();
            String columnName = columnConfig.columnName();
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

    protected String getTaskName() {
        TimeZone timeZone = TimeZone.getDefault();
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSS");
        dateFormat.setTimeZone(timeZone);
        return "datamock_" + dateFormat.format(date);
    }

}
