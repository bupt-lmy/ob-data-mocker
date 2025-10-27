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

/**
 * 行大小估算工具。估算 SQL 行长度并动态计算每次 LLM 调用应生成的行数。
 */
public final class RowSizeEstimator {
    private RowSizeEstimator() {}

    /** 估算一行 VALUES(...) 的字符长度（粗略保守） */
    public static int estimateOneRowChars(TaskConfig cfg) {
        int base = 6; // 括号、逗号、空格等
        for (TaskConfig.Column c : cfg.table.columns) {
            base += estimateColumnChars(c);
            base += 2; // 加上逗号与空格余量
        }
        return base + 4; // 结尾 ),\n 余量
    }

    private static int estimateColumnChars(TaskConfig.Column c) {
        // 以类型名/定义粗略推断长度；同时考虑注释越长，模型输出越“啰嗦”，略微增加
        String t = (c.type == null ? "" : c.type.toUpperCase());
        int commentBias = Math.min(20, c.comment == null ? 0 : c.comment.length() / 10);

        if (t.contains("CHAR") || t.contains("TEXT") || t.contains("CLOB")) {
            int width = extractWidth(t, 32);
            int avg = Math.max(8, Math.min(width, 24)); // 平均字符串长度(反引号/引号不计)
            return avg + 2 /*引号*/ + commentBias;
        }
        if (t.contains("DATE")) {
            return 12 /*'YYYY-MM-DD'*/ + 2 /*引号*/;
        }
        if (t.contains("TIME")) {
            return 21 /*'YYYY-MM-DD HH:MM:SS'*/ + 2;
        }
        if (t.contains("DECIMAL") || t.contains("NUMBER")) {
            int[] ps = extractPrecisionScale(t);
            int digits = Math.max(3, ps[0] == 0 ? 6 : ps[0] + 1); // 带符号/小数点
            return digits;
        }
        if (t.contains("INT") || t.contains("BIGINT") || t.contains("SMALLINT") || t.contains("TINYINT")) {
            return 6; // 平均 2~6 位
        }
        // 其他未知类型：保守估 16
        return 16 + commentBias;
    }

    private static int extractWidth(String type, int defVal) {
        // 形如 VARCHAR(50) / NVARCHAR(128)
        int l = type.indexOf('(');
        int r = type.indexOf(')');
        if (l > 0 && r > l) {
            try { return Integer.parseInt(type.substring(l + 1, r).trim()); } catch (Exception ignore) {}
        }
        return defVal;
    }

    private static int[] extractPrecisionScale(String type) {
        // DECIMAL(p,s) / NUMBER(p,s)
        int[] ps = new int[]{6, 0};
        int l = type.indexOf('(');
        int r = type.indexOf(')');
        if (l > 0 && r > l) {
            String[] arr = type.substring(l + 1, r).split(",");
            try {
                ps[0] = Integer.parseInt(arr[0].trim());
                if (arr.length > 1) ps[1] = Integer.parseInt(arr[1].trim());
            } catch (Exception ignore) {}
        }
        return ps;
    }

    /**
     * 动态计算“本次最多生成行数”：
     * - 以 maxCharsBudget 控制本次返回字符预算（建议 10k~18k 区间）
     * - 同时受 batchSize 限制（每条 INSERT 的 VALUES 数）
     */
    public static int decideRowsPerCall(TaskConfig cfg, int maxCharsBudget) {
        int perRow = Math.max(20, estimateOneRowChars(cfg));
        // 单个 INSERT 头部开销(INSERT INTO `sch`.`tbl`(`a`,`b`,... ) VALUES\n)
        int insertHead = 32 + cfg.table.columns.size() * 4;
        int rowsByBudget = Math.max(20, (maxCharsBudget - insertHead) / perRow);
        // 不要太激进，给模型一点冗余：
        rowsByBudget = (int) Math.floor(rowsByBudget * 0.85);

        // 再与 batchSize&安全阈值取 min：每条 INSERT 最多 batchSize 组；允许多条 INSERT
        int cap = Math.max(50, cfg.output.batchSize == null ? 100 : cfg.output.batchSize);
        // 每次调用最多输出 ~8 个 INSERT（经验值，避免超长）
        int logicalMax = cap * 8;
        return Math.max(30, Math.min(rowsByBudget, logicalMax));
    }
}
