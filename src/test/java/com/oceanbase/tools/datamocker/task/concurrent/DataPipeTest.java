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
package com.oceanbase.tools.datamocker.task.concurrent;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.oceanbase.tools.datamocker.core.task.AbstractDataPipe;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.Pair;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test object for data pipe
 *
 * @author yh263208
 * @date 2021-06-26 14:26
 */
public class DataPipeTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 5, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());

    @Test
    public void testIllegalTimeoutForWrite() throws InterruptedException {
        StringDataPipe dataPipe = new StringDataPipe(1);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Timeout for pipeline write can not be negative");
        dataPipe.write("Hello", -100, TimeUnit.SECONDS);
    }

    @Test
    public void testIllegalTimeoutForRead() throws InterruptedException {
        StringDataPipe dataPipe = new StringDataPipe(1);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Timeout for pipeline read can not be negative");
        dataPipe.read(-100, TimeUnit.SECONDS);
    }

    @Test
    public void testClosedDataPipeWriteIn() throws InterruptedException {
        StringDataPipe dataPipe = new StringDataPipe(1);
        dataPipe.close();
        Assert.assertTrue(dataPipe.isClosed());
        thrown.expectMessage("Data pipe has been closed");
        thrown.expect(MockerException.class);
        dataPipe.write("Hello", 3, TimeUnit.SECONDS);
    }

    @Test
    public void testInputNullToDataPipe() throws InterruptedException {
        StringDataPipe dataPipe = new StringDataPipe(1);
        thrown.expectMessage("Input element can not be null");
        thrown.expect(NullPointerException.class);
        dataPipe.write(null, 3, TimeUnit.SECONDS);
    }

    @Test
    public void testTimeoutForWriteOperation() throws InterruptedException {
        StringDataPipe dataPipe = new StringDataPipe(1);
        dataPipe.write("Hello", 3, TimeUnit.SECONDS);
        long timestamp = System.currentTimeMillis();
        dataPipe.write("Hello", 3, TimeUnit.SECONDS);
        Assert.assertTrue(System.currentTimeMillis() - timestamp >= 3000);
    }

    @Test
    public void testTimeoutForReadOperation() throws InterruptedException {
        StringDataPipe dataPipe = new StringDataPipe(1);
        long timestamp = System.currentTimeMillis();
        dataPipe.read(3, TimeUnit.SECONDS);
        Assert.assertTrue(System.currentTimeMillis() - timestamp >= 3000);
    }

    @Test
    public void testTimeoutForWriteIn() throws InterruptedException, ExecutionException {
        StringDataPipe dataPipe = new StringDataPipe(1);
        dataPipe.write("hello");
        Callable<Long> writeTask1 = () -> {
            long start = System.currentTimeMillis();
            dataPipe.write("Hello,world", 3, TimeUnit.SECONDS);
            return System.currentTimeMillis() - start;
        };
        Future<Long> future1 = executor.submit(writeTask1);
        Callable<Long> writeTask2 = () -> {
            long start = System.currentTimeMillis();
            dataPipe.write("Hello,world", 3, TimeUnit.SECONDS);
            return System.currentTimeMillis() - start;
        };
        Future<Long> future2 = executor.submit(writeTask2);
        Thread.sleep(500);
        Callable<Boolean> readTask = () -> "hello".equals(dataPipe.read());
        Future<Boolean> readFuture = executor.submit(readTask);
        Assert.assertTrue(
                (future1.get() >= 3000 && future2.get() < 3000) || (future2.get() >= 3000 && future1.get() < 3000));
        Assert.assertTrue(readFuture.get());
    }

    @Test
    public void testTimeoutForReadOut() throws InterruptedException, ExecutionException {
        StringDataPipe dataPipe = new StringDataPipe(1);
        Callable<Pair<Long, String>> readTask1 = () -> {
            long start = System.currentTimeMillis();
            String value = dataPipe.read(3, TimeUnit.SECONDS);
            return new Pair<>(System.currentTimeMillis() - start, value);
        };
        Future<Pair<Long, String>> future1 = executor.submit(readTask1);
        Callable<Pair<Long, String>> readTask2 = () -> {
            long start = System.currentTimeMillis();
            String value = dataPipe.read(3, TimeUnit.SECONDS);
            return new Pair<>(System.currentTimeMillis() - start, value);
        };
        Future<Pair<Long, String>> future2 = executor.submit(readTask2);
        Thread.sleep(500);
        Callable<Long> writeTask = () -> {
            long start = System.currentTimeMillis();
            dataPipe.write("hello");
            return System.currentTimeMillis() - start;
        };
        Future<Long> future3 = executor.submit(writeTask);
        Assert.assertTrue(future3.get() <= 10);
        Pair<Long, String> pair1 = future1.get();
        Pair<Long, String> pair2 = future2.get();
        if (pair1.getValue() != null) {
            Assert.assertTrue(pair1.getKey() > 500 && pair1.getKey() < 550);
        }
        if (pair2.getValue() != null) {
            Assert.assertTrue(pair2.getKey() > 500 && pair2.getKey() < 550);
        }
    }
}


/**
 * Data pipe for string value
 *
 * @author yh263208
 * @date 2021-06-26 16:03
 * @since OB_MOCKER_snapshot_0.1.2
 */
class StringDataPipe extends AbstractDataPipe<String> {

    private final List<String> container = new LinkedList<>();

    public StringDataPipe(int maxRetained) {
        super(maxRetained);
    }

    @Override
    public void doWrite(String element, long timeout, TimeUnit timeUnit) {
        container.add(element);
    }

    @Override
    public String doRead(long timeout, TimeUnit timeUnit) {
        String returnVal = null;
        synchronized (this.container) {
            Iterator<String> iter = container.iterator();
            if (iter.hasNext()) {
                returnVal = iter.next();
                iter.remove();
            }
        }
        return returnVal;
    }

    @Override
    public long size() {
        return container.size();
    }
}
