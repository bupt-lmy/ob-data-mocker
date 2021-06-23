package com.oceanbase.tools.datamocker.core;

import java.util.concurrent.locks.ReentrantLock;

import lombok.Getter;
import org.apache.commons.lang.Validate;

/**
 * This object is used to encapsulate the tasks to be executed, and use this object to describe the
 * serial or parallel relationship between tasks. The data encapsulation object is essentially
 * multiple stacks (but different from the normal stack is that the data object is appended at the
 * end rather than operated on the top of the stack when "stacked"), and the stack top pointer is
 * maintained by an array. The tasks in a stack need to be executed serially.
 *
 * @author yh263208
 * @date 2021-01-08 20:33
 * @since OBMOCKER_0.1.0_snapshot
 */
public class Dispatcher<T> {
    @Getter
    private final String taskId;
    /**
     * The name can refer to the name of the distributor or the name of the entire task
     */
    @Getter
    private final String name;
    /**
     * The lock object, the data encapsulation object uses an array to maintain the top pointers of
     * multiple stacks. Since the array cannot be dynamically changed in size, the lock is acquired
     * whenever the program wants to maintain the array of top pointers to prevent multiple threads from
     * concurrency Modifying the array of pointers at the top of the stack causes a race condition
     */
    private final ReentrantLock lock = new ReentrantLock();
    /**
     * The length of the pointer data on the top of the stack can also be used to describe the current
     * number of concurrent tasks
     */
    @Getter
    private int concurrent;
    /**
     * Stack top pointer array
     */
    private TopNode<T>[] queuePointers;

    /**
     * The default constructor, at this time, initialize the length of the pointer array at the top of
     * the stack to 0. It is not recommended to use this constructor. It is best to set the size of the
     * pointer array on the top of the stack at the beginning of the construction, because adjusting the
     * size of the pointer array is a resource-consuming behavior
     *
     * @param name dispatcher's name or task name
     * @param taskId task id
     */
    public Dispatcher(String name, String taskId) {
        Validate.notEmpty(taskId, "TaskId can not be null for Dispatcher");
        this.name = name;
        this.taskId = taskId;
        this.concurrent = 0;
    }

    /**
     * Constructor, the constructor passes in the default size of the stack top pointer array, and the
     * program initializes the stack top pointer array according to the incoming size
     *
     * @param concurrent initial size of this dispatcher
     * @param name dispatcher's name or task name
     * @param taskId task id
     */
    public Dispatcher(int concurrent, String name, String taskId) {
        Validate.notEmpty(taskId, "TaskId can not be null for Dispatcher");
        Validate.isTrue(concurrent > 0, "Concurrent can not be negative");
        this.name = name;
        this.taskId = taskId;
        this.concurrent = concurrent;
        queuePointers = new TopNode[concurrent];
        for (int i = 0; i < concurrent; i++) {
            queuePointers[i] = new TopNode<>();
        }
    }

    /**
     * Returns the number of all objects in the distributor
     *
     * @return Return specific quantity
     */
    public int totalCount() {
        int queueSize = getConcurrent();
        int returnVal = 0;
        for (int i = 0; i < queueSize; i++) {
            returnVal += getTaskSize(i);
        }
        return returnVal;
    }

    /**
     * Get the length of a specific task queue
     *
     * @param index Index of the target task queue
     * @return Returns the length of the target task queue
     */
    public int getTaskSize(int index) {
        if (index >= this.concurrent || index < 0) {
            throw new IllegalArgumentException(String.format("index %d out of bound [0,%d)", index, this.concurrent));
        }
        TopNode<T> topNode = this.queuePointers[index];
        return topNode.length;
    }

    /**
     * This method passes in two parameters, through these two parameters to uniquely locate a task
     * object and scope, this method will not change the data package object
     *
     * @param index The index number of the stack top pointer array, the method obtains the
     *        corresponding position of the stack top pointer according to the index
     * @param columnIndex Stack index, the method finds the corresponding position of the stack index
     *        according to this index
     * @return Return the queried object
     */
    public T getObj(int index, int columnIndex) {
        if (index >= this.concurrent || index < 0) {
            throw new IllegalArgumentException(String.format("index %d out of bound [0,%d)", index, this.concurrent));
        }
        TopNode<T> topNode = this.queuePointers[index];
        if (columnIndex >= topNode.length) {
            return null;
        }
        Node destNode;
        for (destNode = topNode.downNext; destNode != null; destNode = destNode.downNext) {
            if ((columnIndex--) == 0) {
                break;
            }
        }
        if (destNode == null) {
            throw new NullPointerException("Can't find data node");
        }
        return destNode.getObj();
    }

    /**
     * Perform a pop operation
     *
     * @param index Which task queue to "pop"
     * @return Return the specific task object
     */
    public T pop(int index) {
        if (index >= this.concurrent || index < 0) {
            throw new IllegalArgumentException(String.format("index %d out of bound [0,%d)", index, this.concurrent));
        }
        TopNode<T> topNode = this.queuePointers[index];
        if (topNode.length <= 0) {
            return null;
        }
        // Since the array of pointers on the top of the stack needs to be operated, the lock processing
        // must be performed first
        T returnObj = null;
        topNode.writeLock.lock();
        try {
            // Find the first stack whose size is not 0, and pass the top pointer of the stack to the return
            // object to prepare to return
            if (topNode.length != 0) {
                Node destNode = topNode.downNext;
                if (destNode != null) {
                    returnObj = destNode.getObj();
                }
            }
            // The following code performs the actual pop operation, replacing the old stack top pointer with
            // the new stack top pointer
            Node next = topNode.downNext;
            if (next != null) {
                topNode.downNext = next.downNext;
                next.downNext = null;
            }
            if (topNode.length > 0) {
                // Stack size minus one
                topNode.length--;
            }
        } finally {
            topNode.writeLock.unlock();
        }
        return returnObj;
    }

    /**
     * Use this method to publish tasks, index indicates which task stack needs to be inserted into, and
     * appends to the end of the task stack.
     *
     * @param index The index of the pointer array at the top of the stack indicates which stack the
     *        current task wants to be inserted into. The value range of the index is from 0 to the size
     *        of the stack top pointer array. If the maximum value is taken, the stack top pointer array
     *        performs an expansion operation.
     * @param obj Task object to be inserted
     */
    public void setObj(int index, T obj) {
        if (obj == null) {
            return;
        }
        // The index of the top pointer of the stack is within the range of the array, and the insertion
        // operation is performed directly
        if (index < this.concurrent) {
            TopNode<T> topNode = queuePointers[index];
            if (topNode == null) {
                throw new IllegalArgumentException(String.format("index %d is invaild", index));
            }
            // Lock the current stack pointer
            topNode.writeLock.lock();
            try {
                // Find the end of the task stack
                Node lastNode = topNode.downNext;
                if (lastNode != null) {
                    for (; lastNode.downNext != null; lastNode = lastNode.downNext) {
                    }
                }
                // Construct a new task node and insert the end of the new task node to the end of the task stack
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
            // Here to expand the stack top pointer array, the new size is the original size plus one
            lock.lock();
            try {
                if (index == this.concurrent) {
                    this.concurrent++;
                    TopNode<T>[] newTopNodes = new TopNode[this.concurrent];
                    for (int i = 0; i < this.concurrent - 1; i++) {
                        newTopNodes[i] = this.queuePointers[i];
                    }
                    newTopNodes[this.concurrent - 1] = new TopNode<>();
                    this.queuePointers = newTopNodes;
                }
            } finally {
                lock.unlock();
            }
            this.setObj(index, obj);
        } else {
            // Illegal index
            throw new IllegalArgumentException(String.format("index %d out of bound", index));
        }
    }

    /**
     * The task node, which is used to encapsulate task objects, has a read lock and a write lock, and
     * both horizontal and vertical pointers are used to describe the asynchronous and synchronous
     * execution relationship with other task objects.
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
 * The top node of the task stack is the management node and does not undertake task encapsulation
 * work
 *
 * @author yh263208
 * @date 2021-01-08 20:32
 * @since OBMOCKER_0.1.0_snapshot
 */
class TopNode<T> {
    public Dispatcher<T>.Node downNext = null;
    public ReentrantLock readLock;
    public ReentrantLock writeLock;
    public int length = 0;

    public TopNode() {
        readLock = new ReentrantLock();
        writeLock = new ReentrantLock();
    }
}
