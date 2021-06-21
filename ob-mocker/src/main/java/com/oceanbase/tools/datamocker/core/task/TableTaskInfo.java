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
 * Table generation task object, used to encapsulate all objects related to a table generation task
 *
 * @author yh263208
 * @date 2021-01-09 19:33
 * @since OBMOCKER_0.1.0_snapshot
 */
@Getter
public class TableTaskInfo {
    /**
     * Metadata information of the table generation task
     */
    private final TableTaskMetaData metaData;
    /**
     * Mock data buffer object
     */
    private final MockerBuffer buffer;
    private final List<ColumnReader> columnReaders;
    private final List<AbstractMockWriter> dataWriters;
    private final List<AbstractConstraint> constraints;
    private final DataSource dataSource;
    private final List<MockerFile> fileManagers;

    /**
     * Construction method, used to construct a table task bean object
     *
     * @param columnReaders list of column reader
     * @param dataWriters list of writers
     * @param constraints list of constraint
     * @param buffer buffer object
     * @param dataSource datasource
     * @param fileManagers list file manager
     * @param metaData meta data for table task
     */
    public TableTaskInfo(List<ColumnReader> columnReaders, List<AbstractMockWriter> dataWriters,
            List<AbstractConstraint> constraints, MockerBuffer buffer, DataSource dataSource,
            List<MockerFile> fileManagers,
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
     * Get column grouping collection
     *
     * @return Returns the column grouping collection
     */
    public Set<String> columnGroups() {
        Set<String> returnVal = new HashSet<>();
        for (ColumnReader reader : this.columnReaders) {
            returnVal.add(reader.groupId());
        }
        return returnVal;
    }

    /**
     * Get data and write out a grouping set of primitives
     *
     * @return Return to grouped collection
     */
    public Set<String> dataWriteGroups() {
        Set<String> returnVal = new HashSet<>();
        for (AbstractMockWriter writer : this.dataWriters) {
            returnVal.add(writer.groupId());
        }
        return returnVal;
    }
}
