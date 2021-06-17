package com.oceanbase.tools.datamocker.constraint.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oceanbase.tools.datamocker.constraint.AbstractConstraint;
import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.DuplicatedJudger;
import com.oceanbase.tools.datamocker.util.Pair;
import lombok.extern.slf4j.Slf4j;

/**
 * 唯一约束校验类
 *
 * @author yh263208
 * @date 2020-12-31 20:47
 * @since OBMOCKER_snapshot_0.1.0
 */
@Slf4j
public class UniqueConstraint extends AbstractConstraint {
    /**
     * 重复定义工具类
     */
    private final DuplicatedJudger judger;
    /**
     * 唯一约束关联列排序后的列集合
     */
    private List<String> sortedList;

    /**
     * 唯一约束的构造方法
     *
     * @param count 需要进行唯一约束的条目数量
     */
    public UniqueConstraint(String constraintName, String database, String tableName,
            Map<String, Map<String, Integer>> consColumns, int count) {
        super(constraintName, database, tableName, consColumns);
        if (count <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Count for unique constraint can not be equal to or smaller than zero");
        }
        this.judger = new DuplicatedJudger(count);
    }

    /**
     * 唯一约束的构造函数，通过该构造函数构造一个唯一约束对象
     *
     * @param rows  初始化列数据，通过该列数据的传入定义唯一约束的一些初始值
     * @param count 需要进行唯一约束的记录条目数
     */
    public UniqueConstraint(String constraintName, String database, String tableName,
            Map<String, Map<String, Integer>> consColumns, List<Map<String, Pair<AbstractDataType, Object>>> rows, int count) {
        super(constraintName, database, tableName, consColumns, rows);
        if (count <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    "Count for unique constraint can not be equal to or smaller than zero");
        }
        if (rows != null && rows.size() != 0) {
            this.judger = new DuplicatedJudger(rows.size() + count);
            for (Map<String, Pair<AbstractDataType, Object>> row : rows) {
                String result = convert(row);
                judger.add(result);
            }
        } else {
            this.judger = new DuplicatedJudger(count);
        }
    }

    @Override
    protected void initWithRows(Map<String, Integer> columns, List<Map<String, Pair<AbstractDataType, Object>>> rows) {
        this.sortedList = sortMapByValue(columns);
    }

    @Override
    protected boolean doCheck(Map<String, Integer> columns, Map<String, Pair<AbstractDataType, Object>> value, Boolean markable) {
        String checkValue = convert(value);
        if (judger.contains(checkValue)) {
            //log.warn(String.format("value \"%s\" for columns \"%s\" can not pass the unique constraint, will be droped", checkValue,
            //        columns.keySet().stream().collect(Collectors.joining(","))));
            return false;
        }
        if (markable) {
            if (!judger.add(checkValue)) {
                log.warn(String.format("fail to add row \"%s\" to dup util", checkValue));
            }
        }
        return true;
    }

    /**
     * 转化方法，在这里需要将一行数据转化为一个字符串用于接下来的唯一性检测
     *
     * @param row 一行数据
     * @return 返回一行数据的字符串
     */
    private String convert(Map<String, Pair<AbstractDataType, Object>> row) {
        List<String> list = new ArrayList<>();
        for (String column : sortedList) {
            Pair<AbstractDataType, ?> value = row.get(column);
            if (value == null) {
                throw new MockerException(MockerError.OPERATION_FAILURE,
                        String.format("Data for unique constraint have to have same column list \"%s\"",
                                sortedList.stream().collect(Collectors.joining(","))));
            }
            Object convertVal = value.getKey().convert(value.getValue());
            Object digestVal = value.getKey().toDigest(convertVal);
            list.add(value.getKey().toString(digestVal));
        }
        return list.stream().collect(Collectors.joining(","));
    }

    /**
     * 对列映射表进行排序，按照列的position进行排序
     *
     * @param map 映射表
     * @return 返回排序好的集合
     */
    public static List<String> sortMapByValue(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        List<String> sortedList = new ArrayList<>();
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
        Collections.sort(entryList, (o1, o2) -> o1.getValue().compareTo(o2.getValue()));
        Iterator<Map.Entry<String, Integer>> iter = entryList.iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Integer> entry = iter.next();
            sortedList.add(entry.getKey());
        }
        return sortedList;
    }
}
