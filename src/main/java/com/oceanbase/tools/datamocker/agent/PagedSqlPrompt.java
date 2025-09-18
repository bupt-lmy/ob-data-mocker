/*
 * Copyright (c) 2025 OceanBase.
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

package com.oceanbase.tools.datamocker.agent;


import com.oceanbase.tools.datamocker.config.TaskConfig;
import java.util.List;
import java.util.stream.Collectors;

public class PagedSqlPrompt {

    private static String fqTable(TaskConfig cfg) {
        String schema = cfg.output.schema;
        String table = cfg.output.table;
        return (schema != null && !schema.isEmpty())
                ? "`" + schema + "`.`" + table + "`"
                : "`" + table + "`";
    }

    /** 列清单（反引号包裹，**使用配置顺序**） */
    private static String backtickCols(TaskConfig cfg) {
        return cfg.table.columns.stream()
                .map(c -> "`" + c.name + "`")
                .collect(Collectors.joining(","));
    }

    /** 唯一约束提示（不必包含全部历史键，仅声明规则） */
    private static String uniqueRules(TaskConfig cfg) {
        List<List<String>> ucs = cfg.table.uniqueConstraints;
        if (ucs == null || ucs.isEmpty()) return "- 无显式唯一性要求（除非DDL/数据库约束另行定义）。\n";
        StringBuilder sb = new StringBuilder();
        for (List<String> uc : ucs) {
            sb.append("- 组合唯一：(")
                    .append(uc.stream().map(s -> "`" + s + "`").collect(Collectors.joining(",")))
                    .append(") 必须在本次与历史输出中不重复。\n");
        }
        return sb.toString();
    }

    private static String columnComments(TaskConfig cfg) {
        StringBuilder sb = new StringBuilder();
        for (TaskConfig.Column c : cfg.table.columns) {
            sb.append("- ").append(c.name).append(": ")
                    .append(c.comment == null ? "" : c.comment).append("\n");
        }
        return sb.toString();
    }

    /** 首段：带 BEGIN;，无 COMMIT;，不做“起始ID”的假设 */
    public static String firstChunk(TaskConfig cfg, int rowsThisCall) {
        String fq = fqTable(cfg);
        String cols = backtickCols(cfg);
        return ""
                + "你是专业数据库数据生成器，请为表 " + fq + " 生成最多 " + rowsThisCall + " 行 INSERT 语句，作为【首段】。\n"
                + "【输出**仅限**SQL文本】严禁输出解释、JSON、Markdown。\n"
                + "【事务要求】本段**必须**以 `BEGIN;` 开头，**禁止**出现 `COMMIT;`。\n"
                + "【语句形式】仅允许：\n"
                + "  BEGIN;\n"
                + "  INSERT INTO " + fq + "(" + cols + ") VALUES\n"
                + "    (...),(...);  -- 可多条 INSERT；每条 INSERT 的 VALUES 数量 ≤ " + Math.max(1, cfg.output.batchSize == null ? 100 : cfg.output.batchSize) + "\n"
                + "【列顺序】严格与 (" + cols + ") 一致。\n"
                + "【约束/类型】\n"
                + "  - NOT NULL 必须赋值；字符串单引号并转义；日期 'YYYY-MM-DD'；数值不用引号。\n"
                + "  - 唯一性：\n" + uniqueRules(cfg)
                + "【字段解释(可空)】\n" + columnComments(cfg)
                + "【请开始输出首段 SQL】";
    }

    /** 中段：禁止 BEGIN/COMMIT；避免机械化规律数据 */
    public static String nextChunk(TaskConfig cfg, int rowsThisCall, int alreadyGenerated) {
        String fq = fqTable(cfg);
        String cols = backtickCols(cfg);
        return ""
                + "继续为表 " + fq + " 生成 INSERT 语句，作为【中间续段】。\n"
                + "【上下文】之前已生成约 " + alreadyGenerated + " 行，请继续追加**全新**行，避免与历史重复，尤其是唯一键。\n"
                + "【输出仅限SQL】严禁解释/JSON/Markdown。\n"
                + "请严格按以下排版输出：\n" +
                "   - 每个 INSERT 语句单独一行起始；\n" +
                "   - 多组 VALUES 请使用换行分隔格式：\"),\\n(\"；\n"
                + "【禁止】本段**禁止**出现 `BEGIN;` 与 `COMMIT;`。\n"
                + "【行数上限】最多 " + rowsThisCall + " 行；可拆多条 INSERT；每条 INSERT 的 VALUES 数量 ≤ " + Math.max(1, cfg.output.batchSize == null ? 100 : cfg.output.batchSize) + "\n"
                + "【列顺序】(" + cols + ")\n"
                + "【类型/约束】同首段；确保唯一性：\n" + uniqueRules(cfg)
                + "【随机性要求】\n"
                + "  - 姓名、日期、数字等必须随机多样，避免出现 'Emp101','Emp102' 这种机械规律。\n"
                + "  - 不要简单按序号拼接生成值。\n"
                + "  - 尽量模拟真实场景，如英文人名、常见年龄分布、合理日期范围。\n"
                + "【正例】(101,'Robert',29,'2016-08-25')\n"
                + "【反例】(102,'Emp102',30,'2020-01-01')\n"
                + "【请开始输出中间续段 SQL】";
    }

    /** 末段：补齐剩余行并输出唯一 COMMIT;，同样避免机械化规律 */
    public static String finalChunk(TaskConfig cfg, int remaining, int alreadyGenerated) {
        String fq = fqTable(cfg);
        String cols = backtickCols(cfg);
        return ""
                + "输出表 " + fq + " 的【最后一段】插入 SQL。\n"
                + "【上下文】已生成约 " + alreadyGenerated + " 行，还需补足 " + remaining + " 行。\n"
                + "【输出仅限SQL】严禁解释/JSON/Markdown。\n"
                + "请严格按以下排版输出：\n" +
                "   - 每个 INSERT 语句单独一行起始；\n" +
                "   - 多组 VALUES 请使用换行分隔格式：\"),\\n(\"；\n"
                + "【禁止】本段**禁止**出现 `BEGIN;`。\n"
                + "【要求】先输出若干 INSERT 语句（每条 VALUES 数量 ≤ " + Math.max(1, cfg.output.batchSize == null ? 100 : cfg.output.batchSize) + "），随后**只输出一次** `COMMIT;` 收尾。\n"
                + "【列顺序】(" + cols + ")\n"
                + "【类型/约束/唯一性】同前：\n" + uniqueRules(cfg)
                + "【随机性要求】\n"
                + "  - 所有值必须保持语义随机性，避免简单自增或拼接。\n"
                + "  - 姓名应分布多样，日期覆盖范围广，年龄合理分布。\n"
                + "  - 严禁出现机械规律序列值（如 Emp123, Emp124）。\n"
                + "【正例】(200,'Maria',36,'2018-03-12')\n"
                + "【反例】(201,'Emp201',36,'2020-01-01')\n"
                + "【请开始输出最后一段 SQL并在末尾给出唯一 COMMIT;】";
    }

}
