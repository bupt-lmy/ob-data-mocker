package com.oceanbase.tools.datamocker.core.write.output;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.oceanbase.tools.datamocker.model.enums.ScriptType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.extern.slf4j.Slf4j;

/**
 * mock数据的文件管理器，用于文件的增删改查和数据的写入
 *
 * @author yh263208
 * @date 2021-01-08 20:48
 * @since OBMOCKER-snasphot-0.1.0
 */
@Slf4j
public class MockerFile {
    /**
     * 操作文件
     */
    private final File file;
    /**
     * 文件输出流
     */
    private FileOutputStream output;
    /**
     * 脚本类型
     */
    private ScriptType scriptType;

    public MockerFile(String fileName, ScriptType scriptType) throws IOException {
        file = new File(fileName);
        if (file.exists()) {
            if (!file.delete()) {
                throw new MockerException(MockerError.OPERATION_FAILURE, "Fail to delete a file \"%s\"");
            }
        }
        create(file);
        output = new FileOutputStream(file);
        this.scriptType = scriptType;
    }

    public MockerFile(String fileName, ScriptType scriptType, Boolean truncate) throws IOException {
        file = new File(fileName);
        if (file.exists()) {
            if (truncate) {
                if (!file.delete()) {
                    throw new IOException(String.format("fail to truncate file \"%s\"", fileName));
                }
                create(file);
            }
        } else {
            create(file);
        }
        output = new FileOutputStream(file, true);
        this.scriptType = scriptType;
    }

    /**
     * 关闭文件管理器
     */
    public void close() {
        try {
            this.output.close();
            log.info("file manager will be shut down, file manager has been closed");
        } catch (IOException e) {
            log.error("fail to close a out put stream", e);
        }
    }

    /**
     * 创建一个文件
     *
     * @param file 需要创建的文件
     * @throws IOException 文件操作可能会抛出异常
     */
    private void create(File file) throws IOException {
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new MockerException(MockerError.OPERATION_FAILURE,
                        String.format("Fail to create dir \"%s\"", file.getParent()));
            }
        }
        file.createNewFile();
    }

    /**
     * 写入数据
     *
     * @param bytes 缓冲，要写入的数据放在此处
     * @return 返回写入的字节数
     * @throws IOException 文件操作可能会导致异常
     */
    synchronized public long write(byte[] bytes, int offset, int length, boolean immediateFlush) throws IOException {
        if (this.output == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "File output stream can not be null");
        }
        this.output.write(bytes, offset, length);
        if (immediateFlush) {
            this.output.flush();
        }
        return length;
    }

    /**
     * 获取文件对象
     *
     * @return 返回文件对象
     */
    public File getFile() {
        return this.file;
    }

    /**
     * 清理一个文件管理器，将文件管理器关联的文件删除
     *
     * @return 返回清理结果
     * @throws IOException 可能会抛出异常
     */
    public synchronized boolean clear() throws IOException {
        this.output.close();
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }

    public ScriptType getScriptType() {
        return this.scriptType;
    }
}
