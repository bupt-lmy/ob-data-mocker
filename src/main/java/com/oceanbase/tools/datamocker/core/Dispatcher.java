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
package com.oceanbase.tools.datamocker.core;

import java.util.concurrent.locks.ReentrantLock;

import lombok.Getter;
import lombok.NonNull;
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
    private final String logDir;
    private final ReentrantLock lock = new ReentrantLock();
    /**
     * The length of the pointer data on the top of the stack can also be used to describe the current
     * number of concurrent tasks
     */
    @Getter
    private int width;
    private TopNode<T>[] queuePointers;

    public Dispatcher(@NonNull String logDir) {
        this.width = 0;
        this.logDir = logDir;
    }

    public Dispatcher(int width, @NonNull String logDir) {
        Validate.isTrue(width > 0, "Width can not be negative");
        this.logDir = logDir;
        this.width = width;
        queuePointers = new TopNode[width];
        for (int i = 0; i < width; i++) {
            queuePointers[i] = new TopNode<>();
        }
    }

    public int getTotalCount() {
        int queueSize = getWidth();
        int returnVal = 0;
        for (int i = 0; i < queueSize; i++) {
            returnVal += getTaskSize(i);
        }
        return returnVal;
    }

    public int getTaskSize(int index) {
        if (index >= this.width || index < 0) {
            throw new IllegalArgumentException(String.format("Index %d out of bound [0,%d)", index, this.width));
        }
        TopNode<T> topNode = this.queuePointers[index];
        return topNode.length;
    }

    public T getObj(int index, int columnIndex) {
        if (index >= this.width || index < 0) {
            throw new IllegalArgumentException(String.format("Index %d out of bound [0,%d)", index, this.width));
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

    public T pop(int index) {
        if (index >= this.width || index < 0) {
            throw new IllegalArgumentException(String.format("Index %d out of bound [0,%d)", index, this.width));
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

    public void setObj(int index, T obj) {
        if (obj == null) {
            return;
        }
        // The index of the top pointer of the stack is within the range of the array, and the insertion
        // operation is performed directly
        if (index < this.width) {
            TopNode<T> topNode = queuePointers[index];
            if (topNode == null) {
                throw new IllegalArgumentException(String.format("Index %d is invaild", index));
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
        } else if (index == this.width) {
            // Here to expand the stack top pointer array, the new size is the original size plus one
            lock.lock();
            try {
                if (index == this.width) {
                    this.width++;
                    TopNode<T>[] newTopNodes = new TopNode[this.width];
                    for (int i = 0; i < this.width - 1; i++) {
                        newTopNodes[i] = this.queuePointers[i];
                    }
                    newTopNodes[this.width - 1] = new TopNode<>();
                    this.queuePointers = newTopNodes;
                }
            } finally {
                lock.unlock();
            }
            this.setObj(index, obj);
        } else {
            // Illegal index
            throw new IllegalArgumentException(String.format("Index %d out of bound", index));
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
