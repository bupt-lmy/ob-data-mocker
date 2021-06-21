package com.oceanbase.tools.datamocker;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.core.Dispatcher;
import com.oceanbase.tools.datamocker.core.read.ColumnReader;
import com.oceanbase.tools.datamocker.core.task.AbstractMockerFactory;
import com.oceanbase.tools.datamocker.core.task.TableTaskInfo;
import com.oceanbase.tools.datamocker.core.task.TableTaskMetaData;
import com.oceanbase.tools.datamocker.core.write.AbstractMockWriter;
import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.config.AbstractTableConfig;
import com.oceanbase.tools.datamocker.model.config.AbstractTaskConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.MockerBuffer;
import lombok.extern.slf4j.Slf4j;

/**
 * The implementation class of the simple dispatcher factory,
 * used to generate a simple table generation task dispatcher.
 * This is a simple dispatcher factory class for the first phase of mock data.
 * It can only be used to process table generation tasks that do not contain foreign key constraints,
 * do not contain check constraints, and unique constraints do not contain virtual columns.
 *
 * @author yh263208
 * @date 2021-01-11 21:41
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class ObMockerFactory extends AbstractMockerFactory {
    public ObMockerFactory(AbstractTaskConfig taskConfig) throws SQLException {
        super(taskConfig);
    }

    @Override
    protected Dispatcher<TableTaskInfo> generate(AbstractTaskConfig taskConfig, String taskId) throws Throwable {
        String taskName = taskConfig.taskName() == null ? getTaskName() : taskConfig.taskName();
        List<? extends AbstractTableConfig> tableConfigs = taskConfig.tasks();
        if (tableConfigs == null || tableConfigs.size() == 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Table task's count can not be null or zero");
        }
        ObModeType obModeType = taskConfig.obDialectType();
        Dispatcher<TableTaskInfo> dispatcher = new Dispatcher<>(tableConfigs.size(), taskName, taskId);
        for (int i = 0; i < tableConfigs.size(); i++) {
            AbstractTableConfig tableConfig = tableConfigs.get(i);
            List<AbstractConstraint> constraints = this.getConstraints(tableConfig, taskConfig.obDialectType());
            List<ColumnReader> columnReaders = this.getColumnReader(tableConfig, constraints);
            Map<String, AbstractDataType> tableSchema = getTableSchema(tableConfig);
            MockerBuffer buffer = new MockerBuffer(tableSchema, tableConfig.maxBatchSize());
            TableTaskMetaData metaData = new TableTaskMetaData(tableSchema, tableConfig, obModeType, taskId, 0, i);
            DataSource dataSource = getDataSource(metaData.getTableTaskId());
            List<MockerFile> managers = getFileManager(metaData.getTableTaskId(), tableConfig);
            List<AbstractMockWriter> dataWriter = this.getDataWriter(tableConfig, buffer, managers, dataSource);
            TableTaskInfo bean = new TableTaskInfo(columnReaders, dataWriter, constraints, buffer, dataSource, managers, metaData);
            dispatcher.setObj(i, bean);
        }
        return dispatcher;
    }
}
