package com.oceanbase.tools.datamocker.config;

import java.util.List;
import java.util.Map;

/**
 * SQL 数据生成任务配置类。定义表名、列定义、输出路径等生成任务的全部配置信息。
 */
public class TaskConfig {
    public String taskName;
    public Output output;
    public Table table;

    public static class Output {
        public String dir;
        public String filePrefix;
        public Integer rows;
        public Integer batchSize;
        public String schema;
        public String table;
    }

    public static class Table {
        public String name;
        public List<List<String>> uniqueConstraints; // [["ID"], ["NAME","HIRE_DATE"]] 可选
        public List<Column> columns;
    }

    public static class Column {
        public String name;
        public String type;       // 例如 "INT" / "VARCHAR(50)" / "DATE"
        public Boolean nullable;
        public String comment;    // 可选
        public Generator generator;
    }

    public static class Generator {
        public String name;                   // STEP / REGEXP / RANGE_INT / RANDOM_DATE ...
        public Map<String, Object> params;   // 🔁 弱类型，兼容数字或字符串
    }
}
