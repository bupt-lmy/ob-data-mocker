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
import java.io.IOException;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for {@link SqlScriptOutput}
 *
 * @author yh263208
 * @date 2023-12-05 11:49
 * @since ODC_release_4.2.3
 */
public class SqlScriptOutputTest {

    private static final String DEST_DIR = "test/emp";

    @Before
    public void setUp() throws IOException {
        FileUtils.forceMkdir(new File(DEST_DIR));
    }

    @After
    public void clear() throws IOException {
        FileUtils.deleteDirectory(new File(DEST_DIR));
    }

    @Test
    public void getOutputStream_lessThanMaxBytes_getSucceed() throws IOException {
        SqlScriptOutput output = getOutput();
        String target = "Hello,world";
        IOUtils.write(target.getBytes(), output.getOutputStream());
        Assert.assertEquals(target.getBytes().length,
                new FileInputStream(new File(DEST_DIR).listFiles()[0]).available());
    }

    @Test
    public void getOutputStream_moreThanMaxBytes_MultiFileGenerated() throws IOException {
        SqlScriptOutput output = getOutput();
        String target = "HelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHello";
        IOUtils.write(target.getBytes(), output.getOutputStream());
        IOUtils.write(target.getBytes(), output.getOutputStream());
        File targetFile = new File(DEST_DIR);
        Assert.assertEquals(2, Objects.requireNonNull(targetFile.listFiles()).length);
    }

    @Test
    public void toZip_moreThanMaxBytes_MultiFileGenerated() throws IOException {
        SqlScriptOutput output = getOutput();
        String target = "HelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHello";
        IOUtils.write(target.getBytes(), output.getOutputStream());
        IOUtils.write(target.getBytes(), output.getOutputStream());
        File zipFile = new File(DEST_DIR, "emp.zip");
        SqlScriptOutput.toZip(zipFile, new File(DEST_DIR));
        Assert.assertTrue(zipFile.exists());
    }

    private SqlScriptOutput getOutput() throws IOException {
        return new SqlScriptOutput(new File(DEST_DIR), "emp", 50L);
    }

}
