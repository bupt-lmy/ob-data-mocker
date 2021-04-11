package com.oceanbase.tools.datamocker.schedule.impl;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.constraint.ConstraintFactory;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.core.task.TableTaskMetaData;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.schedule.AbstractMockTask;
import com.oceanbase.tools.datamocker.util.SqlUtil;
import com.oceanbase.tools.datamocker.util.SqlUtil.CallBack;
import lombok.extern.slf4j.Slf4j;

/**
 * mock数据业务逻辑开始前的准备逻辑，在这里主要是进行表的清空以及约束的重新装载逻辑
 *
 * @author yh263208
 * @date 20210-01-13 22:37
 * @since OBMOCKER_0.1.0_snapshot
 */
@Slf4j
public class MockDataBeforeTask extends AbstractMockTask<List<AbstractConstraint>> {
    /**
     * 任务执行结果
     */
    private Boolean result = false;
    /**
     * 数据源
     */
    private final DataSource dataSource;

    public MockDataBeforeTask(TableTaskMetaData metaData, TableTaskContext context, DataSource dataSource) {
        super(metaData, context);
        if (dataSource == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "data source can not be null");
            log.error("fail to init mock data before task, data source can not be null", e);
            throw e;
        }
        this.dataSource = dataSource;
    }

    @Override
    protected boolean isTaskSuccess() {
        return this.result;
    }

    @Override
    public List<AbstractConstraint> execute(TableTaskMetaData metaData, TableTaskContext context) {
        log.info("begin execute mock before task");
        //如果设置了清空表则需要重新加载约束
        if (Boolean.TRUE.equals(metaData.getShouldTruncate())) {
            String sql;
            if (ObModeType.OB_MYSQL.equals(metaData.getDialectType())) {
                sql = String.format("delete from `%s`.`%s` where 1=1; ", metaData.getSchema(), metaData.getTableName());
            } else if (ObModeType.OB_ORACLE.equals(metaData.getDialectType())) {
                sql = String.format("delete from %s.\"%s\" where 1=1; ", metaData.getSchema(), metaData.getTableName());
            } else {
                result = false;
                log.error("fail to execute before task for mock", new MockerException(MockerError.INVALID_OB_MODE));
                return null;
            }
            List<AbstractConstraint> returnVal = new ArrayList<>();
            SqlUtil.executeUpdate(this.dataSource, sql, null, new CallBack<Integer>() {
                @Override
                public void onComplete(Integer effectRow) {
                    log.info(String.format("truncate table %s.\"%s\" successfully, effect row is %d", metaData.getSchema(),
                            metaData.getTableName(), effectRow));
                    if (effectRow > 0) {
                        List<ConstraintFactory> factories = ConstraintFactory.listInstances();
                        for (ConstraintFactory factory : factories) {
                            List<AbstractConstraint> customConstraint = factory.make(dataSource, metaData.getDialectType(),
                                    metaData.getSchema(), metaData.getTableName(), metaData.getTableSchema(),
                                    metaData.getTotalCount().intValue());
                            if (customConstraint != null) {
                                returnVal.addAll(customConstraint);
                            }
                        }
                        log.info("reload constraints settings successfully");
                    }
                    result = true;
                }

                @Override
                public void onFailure(Throwable e) {
                    result = false;
                    log.error("fail to execute before task for mock", e);
                }
            });
            return returnVal;
        }
        this.result = true;
        return null;
    }
}
