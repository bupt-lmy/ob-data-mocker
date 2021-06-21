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
}
