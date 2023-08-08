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
 * ODC object deserialization tool class, used to read objects from the DB in the form of objects
 *
 * @author yh263208
 * @date 2020-12-02 17:24
 * @since ODC_release_2.4.0
 */
public class SerializeUtil {
    /**
     * Deserialize a collection of objects from the database
     *
     * @param result Query result
     * @param clazz 目标对象的类型
     * @return Return the deserialized collection of objects
     * @throws Exception An exception will be thrown when the length of the reflection generated object
     *         and the column collection is inconsistent with the result set
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
     * Deserialize objects from the database
     *
     * @param result Query Result
     * @param clazz Target type
     * @return Return the deserialized object
     * @throws SQLException,InstantiationException,IllegalAccessException An exception will be thrown
     *         when the length of the reflection generated object and the column collection is
     *         inconsistent with the result set
     */
    public static <T> T getObject(ResultSet result, Class<T> clazz)
            throws SQLException, InstantiationException, IllegalAccessException {
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
                    String.format("Result set's length \"%d\" is not equal to the length of the column names \"%d\"",
                            resultList.size(),
                            columnList.size()));
        }
        int columnLength = columnList.size();
        for (int i = 0; i < columnLength; i++) {
            columnMap.putIfAbsent(columnList.get(i), i);
        }
        return parseObject(resultList, columnMap, clazz);
    }

    /**
     * Internal method, get an object from a list
     *
     * @param input Input set
     * @param columnMap The mapping relationship between column names and indexes
     * @param clazz Target type
     * @return Return the deserialized object
     * @throws IllegalAccessException,InstantiationException An exception will be thrown when the length
     *         of the reflection generated object and the column collection is inconsistent with the
     *         result set
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
