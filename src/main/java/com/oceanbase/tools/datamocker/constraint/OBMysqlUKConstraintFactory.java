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

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.model.dbobject.ConstraintColumn;
import com.oceanbase.tools.datamocker.model.dbobject.TableColumn;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.util.SerializeUtil;
import lombok.NonNull;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@link OBMysqlUKConstraintFactory}
 *
 * @author yh263208
 * @date 2023-11-27 20:01
 * @since ODC_release_4.2.3
 */
class OBMysqlUKConstraintFactory extends AbstractUniqueConstraintFactory {

    private static final String LIST_CONSTRAINTS_COLS_KEY = "list-constraint-columns";

    public OBMysqlUKConstraintFactory(@NonNull DataSource dataSource,
            @NonNull String schema, @NonNull String tableName, int totalCount) {
        super(dataSource, schema, tableName, totalCount);
    }

    @Override
    protected String getFileName() {
        return OB_MYSQL_CONSTRAINT_FILE;
    }

    @Override
    protected String getListConstraintsKey() {
        return LIST_UK_KEY;
    }

    @Override
    protected void checkVirtualColumn(DataSource dataSource, ConstraintColumn constraintColumn) {
        String content = getSqlContent(getFileName(), LIST_CONSTRAINTS_COLS_KEY);
        Boolean result = new JdbcTemplate(dataSource).query(content, ps -> {
            ps.setString(1, constraintColumn.getOwner());
            ps.setString(2, constraintColumn.getColumnName());
            ps.setString(3, constraintColumn.getTableName());
        }, rs -> {
            try {
                TableColumn tableCol = SerializeUtil.getObject(rs, TableColumn.class);
                if (tableCol == null) {
                    return false;
                }
                return StringUtils.isBlank(tableCol.getExpression());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        });
        if (Boolean.FALSE.equals(result)) {
            String msg = String.format("Virtual column \"%s.%s\" for constraint is not support yet",
                    constraintColumn.getTableName(), constraintColumn.getColumnName());
            throw new MockerException(MockerError.NOT_SUPPORT_FEATURE, msg);
        }
    }

}
