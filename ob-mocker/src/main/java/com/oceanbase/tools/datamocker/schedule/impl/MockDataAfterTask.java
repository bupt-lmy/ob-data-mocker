package com.oceanbase.tools.datamocker.schedule.impl;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.core.task.AbstractCallBack;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.core.task.TableTaskMetaData;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.schedule.AbstractMockTask;
import com.oceanbase.tools.datamocker.util.DbObjectNameUtil;
import com.oceanbase.tools.datamocker.util.SqlUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * mock数据业务逻辑执行完毕后的收尾任务，主要是统计当前表的数据量
 *
 * @author yh263208
 * @date 2021-01-14 10:54
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class MockDataAfterTask extends AbstractMockTask {
    /**
     * 数据源
     */
    private final DataSource dataSource;

    public MockDataAfterTask(TableTaskMetaData metaData, TableTaskContext context, DataSource dataSource) {
        super(metaData, context);
        if (dataSource == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Datasource can not be null");
            log.error("fail to init mock data after task, datasource can not be null", e);
            throw e;
        }
        this.dataSource = dataSource;
    }

    @Override
    public Void execute(TableTaskMetaData metaData, TableTaskContext context) throws Throwable {
        log.info("begin execute mock after task");
        String sql;
        if (ObModeType.OB_ORACLE.equals(metaData.getDialectType())) {
            sql = String.format("select count(*) from \"%s\".\"%s\"; ", DbObjectNameUtil.doubleCharToEscape(metaData.getSchema(), '"'),
                    DbObjectNameUtil.doubleCharToEscape(metaData.getTableName(), '"'));
        } else if (ObModeType.OB_MYSQL.equals(metaData.getDialectType())) {
            sql = String.format("select count(*) from `%s`.`%s`; ", DbObjectNameUtil.doubleCharToEscape(metaData.getSchema(), '`'),
                    DbObjectNameUtil.doubleCharToEscape(metaData.getTableName(), '`'));
        } else {
            MockerException e = new MockerException(MockerError.INVALID_OB_MODE);
            log.error("fail to execute after task for mock", e);
            throw e;
        }
        SqlUtil.executeQuery(this.dataSource, sql, null, new AbstractCallBack<ResultSet>() {
            @Override
            public void doOnSuccess(ResultSet resultSet) throws Exception {
                ResultSetMetaData md = resultSet.getMetaData();
                if (md.getColumnCount() != 1) {
                    throw new MockerException(MockerError.ILLEGAL_RETURN_VALUE,
                            String.format("Column count for \"select count(*) from \"%s\".\"%s\" is not equal to one, [%d!=1]",
                                    metaData.getSchema(), metaData.getTableName(), md.getColumnCount()));
                }
                if (resultSet.next()) {
                    long rowCount = resultSet.getLong(1);
                    context.setCurrentRecordNum(rowCount);
                }
            }

            @Override
            public void doOnFailure(ResultSet resultSet, Throwable e) throws Throwable {
                log.error("fail to execute after task for mock", e);
                throw e;
            }
        });
        return null;
    }
}
