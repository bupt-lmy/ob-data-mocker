package com.oceanbase.tools.datamocker.schedule.impl;

import java.util.List;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.constraint.ConstraintFactory;
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
 * mock数据业务逻辑开始前的准备逻辑，在这里主要是进行表的清空以及约束的重新装载逻辑
 *
 * @author yh263208
 * @date 20210-01-13 22:37
 * @since OBMOCKER_0.1.0_snapshot
 */
@Slf4j
public class MockDataBeforeTask extends AbstractMockTask {
    /**
     * 数据源
     */
    private final DataSource dataSource;

    public MockDataBeforeTask(TableTaskMetaData metaData, TableTaskContext context, DataSource dataSource) {
        super(metaData, context);
        if (dataSource == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Datasource can not be null");
            log.error("The mock data preparation task failed to initialize because the data source could not be found", e);
            throw e;
        }
        this.dataSource = dataSource;
    }

    @Override
    public Void execute(TableTaskMetaData metaData, TableTaskContext context) throws Throwable {
        log.info("Start the mock data preparation task");
        //如果设置了清空表则需要重新加载约束
        if (Boolean.TRUE.equals(metaData.getShouldTruncate())) {
            String sql;
            if (ObModeType.OB_MYSQL.equals(metaData.getDialectType())) {
                sql = String.format("delete from `%s`.`%s` where 1=1; ", DbObjectNameUtil.doubleCharToEscape(metaData.getSchema(), '`'),
                        DbObjectNameUtil.doubleCharToEscape(metaData.getTableName(), '`'));
            } else if (ObModeType.OB_ORACLE.equals(metaData.getDialectType())) {
                sql = String.format("delete from \"%s\".\"%s\" where 1=1; ", DbObjectNameUtil.doubleCharToEscape(metaData.getSchema(), '"'),
                        DbObjectNameUtil.doubleCharToEscape(metaData.getTableName(), '"'));
            } else {
                MockerException e = new MockerException(MockerError.INVALID_OB_MODE);
                log.error("Fail to execute mock data preparation task because the ObModeType is illegal, obModeType={}",
                        metaData.getDialectType(), e);
                throw e;
            }
            SqlUtil.executeUpdate(this.dataSource, sql, null, new AbstractCallBack<Integer>() {
                @Override
                public void doOnSuccess(Integer effectRow) throws Throwable {
                    log.info("Truncate table successfully, schema={}, tableName={}, effectRow={}", metaData.getSchema(),
                            metaData.getTableName(), effectRow);
                    if (effectRow > 0) {
                        List<ConstraintFactory> factories = ConstraintFactory.listInstances();
                        for (ConstraintFactory factory : factories) {
                            List<AbstractConstraint> customConstraint = factory.make(dataSource, metaData.getDialectType(),
                                    metaData.getSchema(), metaData.getTableName(), metaData.getTableSchema(),
                                    metaData.getTotalCount().intValue());
                            context.setConstraints(customConstraint);
                        }
                        log.info("Reload constraint succeeded");
                    }
                }

                @Override
                public void doOnFailure(Integer effectRow, Throwable e) throws Throwable {
                    log.error("Fail to execute mock data preparation task", e);
                    throw e;
                }
            });
        }
        return null;
    }
}
