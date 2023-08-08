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
package com.oceanbase.tools.datamocker.model.dbobject;

import com.oceanbase.tools.datamocker.util.Column;
import lombok.Getter;
import lombok.Setter;

/**
 * Database Constraint Associated Column Data Package Object
 *
 * @author yh263208
 * @date 2021-01-11 20:19
 * @since OBMOCKER-0.1.0-snapshot
 */
@Getter
@Setter
public class ConstraintColumn {
    @Column("OWNER")
    private String owner;
    @Column("CONSTRAINT_NAME")
    private String constraintName;
    @Column("TABLE_NAME")
    private String tableName;
    @Column("COLUMN_NAME")
    private String columnName;
    @Column("POSITION")
    private Object position;
    @Column("SEARCH_CONDITION")
    private String searchCondition;
}
