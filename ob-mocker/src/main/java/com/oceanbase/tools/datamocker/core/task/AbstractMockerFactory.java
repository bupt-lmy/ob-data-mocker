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
import com.oceanbase.tools.datamocker.core.write.DataBaseWriter;
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
 * 抽象数据模拟器，用于new一个数据模拟器出来
 *
 * @author yh263208
 * @date 2021-02-03 20:10
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public abstract class AbstractMockerFactory {
    /**
     * 抽象任务配置
     */
    private AbstractTaskConfig taskConfig;
    /**
     * 工厂类型的内部数据源，使用该数据源进行表存在性校验，约束等信息的校验
     */
    private MockerDataSource innerDatasource = null;
    /**
     * 该数据源是业务数据源
     */
    private Map<String, DataSource> taskId2DataSource;
    /**
     * mock数据文件管理器集合
     */
    private Map<String, List<MockerFile>> taskId2MockerFiles;

    public AbstractMockerFactory(AbstractTaskConfig taskConfig) {
        this.taskConfig = taskConfig;
        if (taskConfig == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Input task config can not be null");
        }
        this.taskId2DataSource = new HashMap<>();
        this.taskId2MockerFiles = new HashMap<>();
    }

    /**
     * 验证数据库配置对象封装体的有效性
     *
     * @param dbConfig 数据库配置
     * @return 返回验证结果
     */
    private boolean validateDbConfig(DataBaseConfig dbConfig) {
        if (dbConfig == null) {
            return false;
        }
        if (StringUtils.isBlank(dbConfig.getUser()) || StringUtils.isBlank(dbConfig.getTenant()) || StringUtils.isBlank(
                dbConfig.getHost())) {
            return false;
        }
        return true;
    }

    /**
     * 获取模拟数据对象
     *
     * @throws Exception 可能会抛出异常
     */
    public ObDataMocker create() {
        return create(new DefaultScheduler(this.taskConfig.maxConnection()));
    }

    /**
     * 获取模拟数据对象
     *
     * @param scheduler 调度器对象，用于线程资源的调度
     * @throws Exception 生成mocker对象可能会抛出异常
     */
    public ObDataMocker create(AbstractScheduler scheduler) {
        if (scheduler == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Scheduler for mocker factory can not be null");
        }
        try {
            String taskId = UUID.randomUUID().toString().toUpperCase();
            MDC.put("mocktask.workspace", taskId);
            if (validateDbConfig(taskConfig.dbConfig())) {
                if (this.innerDatasource == null) {
                    this.innerDatasource = new MockerDataSource(taskConfig.dbConfig(), 3, 5, 3, null);
                }
            }
            for (AbstractTableConfig tableConfig : this.taskConfig.tasks()) {
                validateTableFromDB(tableConfig.schemaName(), tableConfig.tableName());
            }
            Dispatcher<TableTaskInfo> dispatcher = generate(this.taskConfig, taskId);
            this.innerDatasource.clear();
            return new ObDataMocker(dispatcher, scheduler);
        } catch (Throwable e) {
            throw new MockerException(e);
        }
    }

    /**
     * 实现者自己定义分发器对象的逻辑
     *
     * @param task   任务配置读喜庆封装体
     * @param taskId 任务Id
     * @return 返回分发器对象
     */
    abstract protected Dispatcher<TableTaskInfo> generate(AbstractTaskConfig task, String taskId) throws Throwable;

    /**
     * 验证表的存在性
     *
     * @param table  表名
     * @param schema 所在数据库或者schema
     * @throws MockerException 表存在性校验失败时抛出异常
     */
    protected void validateTableFromDB(String schema, String table) throws Throwable {
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
     * 获取表结构
     *
     * @param tableConfig 表配置信息
     * @return 返回表结构
     */
    protected Map<String, AbstractDataType> getTableSchema(AbstractTableConfig tableConfig) {
        Map<String, AbstractDataType> columnName2DataType = new HashMap<>();
        for (AbstractColumnConfig columnConfig : tableConfig.columns()) {
            columnName2DataType.putIfAbsent(columnConfig.columnName(), columnConfig.columnType());
        }
        return columnName2DataType;
    }

    /**
     * 获取表相关的约束对象
     *
     * @param tableConfig 表定义
     * @return 返回约束集合
     */
    protected List<AbstractConstraint> getConstraints(AbstractTableConfig tableConfig, ObModeType dialectType) throws Throwable {
        if (tableConfig.constraints() != null) {
            return tableConfig.constraints();
        } else if (this.innerDatasource == null) {
            return Collections.emptyList();
        }
        Map<String, AbstractDataType> columnName2DataType = getTableSchema(tableConfig);
        List<ConstraintFactory> factories = ConstraintFactory.listInstances();
        List<AbstractConstraint> returnVal = new ArrayList<>();
        for (ConstraintFactory factory : factories) {
            List<AbstractConstraint> customConstraint = factory.make(this.innerDatasource, dialectType, tableConfig.schemaName(),
                    tableConfig.tableName(), columnName2DataType, tableConfig.maxCount().intValue());
            if (customConstraint != null) {
                returnVal.addAll(customConstraint);
            }
        }
        return returnVal;
    }

    /**
     * 获取数据源
     *
     * @return 返回数据源
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
        //realParam.put("useServerPrepStmts", "true");
        Map<String, String> param = this.taskConfig.dbConfig().getConnectParam();
        if (param != null) {
            Set<Entry<String, String>> entries = param.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                realParam.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        dataSource = new MockerDataSource(this.taskConfig.dbConfig(), taskConfig.minConnection(), taskConfig.maxConnection(),
                taskConfig.connectionIncreasementStep(), realParam);
        this.taskId2DataSource.putIfAbsent(tableTaskId, dataSource);
        return dataSource;
    }

    /**
     * 获取文件管理器集合
     *
     * @return 返回数据源
     */
    protected synchronized List<MockerFile> getFileManager(String tableTaskId, AbstractTableConfig tableConfig) throws IOException {
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
     * 获取数据库写入原语
     *
     * @param tableConfig 表生成任务封装体
     * @return 返回原语集合
     */
    protected List<AbstractMockWriter> getDataWriter(AbstractTableConfig tableConfig, MockerBuffer buffer, List<MockerFile> managers,
            DataSource ds) {
        Validate.notNull(managers, "Mocker file manager list can not be null");
        List<AbstractMockWriter> dataWriters = new ArrayList<>();
        for (MockerFile manager : managers) {
            SqlScriptWriter writer = new SqlScriptWriter(manager, this.taskConfig.obDialectType(), tableConfig.schemaName(),
                    tableConfig.tableName());
            dataWriters.add(writer);
        }
        if (ds == null) {
            return dataWriters;
        }
        DataBaseWriter writer = new DataBaseWriter(ds, this.taskConfig.obDialectType(), tableConfig.schemaName(),
                tableConfig.tableName());
        dataWriters.add(writer);
        Map<String, AbstractDataPipe> map = new HashMap<>();
        for (AbstractMockWriter item : dataWriters) {
            AbstractDataPipe dataPipe = map.getOrDefault(item.groupId(), new MockDataPipe(tableConfig.maxRetainedCount()));
            item.register(dataPipe);
            buffer.register(dataPipe);
        }
        return dataWriters;
    }

    /**
     * 获取一个表生成任务的全部列数据原语
     *
     * @param tableConfig 表生成任务
     * @param constraints 约束集合
     * @return 返回原语列表
     */
    protected List<ColumnReader> getColumnReader(AbstractTableConfig tableConfig, List<AbstractConstraint> constraints) {
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
        List<ColumnReader> returnValue = new ArrayList<>();
        for (AbstractColumnConfig columnConfig : columnConfigs) {
            AbstractDataType dataType = columnConfig.columnType();
            String columnName = columnConfig.columnName();
            Set<Map.Entry<String, Set<String>>> entrySet = groupMap.entrySet();
            ColumnReader reader = null;
            for (Map.Entry<String, Set<String>> item : entrySet) {
                String groupId = item.getKey();
                if (item.getValue().contains(columnName)) {
                    reader = new ColumnReader(dataType, columnName, groupId);
                    break;
                }
            }
            if (reader == null) {
                reader = new ColumnReader(dataType, columnName, UUID.randomUUID().toString());
            }
            returnValue.add(reader);
        }
        return returnValue;
    }

    /**
     * 获取任务名称
     *
     * @return 返回任务名称
     */
    protected String getTaskName() {
        TimeZone timeZone = TimeZone.getDefault();
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSS");
        dateFormat.setTimeZone(timeZone);
        return "datamock_" + dateFormat.format(date);
    }
}
