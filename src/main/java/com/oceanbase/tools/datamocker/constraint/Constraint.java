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

import java.util.Map;

import com.oceanbase.tools.datamocker.model.mock.MockRowData;

/**
 * {@link Constraint}
 *
 * @author yh263208
 * @date 2023-11-27 14:45
 * @since ODC_release_4.2.3
 */
public interface Constraint {

    boolean check(MockRowData rowData);

    boolean mark(MockRowData value);

    String name();

    /**
     * The name of the column to which the constraint is associated. The database has a position
     * description for the column to which the constraint is associated. That is, the column to which
     * the constraint is associated is in the position of the constraint, and the column to which a
     * constraint is associated may not only be in one table Here, the Key of the outer Map represents
     * the table name, used to indicate which table the constraint-related column is in, and the inner
     * Map is used to indicate the position of the constraint-related column in a table in the
     * constraint, Key represents the column name, and value represents The position of the constraint
     * is meaningful for constraint checking, because if a unique constraint is established on two
     * columns, then the positional relationship between the two columns in the constraint is necessary,
     * because AB and BA are obviously in compliance with the constraint even if They just swapped
     * positions
     */
    Map<String, Map<String, Integer>> columns();

}
