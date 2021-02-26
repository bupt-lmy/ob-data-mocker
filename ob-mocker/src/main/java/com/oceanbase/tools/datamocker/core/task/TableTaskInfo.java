package com.oceanbase.tools.datamocker.core.task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.core.read.ColumnReader;
import com.oceanbase.tools.datamocker.core.write.AbstractMockWriter;
import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import com.oceanbase.tools.datamocker.util.MockerBuffer;
import lombok.Getter;

/**
 * 表生成任务对象，用于封装一个表的生成任务相关的所有对象
 *
 * @author yh263208
 * @date 2021-01-09 19:33
 * @since OBMOCKER_0.1.0_snapshot
 */
@Getter
public class TableTaskInfo {
    /**
     * 表生成任务的元数据信息
     */
    private TableTaskMetaData metaData;
    /**
     * mock数据缓冲对象
     */
    private MockerBuffer buffer;
    /**
     * 列数据生成原语集合对象
     */
    private List<ColumnReader> columnReaders;
    /**
     * 数据写出原语，用于向数据库中写入数据
     */
    private List<AbstractMockWriter> dataWriters;
    /**
     * 约束集合，用于描述该表中的约束信息
     */
    private List<AbstractConstraint> constraints;
    /**
     * 数据源头
     */
    private DataSource dataSource;
    /**
     * 文件管理器
     */
    private List<MockerFile> fileManagers;

    /**
     * 构造方法，用于构造出一个表任务bean对象
     *
     * @param columnReaders 列数据生成原语
     * @param dataWriters   数据写入生成原语
     * @param constraints   表约束对象集合
     */
    public TableTaskInfo(List<ColumnReader> columnReaders, List<AbstractMockWriter> dataWriters,
            List<AbstractConstraint> constraints, MockerBuffer buffer, DataSource dataSource, List<MockerFile> fileManagers,
            TableTaskMetaData metaData) {
        this.columnReaders = columnReaders;
        this.dataWriters = dataWriters;
        this.constraints = constraints;
        this.metaData = metaData;
        this.buffer = buffer;
        this.dataSource = dataSource;
        this.fileManagers = fileManagers;
    }

    /**
     * 获取列分组集合
     *
     * @return 返回列分组集合
     */
    public Set<String> columnGroups() {
        Set<String> returnVal = new HashSet<>();
        for (ColumnReader reader : this.columnReaders) {
            returnVal.add(reader.groupId());
        }
        return returnVal;
    }

    /**
     * 获取数据写出原语的分组集合
     *
     * @return 返回分组集合
     */
    public Set<String> dataWriteGroups() {
        Set<String> returnVal = new HashSet<>();
        for (AbstractMockWriter writer : this.dataWriters) {
            returnVal.add(writer.groupId());
        }
        return returnVal;
    }
}
