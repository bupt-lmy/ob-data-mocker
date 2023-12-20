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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.model.dbobject.ConstraintColumn;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.SerializeUtil;
import lombok.NonNull;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@link AbstractUniqueConstraintFactory}
 *
 * @author yh263208
 * @date 2023-11-27 16:45
 * @since ODC_release_4.2.3
 */
abstract class AbstractUniqueConstraintFactory extends AbstractConstraintFactory {

    private final int totalCount;

    public AbstractUniqueConstraintFactory(@NonNull DataSource dataSource,
            @NonNull String schema, @NonNull String tableName, int totalCount) {
        super(dataSource, schema, tableName);
        this.totalCount = totalCount;
    }

    @Override
    protected List<Constraint> doGenerate(String querySql, DataSource dataSource, String schema, String tableName) {
        List<ConstraintColumn> cols = new JdbcTemplate(dataSource).query(querySql, ps -> {
            ps.setString(1, schema);
            ps.setString(2, tableName);
        }, rs -> {
            try {
                return SerializeUtil.getList(rs, ConstraintColumn.class);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        });
        if (cols == null) {
            throw new IllegalStateException("Failed to get constraint columns");
        }
        return cols.stream().collect(Collectors.groupingBy(ConstraintColumn::getConstraintName)).entrySet().stream()
                .map(entry -> {
                    Map<String, Map<String, Integer>> columnMap = new HashMap<>();
                    for (ConstraintColumn item : entry.getValue()) {
                        checkVirtualColumn(dataSource, item);
                        Map<String, Integer> map = columnMap.getOrDefault(item.getTableName(), new HashMap<>());
                        if (item.getPosition() instanceof BigDecimal) {
                            BigDecimal position = (BigDecimal) item.getPosition();
                            map.putIfAbsent(item.getColumnName(), position.intValue());
                        } else if (item.getPosition() instanceof Long) {
                            Long position = (Long) item.getPosition();
                            map.putIfAbsent(item.getColumnName(), position.intValue());
                        } else {
                            throw new MockerException(MockerError.ILLEGAL_RETURN_VALUE,
                                    "Position's type is not support");
                        }
                        columnMap.put(item.getTableName(), map);
                    }
                    return new UniqueConstraint(entry.getKey(), tableName, columnMap, totalCount);
                }).collect(Collectors.toList());
    }

    protected abstract void checkVirtualColumn(DataSource dataSource, ConstraintColumn constraintColumn);

}
