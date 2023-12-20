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

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.model.dbobject.ConstraintColumn;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.SerializeUtil;
import lombok.NonNull;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@link OBOracleCheckConstraintFactory}
 *
 * @author yh263208
 * @date 2023-11-27 20:13
 * @since ODC_release_4.2.3
 */
class OBOracleCheckConstraintFactory extends AbstractConstraintFactory {

    private static final Pattern NOT_NULL_PATTERN = Pattern.compile(".+ is not null", Pattern.CASE_INSENSITIVE);

    public OBOracleCheckConstraintFactory(@NonNull DataSource dataSource, @NonNull String schema,
            @NonNull String tableName) {
        super(dataSource, schema, tableName);
    }

    @Override
    protected List<Constraint> doGenerate(String querySql, DataSource dataSource, String schema, String tableName) {
        List<ConstraintColumn> columns = new JdbcTemplate(dataSource).query(querySql, ps -> {
            ps.setString(1, schema);
            ps.setString(2, tableName);
        }, rs -> {
            try {
                return SerializeUtil.getList(rs, ConstraintColumn.class);
            } catch (InstantiationException | IllegalAccessException e) {
                return Collections.emptyList();
            }
        });
        if (columns == null) {
            return Collections.emptyList();
        }
        if (columns.stream().anyMatch(c -> !NOT_NULL_PATTERN.matcher(c.getSearchCondition()).matches())) {
            throw new MockerException(MockerError.NOT_SUPPORT_FEATURE, "Check constraint is not support yet");
        }
        return Collections.emptyList();
    }

    @Override
    protected String getFileName() {
        return OB_ORACLE_CONSTRAINT_FILE;
    }

    @Override
    protected String getListConstraintsKey() {
        return LIST_CHECK_CONSTRAINT_KEY;
    }

}
