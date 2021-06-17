package com.oceanbase.tools.datamocker.core.write;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.core.task.AbstractDataPipe;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象数据写出器，用于向数据源中写出数据
 *
 * @author yh263208
 * @date 2021-01-15 11:50
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public abstract class AbstractMockWriter {
    /**
     * 数据通信管道，通过管道获取数据
     */
    private AbstractDataPipe dataPipe;

    /**
     * 注册一个管道
     *
     * @param dataPipe 管道对象
     */
    public void register(AbstractDataPipe dataPipe) {
        if (dataPipe == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Data pipe can not be null");
            log.error("data pipe for sql script writer is necessary", e);
            throw e;
        }
        this.dataPipe = dataPipe;
    }

    /**
     * 写方法，通过该方法向数据源中写入数据
     *
     * @return 返回写出的数据条数
     */
    public Long write() throws Throwable {
        if (this.dataPipe == null) {
            MockerException e = new MockerException(MockerError.PARAMETER_ERROR, "Data pipe can not be null");
            log.error("can not read any data from pipe, cause the data pipe is null", e);
            throw e;
        }
        List<Map<String, Pair<AbstractDataType, Object>>> rows = this.dataPipe.read(10, TimeUnit.SECONDS);
        if (rows == null) {
            return null;
        } else if (rows.size() == 0) {
            return 0L;
        }
        return doWrite(rows);
    }

    abstract protected Long doWrite(List<Map<String, Pair<AbstractDataType, Object>>> rows) throws Throwable;

    /**
     * mockwriter用于向数据库或脚本文件中输出数据，目前的输出源有两个，一个是数据库，一个是脚本文件。reader和writer构成了一个生产者和消费者模型，
     * 即一个reader生产数据，多个writer输出数据，但是向数据库输出的writer和向文件输出的writer不能共用同一个数据通信”管道“，否则向数据库写出的
     * writer和向文本文件写出的writer会争用数据导致只有部分数据写出到数据库以及文本文件，解决方案就是不同输出源的writer绑定不同的数据管道，
     * 这里的groupId就是用于区分不同类型的writer的
     */
    abstract public String groupId();
}
