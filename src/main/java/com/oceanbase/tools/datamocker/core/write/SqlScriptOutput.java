/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oceanbase.tools.datamocker.core.write;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * {@link SqlScriptOutput}
 *
 * @author yh263208
 * @date 2023-12-05 10:14
 * @since ODC_release_4.2.3
 */
@Slf4j
public class SqlScriptOutput implements AutoCloseable {
    @Getter
    private final File outputDir;
    private final Long maxSingleFileInByte;
    private final String tableName;
    private Integer counter = 0;
    private FileOutputStreamWrapper currentOutput;

    public SqlScriptOutput(@NonNull File outputDir,
            @NonNull String tableName,
            @NonNull Long maxSingleFileInBytes) throws IOException {
        Validate.isTrue(maxSingleFileInBytes > 0);
        Validate.isTrue(outputDir.isDirectory() && outputDir.exists());
        this.tableName = tableName;
        this.outputDir = outputDir;
        this.maxSingleFileInByte = maxSingleFileInBytes;
        this.currentOutput = createNewOutputStream();
    }

    public OutputStream getOutputStream() {
        if (this.currentOutput.getTotalWriteBytes() >= this.maxSingleFileInByte) {
            try {
                FileOutputStreamWrapper old = this.currentOutput;
                this.currentOutput = createNewOutputStream();
                old.close();
            } catch (Exception e) {
                log.warn("Failed to create a new file, message={}", e.getMessage());
            }
        }
        return this.currentOutput;
    }

    public static void toZip(@NonNull File target, @NonNull File workingDir) throws IOException {
        File parentDir = target.getParentFile();
        if (!parentDir.exists()) {
            FileUtils.forceMkdir(parentDir);
        }
        File[] files = workingDir.listFiles();
        if (files == null) {
            throw new IllegalStateException("Nothing to zip, " + workingDir.getAbsolutePath());
        }
        try (FileOutputStream outputStream = new FileOutputStream(target);
                ArchiveOutputStream out = new ZipArchiveOutputStream(outputStream)) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File[] subFiles = file.listFiles();
                    if (subFiles == null || Arrays.stream(subFiles).noneMatch(
                            f -> f.isFile() && StringUtils.endsWithIgnoreCase(f.getName(), ".sql"))) {
                        continue;
                    }
                    String dirName = file.getName();
                    out.putArchiveEntry(new ZipArchiveEntry(dirName + "/"));
                    out.closeArchiveEntry();
                    for (File subFile : subFiles) {
                        if (!subFile.isFile() || StringUtils.equalsIgnoreCase(subFile.getName(), ".sql")) {
                            continue;
                        }
                        out.putArchiveEntry(new ZipArchiveEntry(dirName + "/" + subFile.getName()));
                        try (InputStream inputStream = new FileInputStream(subFile)) {
                            IOUtils.copy(inputStream, out);
                        }
                        out.closeArchiveEntry();
                    }
                } else if (file.isFile() && StringUtils.endsWithIgnoreCase(file.getName(), ".sql")) {
                    out.putArchiveEntry(new ZipArchiveEntry(file.getName()));
                    try (InputStream inputStream = new FileInputStream(file)) {
                        IOUtils.copy(inputStream, out);
                    }
                    out.closeArchiveEntry();
                }
            }
            out.flush();
        }
        if (!target.exists()) {
            throw new IllegalStateException("Failed to create a zip file");
        }
    }

    @Override
    public void close() throws Exception {
        this.currentOutput.close();
    }

    private FileOutputStreamWrapper createNewOutputStream() throws IOException {
        String fileName = this.tableName + "_" + (this.counter++) + ".sql";
        File target = new File(outputDir, fileName);
        if (!target.createNewFile()) {
            throw new IllegalStateException("Fail to create file, " + target.getName());
        }
        return new FileOutputStreamWrapper(target, true);
    }

    @Getter
    private static class FileOutputStreamWrapper extends FileOutputStream {

        private Long totalWriteBytes = 0L;

        public FileOutputStreamWrapper(File file, boolean append) throws FileNotFoundException {
            super(file, append);
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            this.totalWriteBytes++;
        }

        @Override
        public void write(byte b[]) throws IOException {
            super.write(b);
            this.totalWriteBytes += b.length;
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            super.write(b, off, len);
            this.totalWriteBytes += len;
        }
    }

}
