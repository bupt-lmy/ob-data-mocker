package com.oceanbase.tools.datamocker.core.write.output;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.oceanbase.tools.datamocker.model.enums.ScriptType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.extern.slf4j.Slf4j;

/**
 * File manager for mock data, used for file addition, deletion, modification, and data writing
 *
 * @author yh263208
 * @date 2021-01-08 20:48
 * @since OBMOCKER-snasphot-0.1.0
 */
@Slf4j
public class MockerFile {
    /**
     * Original file
     */
    private final File file;
    /**
     * Raw file output stream
     */
    private final FileOutputStream output;
    /**
     * Script Type
     */
    private final ScriptType scriptType;

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

    public void close() {
        try {
            this.output.close();
            log.info("File manager has been successfully closed");
        } catch (IOException e) {
            log.error("File manager has been closed failed", e);
        }
    }

    /**
     * Create a file
     *
     * @param file Files to be created
     * @throws IOException File operations may throw exceptions
     */
    private void create(File file) throws IOException {
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new MockerException(MockerError.OPERATION_FAILURE,
                        String.format("Fail to create dir \"%s\"", file.getParent()));
            }
        }
        if (!file.createNewFile()) {
            throw new MockerException(MockerError.OPERATION_FAILURE,
                    String.format("Fail to create file \"%s\"", file.getName()));
        }
    }

    /**
     * Write data to file
     *
     * @param bytes Buffer, the data to be written is placed here
     * @return Returns the number of bytes written
     * @throws IOException File operations may cause exceptions
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

    public File getFile() {
        return this.file;
    }

    /**
     * Clean up a file manager and delete files associated with the file manager
     *
     * @return Return cleanup results
     * @throws IOException May throw an exception when clear files
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
