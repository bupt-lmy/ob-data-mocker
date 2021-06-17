package com.oceanbase.tools.datamocker.util;

import java.util.HashSet;
import java.util.Set;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * 工具类，用于判断某个值是否重复出现过
 *
 * @author yh263208
 * @date 2021-01-10 22:56
 * @since OBMOCKER_0.1.0_snapshot
 */
public class DuplicatedJudger {
    /**
     * 集合，用于较少数据量时是否重复的判断
     */
    private Set set;
    /**
     * 位图，用于较多数据量时数据是否重复的判断
     */
    private BitMap bitMap;
    /**
     * 能够承载的最大数量
     */
    private final int maxCount;
    /**
     * 当前的游标
     */
    private int cursor;

    public DuplicatedJudger(int count) {
        if (count <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Count for DuplicatedJudger can not be equal to or smaleer than zero");
        }
        if (count < 10000) {
            this.set = new HashSet();
        } else {
            this.bitMap = new BitMap(count);
        }
        this.maxCount = count;
        this.cursor = 0;
    }

    public boolean contains(Object obj) {
        if (this.set != null) {
            return this.set.contains(obj);
        } else {
            return this.bitMap.contains(obj);
        }
    }

    public boolean add(Object obj) {
        if (++this.cursor > this.maxCount) {
            throw new MockerException(MockerError.OPERATION_FAILURE,
                    String.format("The max count for DuplicatedJudger is %d, can not add more", this.maxCount));
        }
        if (this.set != null) {
            return this.set.add(obj);
        } else {
            return this.bitMap.add(obj);
        }
    }

    public void clear() {
        this.cursor = 0;
        if (this.set != null) {
            this.set.clear();
        } else {
            this.bitMap.clear();
        }
    }
}
