package com.oceanbase.tools.datamocker.model.dbobject;

import java.math.BigDecimal;

import com.oceanbase.tools.datamocker.util.Column;
import lombok.Getter;
import lombok.Setter;

/**
 * Table column data package object
 *
 * @author yh263208
 * @date 2021-01-11 21:59
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class TableColumn {
    @Column("OWNER")
    private String owner;
    @Column("TABLE_NAME")
    private String tableName;
    @Column("COLUMN_NAME")
    private String columnName;
    @Column("DATA_TYPE")
    private String dataType;
    @Column("DATA_TYPE_OWNER")
    private String dataTypeOwner;
    @Column("DATA_LENGTH")
    private Object dataLength;
    @Column("DATA_PRECISION")
    private Object precision;
    @Column("DATA_SCALE")
    private Object scale;
    @Column("NULLABLE")
    private String nullable;
    @Column("COLUMN_ID")
    private BigDecimal columnId;
    @Column("DEFAULT_LENGTH")
    private BigDecimal defaultLength;
    @Column("HIDDEN_COLUMN")
    private String hiddenColumn;
    @Column("VIRTUAL_COLUMN")
    private String virtualColumn;
    @Column("GENERATION_EXPRESSION")
    private String expression;
}
