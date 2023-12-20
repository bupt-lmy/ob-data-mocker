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
import lombok.NonNull;

/**
 * {@link OBOraclePKConstraintFactory}
 *
 * @author yh263208
 * @date 2023-11-27 15:11
 * @since ODC_release_4.2.3
 */
class OBOraclePKConstraintFactory extends AbstractUniqueConstraintFactory {

    public OBOraclePKConstraintFactory(@NonNull DataSource dataSource,
            @NonNull String schema, @NonNull String tableName, int totalCount) {
        super(dataSource, schema, tableName, totalCount);
    }

    @Override
    protected String getFileName() {
        return OB_ORACLE_CONSTRAINT_FILE;
    }

    @Override
    protected String getListConstraintsKey() {
        return LIST_PK_KEY;
    }

    @Override
    protected void checkVirtualColumn(DataSource dataSource, ConstraintColumn constraintColumn) {}

}
