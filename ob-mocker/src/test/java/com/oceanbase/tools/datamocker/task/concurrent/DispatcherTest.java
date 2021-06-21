package com.oceanbase.tools.datamocker.task.concurrent;

import java.util.Arrays;
import java.util.List;

import com.oceanbase.tools.datamocker.MockerTestBase;
import com.oceanbase.tools.datamocker.core.Dispatcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Distributor test class
 *
 * @author yh263208
 * @date 2021-01-08 17:44
 * @since OBMOCKER_0.1.0_snapshot
 */
public class DispatcherTest extends MockerTestBase {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private List<List<String>> getQueue() {
        List<String> queue1 = Arrays.asList("1", "4", "7", "12");
        List<String> queue2 = Arrays.asList("2", "5", "8", "13", "17");
        List<String> queue3 = Arrays.asList("3", "6", "9");
        return Arrays.asList(queue1, queue2, queue3);
    }

    @Test
    public void testDispatcher() throws Exception {
        List<List<String>> queue = getQueue();
        Dispatcher<String> dispatcher = new Dispatcher<>(queue.size(), null, "test_task_id");
        for (int i = 0; i < queue.size(); i++) {
            List<String> list = queue.get(i);
            for (int j = 0; j < list.size(); j++) {
                dispatcher.setObj(i, list.get(j));
            }
        }
        for (int i = 0; i < dispatcher.count(); i++) {
            for (int j = 0; j < dispatcher.getTaskSize(i); j++) {
                String obj = dispatcher.getObj(i, 0);
                String obj1 = dispatcher.pop(i);
                Assert.assertEquals(obj, obj1);
                String returnValue = queue.get(i).get(j);
                Assert.assertEquals(returnValue, obj);
            }
        }
    }

    @Test
    public void testDefaultDispatcher() throws Exception {
        List<List<String>> queue = getQueue();
        Dispatcher<String> dispatcher = new Dispatcher<>(null, "test_task_id");
        for (int i = 0; i < queue.size(); i++) {
            List<String> list = queue.get(i);
            for (int j = 0; j < list.size(); j++) {
                dispatcher.setObj(i, list.get(j));
            }
        }
        for (int i = 0; i < dispatcher.count(); i++) {
            for (int j = 0; j < dispatcher.getTaskSize(i); j++) {
                String obj = dispatcher.getObj(i, 0);
                String obj1 = dispatcher.pop(i);
                Assert.assertEquals(obj, obj1);
                String returnValue = queue.get(i).get(j);
                Assert.assertEquals(returnValue, obj);
            }
        }
    }

    @Test
    public void testDispatcherWithIllegalIndex() throws Exception {
        Dispatcher<String> dispatcher = new Dispatcher<>(3, null, "test_task_id");
        int index = 3;
        thrown.expect(Exception.class);
        thrown.expectMessage(String.format("index %d out of bound [0,%d)", index, 3));
        dispatcher.getTaskSize(index);
    }

    @Test
    public void testDispatcherWithIllegalIndex1() throws Exception {
        Dispatcher<String> dispatcher = new Dispatcher<>(3, null, "test_task_id");
        int index = 3;
        thrown.expect(Exception.class);
        thrown.expectMessage(String.format("index %d out of bound [0,%d)", index, 3));
        dispatcher.getObj(index, 0);
    }

    @Test
    public void testDispatcherWithIllegalIndex2() throws Exception {
        Dispatcher<String> dispatcher = new Dispatcher<>(3, null, "test_task_id");
        dispatcher.setObj(0, "Hello");
        Assert.assertNull(dispatcher.getObj(0, 1));
    }

    @Test
    public void testDispatcherWithIllegalIndexForPop() throws Exception {
        Dispatcher<String> dispatcher = new Dispatcher<>(3, null, "test_task_id");
        int index = 3;
        thrown.expect(Exception.class);
        thrown.expectMessage(String.format("index %d out of bound [0,%d)", index, 3));
        dispatcher.pop(index);
    }

    @Test
    public void testDispatcherWithIllegalIndexForPop1() throws Exception {
        Dispatcher<String> dispatcher = new Dispatcher<>(3, null, "test_task_id");
        Assert.assertNull(dispatcher.pop(0));
    }

    @Test
    public void testDispatcherWithnullSet() throws Exception {
        Dispatcher<String> dispatcher = new Dispatcher<>(3, null, "test_task_id");
        dispatcher.setObj(0, null);
    }

    @Test
    public void testDispatcherWithIllegalSet() throws Exception {
        Dispatcher<String> dispatcher = new Dispatcher<>(3, null, "test_task_id");
        thrown.expect(Exception.class);
        thrown.expectMessage(String.format("index %d out of bound", 4));
        dispatcher.setObj(4, "null");
    }
}

