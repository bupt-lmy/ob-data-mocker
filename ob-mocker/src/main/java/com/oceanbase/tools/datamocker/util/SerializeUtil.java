package com.oceanbase.tools.datamocker.util;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * ODC对象反序列化工具类，用于从DB中以对象形式读取对象
 *
 * @author yh263208
 * @date 2020-12-02 17:24
 * @since ODC_release_2.4.0
 */
public class SerializeUtil {
    /**
     * 从数据库中反序列化对象集合
     *
     * @param result 查询的结果
     * @param clazz  目标对象的类型
     * @return 返回反序列化后的对象集合
     * @throws Exception 反射生成对象以及列集合长度和结果集和长度不一致时都会抛异常
     */
    public static <T> List<T> getList(ResultSet result, Class<T> clazz) throws SQLException, InstantiationException,
                                                                               IllegalAccessException {
        if (result == null) {
            return Collections.emptyList();
        }
        ResultSetMetaData metaData = result.getMetaData();
        List<List<Object>> resultList = new ArrayList<>();
        List<String> columnList = new ArrayList<>();
        int columnWidth = metaData.getColumnCount();
        while (result.next()) {
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < columnWidth; i++) {
                list.add(result.getObject(i + 1));
                if (columnList.size() == i) {
                    columnList.add(metaData.getColumnLabel(i + 1));
                }
            }
            resultList.add(list);
        }
        Map<String, Integer> columnMap = new HashMap<>();
        if (resultList.size() == 0) {
            return Collections.emptyList();
        }
        if (resultList.get(0).size() != columnList.size()) {
            throw new MockerException(MockerError.OPERATION_FAILURE,
                    String.format("Result set's length \"%d\" is not equal to the length of the column names \"%d\"",
                            resultList.get(0).size(),
                            columnList.size()));
        }
        int columnLength = columnList.size();
        for (int i = 0; i < columnLength; i++) {
            columnMap.putIfAbsent(columnList.get(i), i);
        }
        List<T> returnVal = new ArrayList<>();
        for (List<Object> row : resultList) {
            returnVal.add(parseObject(row, columnMap, clazz));
        }
        return returnVal;
    }

    /**
     * 从数据库中反序列化对象
     *
     * @param result 查询的结果
     * @param clazz  目标对象的类型
     * @return 返回反序列化后的对象
     * @throws Exception 反射生成对象以及列集合长度和结果集和长度不一致时都会抛异常
     */
    public static <T> T getObject(ResultSet result, Class<T> clazz) throws SQLException, InstantiationException, IllegalAccessException {
        if (result == null) {
            return null;
        }
        ResultSetMetaData metaData = result.getMetaData();
        List<Object> resultList = new ArrayList<>();
        List<String> columnList = new ArrayList<>();
        int columnWidth = metaData.getColumnCount();
        if (result.next()) {
            for (int i = 0; i < columnWidth; i++) {
                resultList.add(result.getObject(i + 1));
                columnList.add(metaData.getColumnLabel(i + 1));
            }
        }
        Map<String, Integer> columnMap = new HashMap<>();
        if (resultList.size() == 0) {
            return null;
        }
        if (resultList.size() != columnList.size()) {
            throw new MockerException(MockerError.OPERATION_FAILURE,
                    String.format("Result set's length \"%d\" is not equal to the length of the column names \"%d\"", resultList.size(),
                            columnList.size()));
        }
        int columnLength = columnList.size();
        for (int i = 0; i < columnLength; i++) {
            columnMap.putIfAbsent(columnList.get(i), i);
        }
        return parseObject(resultList, columnMap, clazz);
    }

    /**
     * 内部方法，从一个list中得到一个对象
     *
     * @param input     输入的集合
     * @param columnMap 列名与索引之间的映射关系
     * @param clazz     目标对象的类型
     * @return 返回反序列化后的对象
     * @throws Exception 反射生成对象以及列集合长度和结果集和长度不一致时都会抛异常
     */
    private static <T> T parseObject(List<Object> input, Map<String, Integer> columnMap, Class<T> clazz)
            throws IllegalAccessException, InstantiationException {
        T instance = clazz.newInstance();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Column annotation = field.getDeclaredAnnotation(Column.class);
            String serializationName = null;
            boolean ignore = false;
            if (annotation == null) {
                serializationName = field.getName();
            } else {
                serializationName = annotation.value();
                ignore = annotation.ignore();
            }
            if (ignore) {
                continue;
            }
            Integer index = columnMap.get(serializationName);
            if (index == null) {
                continue;
            }
            Object value = input.get(index);
            field.setAccessible(true);
            field.set(instance, value);
        }
        return instance;
    }
}
