package com.oceanbase.tools.datamocker.model.config.impl;

import java.util.List;
import java.util.UUID;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.model.config.AbstractTableConfig;
import com.oceanbase.tools.datamocker.model.enums.DuplicateStrategy;
import com.oceanbase.tools.datamocker.model.enums.ScriptType;
import lombok.Getter;
import lombok.Setter;

/**
 * 表任务配置对象，用于封装和表生成任务相关的配置参数
 *
 * @author yh263208
 * @date 2020-12-27 20:58
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class DefaultTableConfig extends AbstractTableConfig {
    /**
     * 表任务ID
     */
    private final String tableTaskId = UUID.randomUUID().toString();
    /**
     * 列配置集合
     */
    private List<DefaultColumnConfig> columns;
    /**
     * 最大生成数量
     */
    private Long totalCount;
    /**
     * 出现冲突时的处理策略
     */
    private DuplicateStrategy strategy;
    /**
     * 批处理大小
     */
    private Long batchSize;
    /**
     * 是否清空表
     */
    private Boolean whetherTruncate;
    /**
     * 要插入的表名
     */
    private String tableName;
    /**
     * 数据库的模式名
     */
    private String schemaName;
    /**
     * 表生成任务的超时时间，单位为毫秒，默认超时时间为1小时，即3600000
     */
    private Long timeout = 3600000L;
    /**
     * 数据写出地址
     */
    private String location;

    @Override
    protected Long maxRowCount() {
        return totalCount;
    }

    @Override
    public DuplicateStrategy duplicateStrategy() {
        return strategy;
    }

    @Override
    protected Long batchSize() {
        return batchSize;
    }

    @Override
    public Boolean truncated() {
        return whetherTruncate;
    }

    @Override
    public String tableName() {
        return tableName;
    }

    @Override
    public List<DefaultColumnConfig> columns() {
        return columns;
    }

    @Override
    public String schemaName() {
        return schemaName;
    }

    @Override
    public Long timeoutSeconds() {
        return timeout;
    }

    @Override
    public ScriptType[] scriptType() {
        ScriptType[] scriptTypes = new ScriptType[] {
                ScriptType.SQL
        };
        return scriptTypes;
    }

    @Override
    public String dataWriteLocation(ScriptType scriptType) {
        return location;
    }

    @Override
    public List<AbstractConstraint> constraints() {
        return null;
    }

    @Override
    public String tableTaskId() {
        return this.tableTaskId;
    }
}
