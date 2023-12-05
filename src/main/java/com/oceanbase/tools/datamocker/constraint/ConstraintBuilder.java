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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import lombok.Builder;

/**
 * {@link ConstraintBuilder}
 *
 * @author yh263208
 * @date 2023-11-27 21:34
 * @since ODC_release_4.2.3
 */
@Builder
public class ConstraintBuilder {

    private final ObModeType obModeType;
    private final DataSource dataSource;
    private final String schema;
    private final String tableName;
    private final int totalMockCount;
    private final Map<String, AbstractDataType<?, ? extends Comparable<?>>> columnName2DataType;

    public List<Constraint> getConstraints() {
        return getAllConstraintFactories().stream().flatMap(f -> {
            List<Constraint> constraints = f.generate();
            if (f instanceof AbstractUniqueConstraintFactory && columnName2DataType != null) {
                validateConstraints(constraints, tableName, columnName2DataType, totalMockCount);
            }
            return constraints.stream();
        }).collect(Collectors.toList());
    }

    public List<ConstraintFactory> getAllConstraintFactories() {
        switch (obModeType) {
            case OB_MYSQL:
                return getAllConstraintFactoriesMysql(dataSource, schema, tableName, totalMockCount);
            case OB_ORACLE:
                return getAllConstraintFactoriesOracle(dataSource, schema, tableName, totalMockCount);
            default:
                return Collections.emptyList();
        }
    }

    private static List<ConstraintFactory> getAllConstraintFactoriesMysql(DataSource dataSource,
            String schema, String tableName, int totalCount) {
        return Arrays.asList(new OBMysqlUKConstraintFactory(dataSource, schema, tableName, totalCount),
                new OBMysqlPKConstraintFactory(dataSource, schema, tableName, totalCount));
    }

    private static List<ConstraintFactory> getAllConstraintFactoriesOracle(DataSource dataSource,
            String schema, String tableName, int totalCount) {
        return Arrays.asList(new OBOracleForeignConstraintFactory(dataSource, schema, tableName),
                new OBOracleCheckConstraintFactory(dataSource, schema, tableName),
                new OBOracleUKConstraintFactory(dataSource, schema, tableName, totalCount),
                new OBOraclePKConstraintFactory(dataSource, schema, tableName, totalCount));
    }

    /**
     * Verify method for constraint object eg. If a unique constraint restricts at most n different
     * pieces of data can be generated, but the input requires more than n pieces of data to be
     * generated, an error will be reported
     */
    private static void validateConstraints(List<Constraint> constraints, String tableName,
            Map<String, AbstractDataType<?, ? extends Comparable<?>>> columnName2DataType, int totalCount) {
        for (Constraint constraint : constraints) {
            Map<String, Integer> cols = constraint.columns().get(tableName);
            Set<String> colSet = cols.keySet();
            Long limitCount = 1L;
            for (String col : colSet) {
                AbstractDataType<?, ? extends Comparable<?>> dataType = columnName2DataType.get(col);
                Long typeLimit = dataType.distinctLimit();
                if (typeLimit > totalCount) {
                    limitCount = (long) totalCount;
                    break;
                }
                limitCount *= dataType.distinctLimit();
                if (limitCount > totalCount) {
                    break;
                }
            }
            if (limitCount < totalCount) {
                throw new MockerException(MockerError.PARAMETER_ERROR, String.format(
                        "The given data generator can only generate %d unique data for cols {%s}, but the goal is %d",
                        limitCount.intValue(), String.join(", ", colSet), totalCount));
            }
        }
    }

}
