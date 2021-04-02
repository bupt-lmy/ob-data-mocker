package com.oceanbase.tools.datamocker.model.config.impl;

import java.util.List;

import com.oceanbase.tools.datamocker.model.config.AbstractTaskConfig;
import com.oceanbase.tools.datamocker.model.config.model.DataBaseConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import lombok.Getter;
import lombok.Setter;

/**
 * 整体任务配置对象，用于配置整体的任务参数
 *
 * @author yh263208
 * @date 2020-12-27 20:59
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class DefaultTaskConfig extends AbstractTaskConfig {
    /**
     * 表生成任务集合
     */
    private List<DefaultTableConfig> tables;
    /**
     * 生成任务的方言类型
     */
    private ObModeType dialectType;
    /**
     * 数据库配置对象
     */
    private DataBaseConfig dbConfig;
    /**
     * 任务名称，默认为null，如果使用者不传入一个任务名称则这里会自动生成一个
     */
    private String taskName = null;
    /**
     * 连接池最小连接数
     */
    private int minConnectionSize;
    /**
     * 连接池最大连接数
     */
    private int maxConnectionSize;
    /**
     * 连接池连接数扩增步长
     */
    private int connectionIncreasementStep;

    @Override
    public ObModeType obDialectType() {
        return dialectType;
    }

    @Override
    public DataBaseConfig dbConfig() {
        return dbConfig;
    }

    @Override
    public List<DefaultTableConfig> tasks() {
        return tables;
    }

    @Override
    public String taskName() {
        return taskName;
    }

    @Override
    protected int connectionIncreaseStepCount() {
        return this.connectionIncreasementStep;
    }

    @Override
    protected int connectionInitCount() {
        return this.minConnectionSize;
    }

    @Override
    protected int connectionMaxCount() {
        return this.maxConnectionSize;
    }
}
