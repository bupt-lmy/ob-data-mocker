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

package com.oceanbase.tools.datamocker.constraint;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.NonNull;

/**
 * {@link AbstractConstraintFactory}
 *
 * @author yh263208
 * @date 2023-11-27 15:24
 * @since ODC_release_4.2.3
 */
abstract class AbstractConstraintFactory implements ConstraintFactory {

    protected static final String LIST_PK_KEY = "list-pk-constraints";
    protected static final String LIST_UK_KEY = "list-unique-constraints";
    protected static final String LIST_CHECK_CONSTRAINT_KEY = "list-check-constraints";
    protected static final String LIST_FOREIGN_CONSTRAINT_KEY = "list-foreign-constraints";
    protected static final String OB_ORACLE_CONSTRAINT_FILE = "sql/constraint/oboracle.yaml";
    protected static final String OB_MYSQL_CONSTRAINT_FILE = "sql/constraint/obmysql.yaml";
    private final static Map<String, Map<String, String>> FILENAME2KEY2SQL = new HashMap<>();

    static {
        addSchemaFile(OB_ORACLE_CONSTRAINT_FILE);
        addSchemaFile(OB_MYSQL_CONSTRAINT_FILE);
    }

    private final DataSource dataSource;
    private final String schema;
    private final String tableName;

    public AbstractConstraintFactory(@NonNull DataSource dataSource, @NonNull String schema,
            @NonNull String tableName) {
        this.schema = schema;
        this.dataSource = dataSource;
        this.tableName = tableName;
    }

    @Override
    public List<Constraint> generate() {
        return doGenerate(getSqlContent(getFileName(), getListConstraintsKey()), this.dataSource, this.schema,
                this.tableName);
    }

    protected static String getSqlContent(String fileName, String key) {
        Map<String, String> key2Sqls = FILENAME2KEY2SQL.get(fileName);
        if (key2Sqls == null) {
            throw new IllegalStateException("Failed to get file content, " + fileName);
        }
        String sql = key2Sqls.get(key);
        if (sql != null) {
            return sql;
        }
        throw new IllegalStateException("Failed to sql content, " + key);
    }

    protected abstract List<Constraint> doGenerate(String querySql, DataSource dataSource, String schema,
            String tableName);

    protected abstract String getFileName();

    protected abstract String getListConstraintsKey();

    private static void addSchemaFile(String filePath) {
        URL url = AbstractConstraintFactory.class.getClassLoader().getResource(filePath);
        if (url == null) {
            throw new IllegalStateException("Failed to load file, " + filePath);
        }
        Map<String, String> key2Sqls = fromYaml(url);
        if (key2Sqls == null) {
            return;
        }
        FILENAME2KEY2SQL.put(filePath, key2Sqls);
    }

    private static Map<String, String> fromYaml(URL url) {
        if (url == null) {
            return null;
        }
        try {
            return yamlMapper().readValue(url, new TypeReference<Map<String, String>>() {});
        } catch (IOException ex) {
            return null;
        }
    }

    private static ObjectMapper yamlMapper() {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.setPropertyNamingStrategy(new PropertyNamingStrategy.SnakeCaseStrategy());
        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return yamlMapper;
    }

}
