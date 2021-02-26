package com.oceanbase.tools.datamocker.core;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 这个对象用来封装待执行的任务，使用该对象可以描述任务之间的串行或并行关系。该数据封装对象本质上是多个栈（但是和普通栈不同的是
 * 该数据对象在"入栈"的时候是在末尾追加而不是在栈顶进行操作），栈顶指针使用一个数组来维护。一个栈中的任务都是需要串行执行的。
 *
 * @author yh263208
 * @date 2021-01-08 20:33
 * @since OBMOCKER_0.1.0_snapshot
 */
public class Dispatcher<T> {
    /**
     * 名称，可以代指分发器的名称也可以代指整个任务的名称
     */
    private String name;
    /**
     * 锁对象，该数据封装对象使用数组来维护多个栈的栈顶指针，由于数组不能动态地改变大小，因此每当程序要对栈顶指针
     * 数组进行维护时就获取该锁，防止多个线程并发地修改栈顶指针数组造成竞争条件
     */
    private ReentrantLock lock = new ReentrantLock();
    /**
     * 栈顶指针数据的长度，同时也可以用来描述当前并发任务的多少
     */
    private int concurrent;
    /**
     * 栈顶指针数组
     */
    private TopNode[] queuePointers;

    /**
     * 默认构造函数，此时初始化栈顶指针数组的长度为0。不推荐使用该构造函数，最好在构造之初就设定栈顶指针数组的大小，因为调整该指针数组大小
     * 是一个耗资源的行为
     */
    public Dispatcher(String name) {
        this.name = name;
        this.concurrent = 0;
    }

    /**
     * 构造函数，该构造函数传入栈顶指针数组的默认大小，程序根据传入的大小初始化栈顶指针数组
     */
    public Dispatcher(int concurrent, String name) {
        this.name = name;
        this.concurrent = concurrent;
        queuePointers = new TopNode[concurrent];
        for (int i = 0; i < concurrent; i++) {
            queuePointers[i] = new TopNode();
        }
    }

    /**
     * 返回分发器名称，其实也是mock数据任务的名称
     */
    public String name() {
        return this.name;
    }

    /**
     * 获取topNodes数组的长度
     *
     * @return 返回长度
     */
    public int count() {
        return this.concurrent;
    }

    /**
     * 获取某个具体的任务队列的长度
     *
     * @param index 目标任务队列的索引
     * @return 返回目标任务队列的长度
     */
    public int getTaskSize(int index) throws Exception {
        if (index >= this.concurrent || index < 0) {
            throw new Exception(String.format("index %d out of bound [0,%d)", index, this.concurrent));
        }
        TopNode topNode = this.queuePointers[index];
        return topNode.length;
    }

    /**
     * 此方法传入两个参数，通过这两个参数唯一定位到某个任务对象并范围，该方法不会对数据封装对象进行改动
     *
     * @param index       栈顶指针数组的索引号，方法根据该索引获取到对应位置的栈顶指针
     * @param columnIndex 栈索引，方法根据此索引找到栈的索引对应位置
     * @return 返回查询到的对象
     */
    public T getObj(int index, int columnIndex) throws Exception {
        if (index >= this.concurrent || index < 0) {
            throw new Exception(String.format("index %d out of bound [0,%d)", index, this.concurrent));
        }
        TopNode topNode = this.queuePointers[index];
        if (columnIndex >= topNode.length) {
            return null;
        }
        Node destNode;
        for (destNode = topNode.downNext; destNode != null; destNode = destNode.downNext) {
            if ((columnIndex--) == 0) {
                break;
            }
        }
        return destNode.getObj();
    }

    /**
     * 进行一次出栈操作
     *
     * @param index 对哪个任务队列进行"出栈"
     * @return 返回具体的任务对象
     */
    public T pop(int index) throws Exception {
        if (index >= this.concurrent || index < 0) {
            throw new Exception(String.format("index %d out of bound [0,%d)", index, this.concurrent));
        }
        TopNode topNode = this.queuePointers[index];
        if (topNode.length <= 0) {
            return null;
        }
        //由于需要对栈顶指针数组进行操作，因此首先要进行加锁处理
        T returnObj = null;
        topNode.writeLock.lock();
        try {
            //找到第一个栈大小不为0的栈，将栈顶指针传入到返回对象中准备返回
            if (topNode.length != 0) {
                Node destNode = topNode.downNext;
                if (destNode != null) {
                    returnObj = destNode.getObj();
                }
            }
            //以下代码进行实际的出栈操作，将旧的栈顶指针用新的栈顶指针替代
            Node next = topNode.downNext;
            if (next != null) {
                topNode.downNext = next.downNext;
                next.downNext = null;
            }
            if (topNode.length > 0) {
                //栈大小减一
                topNode.length--;
            }
        } finally {
            topNode.writeLock.unlock();
        }
        return returnObj;
    }

    /**
     * 使用该方法进行任务的发布，index表明需要插入到哪一个任务栈中，并且追加到该任务栈的末尾。
     *
     * @param index 栈顶指针数组索引，表明当前任务想要插入到哪一个栈中。该索引的取值范围是0至栈顶指针数组的大小，若取最大值则栈顶
     *              指针数组进行扩容操作。
     * @param obj   待插入的任务对象
     */
    public void setObj(int index, T obj) throws Exception {
        if (obj == null) {
            return;
        }
        //栈顶指针索引在数组的范围内，直接进行插入操作
        if (index < this.concurrent) {
            TopNode topNode = queuePointers[index];
            if (topNode == null) {
                throw new Exception(String.format("index %d is invaild", index));
            }
            //对当前栈顶指针进行加锁
            topNode.writeLock.lock();
            try {
                //找到该任务栈的栈尾
                Node lastNode = topNode.downNext;
                if (lastNode != null) {
                    for (; lastNode.downNext != null; lastNode = lastNode.downNext) {}
                }
                //构造出新的任务节点并且将新的任务节点尾插到任务栈的末尾
                Node newNode = new Node(obj);
                if (lastNode == null) {
                    topNode.downNext = newNode;
                } else {
                    lastNode.downNext = newNode;
                }
                lastNode = newNode;
                lastNode.downNext = null;
                topNode.length++;
            } finally {
                topNode.writeLock.unlock();
            }
        } else if (index == this.concurrent) {
            //此处要进行栈顶指针数组扩容，新的大小为原来大小加一
            lock.lock();
            try {
                if (index == this.concurrent) {
                    this.concurrent++;
                    TopNode[] newTopNodes = new TopNode[this.concurrent];
                    for (int i = 0; i < this.concurrent - 1; i++) {
                        newTopNodes[i] = this.queuePointers[i];
                    }
                    newTopNodes[this.concurrent - 1] = new TopNode();
                    this.queuePointers = newTopNodes;
                }
            } finally {
                lock.unlock();
            }
            this.setObj(index, obj);
        } else {
            //非法的索引
            throw new Exception(String.format("index %d out of bound", index));
        }
    }

    /**
     * 任务节点，该节点用于封装任务对象，拥有读锁和写锁同时拥有横向和纵向两个指针分别用来描述与其他任务对象之间的异步和同步执行关系
     *
     * @author yh263208
     * @date 2021-01-08 20:32
     * @since OBMOCKER_0.1.0_snapshot
     */
    class Node {
        private T obj = null;
        public Node downNext = null;
        public ReentrantLock readLock;
        public ReentrantLock writeLock;

        public Node(T obj) {
            this.obj = obj;
            readLock = new ReentrantLock();
            writeLock = new ReentrantLock();
        }

        public T getObj() {
            return obj;
        }
    }
}

/**
 * 任务栈的栈顶节点，为管理节点，不承担任务的封装工作
 *
 * @author yh263208
 * @date 2021-01-08 20:32
 * @since OBMOCKER_0.1.0_snapshot
 */
class TopNode {
    public Dispatcher.Node downNext = null;
    public ReentrantLock readLock;
    public ReentrantLock writeLock;
    public int length = 0;

    public TopNode() {
        readLock = new ReentrantLock();
        writeLock = new ReentrantLock();
    }
}