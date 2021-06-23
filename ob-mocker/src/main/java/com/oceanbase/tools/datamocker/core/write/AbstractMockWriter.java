package com.oceanbase.tools.datamocker.core.write;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.core.task.AbstractDataPipe;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.model.mock.MockRowData;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.Validate;

/**
 * Abstract data writer, used to write data to the data source
 *
 * @author yh263208
 * @date 2021-01-15 11:50
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public abstract class AbstractMockWriter {
    /**
     * Data communication pipeline, obtain data through pipeline
     */
    private AbstractDataPipe<MockRowData> dataPipe;

    /**
     * Register a pipeline
     *
     * @param dataPipe Pipe object
     */
    public void register(AbstractDataPipe<MockRowData> dataPipe) {
        Validate.notNull(dataPipe, "DataPipe can not be null for AbstractMockWriter#register");
        this.dataPipe = dataPipe;
    }

    /**
     * Write method by which data is written to the data source
     *
     * @return Returns the number of data items written
     */
    public Long write() throws Throwable {
        if (this.dataPipe == null) {
            log.error("Fail to read any data from the data pipe because the data pipe is null");
            throw new MockerException(MockerError.PARAMETER_ERROR, "Data pipe can not be null");
        }
        List<MockRowData> rows = this.dataPipe.read(10, TimeUnit.SECONDS);
        if (rows == null) {
            return null;
        } else if (rows.size() == 0) {
            return 0L;
        }
        return doWrite(rows);
    }

    abstract protected Long doWrite(List<MockRowData> rows) throws Throwable;

    /**
     * Mockwriter is used to output data to a database or script file. There are currently two output
     * sources, one is the database and the other is the script file. Reader and writer constitute a
     * producer and consumer model, that is, a reader produces data, and multiple writers output data,
     * but the writer output to the database and the writer output to the file cannot share the same
     * data communication "pipe", otherwise to the database The writer written out and the writer
     * written to the text file will compete for the data, resulting in only part of the data being
     * written to the database and the text file. The solution is to bind different data pipelines to
     * the writers of different output sources. The groupId here is used to distinguish Different types
     * of writers
     */
    abstract public String groupId();
}
