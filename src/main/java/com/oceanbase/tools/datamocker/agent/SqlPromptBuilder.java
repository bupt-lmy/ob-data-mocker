package com.oceanbase.tools.datamocker.agent;

import com.oceanbase.tools.datamocker.config.TaskConfig;
import com.oceanbase.tools.datamocker.config.GeneratorParamUtil;

import java.util.StringJoiner;

public class SqlPromptBuilder {

    public static String buildFromTaskConfig(TaskConfig cfg) {
        TaskConfig.Output out = cfg.output;
        TaskConfig.Table  tab = cfg.table;

        int rows = out != null && out.rows != null ? out.rows : 100;
        String tableName = (out != null && out.table != null) ? out.table :
                (tab  != null && tab.name != null ? tab.name : "t");

        String ddl = buildDDLFromConfig(cfg); // 仅用于给模型上下文，不做真实建表
        String dialect = "mysql"; // 你也可以做成可配置

        String prompt = ""
                + "你是专业的数据库数据生成器，请基于以下DDL为`" + tableName + "`生成"
                + rows + "行模拟数据的SQL脚本。\n"
                + "【数据库方言】" + dialect + "\n"
                + "【输入DDL】\n" + ddl + "\n\n"
                + "【字段补充说明】\n" + buildFieldHints(cfg) + "\n\n"
                + "【输出要求】\n"
                + "1. 仅输出SQL文本，严禁出现说明文字、JSON、以及```之类的Markdown标记。\n"
                + "2. 全文必须且只出现一次事务包裹：\n"
                + "   BEGIN;\n"
                + "   -- 多条INSERT (每条 <= " + (out != null && out.batchSize != null ? out.batchSize : 100) + " 行 VALUES)\n"
                + "   COMMIT;\n"
                + "   不能产生额外的BEGIN/COMMIT。\n"
                + "3. 标识符用反引号(如`" + tableName + "`)，每条语句以分号;结尾，换行分隔，UTF-8编码。\n"
                + "4. 只输出SQL文本，不要多余说明、不要省略COMMIT。\n"
                + "5. 每条语句以分号;结尾，换行分隔，UTF-8编码。\n"
                + "6. 值格式：字符串用单引号且对单引号转义(用''表示)。日期格式'YYYY-MM-DD'。\n"
                + "7. 严格满足DDL约束：PRIMARY KEY/UNIQUE 唯一非空；NOT NULL 必填；长度/精度不得越界。\n"
                + "8. 性能友好：INSERT ... VALUES 支持多行分组(每条不超过" + (out != null && out.batchSize != null ? out.batchSize : 100) + "行)。\n";
        return prompt;
    }

    private static String buildDDLFromConfig(TaskConfig cfg) {
        TaskConfig.Output out = cfg.output;
        TaskConfig.Table  tab = cfg.table;
        String schema = out != null && out.schema != null ? out.schema : null;
        String table  = out != null && out.table  != null ? out.table  : (tab != null ? tab.name : "t");

        String fq = (schema != null && !schema.isEmpty())
                ? ("`" + schema + "`.`" + table + "`")
                : ("`" + table + "`");

        StringJoiner cols = new StringJoiner(",\n  ", "  ", "");
        if (tab != null && tab.columns != null) {
            for (TaskConfig.Column c : tab.columns) {
                String nullPart = (c.nullable != null && c.nullable) ? "" : " NOT NULL";
                cols.add("`" + c.name + "` " + c.type + nullPart);
            }
        }
        StringJoiner cons = new StringJoiner(",\n  ");
        if (tab != null && tab.uniqueConstraints != null) {
            for (java.util.List<String> uc : tab.uniqueConstraints) {
                if (uc != null && !uc.isEmpty()) {
                    StringJoiner j = new StringJoiner("`,`", "(`", "`)");
                    uc.forEach(j::add);
                    cons.add("UNIQUE " + j.toString());
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(fq).append(" (\n")
                .append(cols.toString());
        if (cons.length() > 0) {
            sb.append(",\n  ").append(cons.toString());
        }
        sb.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
        return sb.toString();
    }

    private static String buildFieldHints(TaskConfig cfg) {
        if (cfg.table == null || cfg.table.columns == null) return "(无)";
        StringBuilder sb = new StringBuilder();
        for (TaskConfig.Column c : cfg.table.columns) {
            sb.append("- ").append(c.name).append(" : ")
                    .append(c.comment == null ? "" : c.comment).append("\n");
            if (c.generator != null && c.generator.params != null) {
                String g = c.generator.name == null ? "" : c.generator.name.toUpperCase();
                switch (g) {
                    case "STEP": {
                        int start = GeneratorParamUtil.getInt(c.generator.params, "start", 1);
                        int step  = GeneratorParamUtil.getInt(c.generator.params, "step", 1);
                        sb.append("  生成器=STEP(start=").append(start).append(", step=").append(step).append(")\n");
                        break;
                    }
                    case "RANGE_INT": {
                        int s = GeneratorParamUtil.getInt(c.generator.params, "start", 0);
                        int e = GeneratorParamUtil.getInt(c.generator.params, "end", 100);
                        sb.append("  生成器=RANGE_INT(").append(s).append("~").append(e).append(")\n");
                        break;
                    }
                    case "REGEXP": {
                        String p = GeneratorParamUtil.getString(c.generator.params, "pattern", "");
                        sb.append("  生成器=REGEXP(\"").append(p).append("\")\n");
                        break;
                    }
                    case "RANDOM_DATE": {
                        String s = GeneratorParamUtil.getString(c.generator.params, "start", "2000-01-01");
                        String e = GeneratorParamUtil.getString(c.generator.params, "end",   "2025-01-01");
                        sb.append("  生成器=RANDOM_DATE(").append(s).append(" ~ ").append(e).append(")\n");
                        break;
                    }
                    default:
                        sb.append("  生成器=").append(g).append("\n");
                }
            }
        }
        return sb.toString();
    }
}
