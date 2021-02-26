package com.oceanbase.tools.datamocker.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.core.write.output.MockerFile;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 文件管理器测试类
 *
 * @author yh263208
 * @date 2021-01-07 21:35
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class FileManagerTest extends MockerTestBase {

    private MockerFile manager = null;

    @Before
    public void initFileManager() throws IOException {
        manager = new MockerFile("test/mock/test.txt", true);
    }

    @Test
    public void testFileManager() throws IOException {
        String[] msgs = new String[] {
                "Hello, world\n",
                "Hello, OceanBase\n",
                "Hello, Ant Group\n"
        };
        for (String item : msgs) {
            long size = manager.write(item.getBytes(), 0, item.getBytes().length, false);
            Assert.assertEquals(item.getBytes().length, size);
        }
    }

    @Test
    public void testConcurrentFileManager() throws IOException, InterruptedException {
        int count = 100;
        for (int i = 0; i < count; i++) {
            int finalI = i;
            Thread t = new Thread(() -> {
                String index = finalI + "\n";
                try {
                    Thread.sleep(new Random().nextInt(1000));
                    manager.write(index.getBytes(), 0, index.getBytes().length, true);
                } catch (IOException | InterruptedException e) {
                    log.error("fail to write", e);
                }
            });
            t.start();
        }
        Thread.sleep(2000);
        FileInputStream input = new FileInputStream(manager.getFile());
        byte[] buffer = new byte[input.available()];
        input.read(buffer);
        input.close();
        String content = new String(buffer);
        String[] indexes = content.split("\n");
        int total = 0;
        for (String item : indexes) {
            total += Integer.parseInt(item);
        }
        Assert.assertEquals(total, (count - 1) * count / 2);
    }

    @After
    public void clearFileManager() throws IOException {
        manager.clear();
    }
}
