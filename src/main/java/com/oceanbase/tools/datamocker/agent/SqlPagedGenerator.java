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
 * 非流式分页 SQL 生成器。通过多次分批调用 LLM API 生成完整的事务包裹 SQL 脚本。
 */
public class SqlPagedGenerator {

    /**
     * @param maxCharsBudgetPerCall 单次响应最大字符预算（建议 12_000 ~ 18_000）
     */
    public static String generate(TaskConfig cfg,
                                  DeepSeekService ds,
                                  String model,
                                  int maxCharsBudgetPerCall) throws Exception {
        int total = cfg.output.rows != null ? cfg.output.rows : 500;
        StringBuilder acc = new StringBuilder(256 * 1024);

        // 动态行数
        int rowsThisCall = RowSizeEstimator.decideRowsPerCall(cfg, maxCharsBudgetPerCall);

        // 1) 首段
        String p1 = PagedSqlPrompt.firstChunk(cfg, Math.min(rowsThisCall, total));
        String first = ds.chatCompletion(model, p1, 0.2);
        first = SqlOutputUtil.sanitizeToSql(first);
        // 必须有 BEGIN，不能包含 COMMIT（若模型误加我们也删）
        if (!first.toUpperCase().contains("BEGIN;") || !first.toUpperCase().contains("INSERT INTO")) {
            throw new IllegalStateException("首段不符合预期（缺少 BEGIN 或 INSERT INTO）。");
        }
        first = first.replaceAll("(?i)\\bCOMMIT;\\s*", ""); // 防止误放
        acc.append(first.trim());
        if (!acc.toString().endsWith(";")) acc.append(";"); // 防断句
        acc.append("\n");

        int generated = SqlTupleCounter.countTuples(first);

        // 2) 续段（不再依赖主键递增）
        while (generated < total) {
            int remaining = total - generated;
            boolean isFinal = remaining <= rowsThisCall;

            String prompt = isFinal
                    ? PagedSqlPrompt.finalChunk(cfg, remaining, generated)
                    : PagedSqlPrompt.nextChunk(cfg, Math.min(rowsThisCall, remaining), generated);

            String chunk = ds.chatCompletion(model, prompt, isFinal ? 0.2 : 0.25);
            chunk = SqlOutputUtil.sanitizeToSql(chunk);

            // 中段禁止 BEGIN；
            chunk = chunk.replaceAll("(?i)\\bBEGIN;\\s*", "");
            if (!isFinal) {
                // 中段禁止 COMMIT；
                chunk = chunk.replaceAll("(?i)\\bCOMMIT;\\s*", "");
            }

            // 追加时，若上一段未分号且下一段直接 INSERT，补分号
            if (acc.length() > 0 && !acc.toString().trim().endsWith(";")
                    && chunk.trim().toUpperCase().startsWith("INSERT INTO")) {
                acc.append(";\n");
            }
            acc.append(chunk.trim());
            if (!acc.toString().endsWith(";")) acc.append(";"); // 防断句
            acc.append("\n");

            int added = SqlTupleCounter.countTuples(chunk);
            if (added == 0 && !isFinal) {
                // 再轻度重试一次，降低温度
                String retry = ds.chatCompletion(model, prompt, 0.1);
                retry = SqlOutputUtil.sanitizeToSql(retry)
                        .replaceAll("(?i)\\bBEGIN;\\s*", "")
                        .replaceAll("(?i)\\bCOMMIT;\\s*", "");
                acc.append(retry.trim());
                if (!acc.toString().endsWith(";")) acc.append(";");
                acc.append("\n");
                added = SqlTupleCounter.countTuples(retry);
            }

            generated += added;

            // 若列很多/内容长导致 rowsThisCall 仍偏大，可根据上次返回的字符数动态收敛
            int lastChars = chunk.length();
            if (lastChars > (maxCharsBudgetPerCall * 0.9)) {
                rowsThisCall = Math.max(20, (int) (rowsThisCall * 0.7));
            } else if (lastChars < (maxCharsBudgetPerCall * 0.5)) {
                rowsThisCall = Math.min(rowsThisCall + 20, rowsThisCall * 2); // 有余量就适度扩
            }
        }

        // 3) 收尾：确保唯一 COMMIT;
        String sql = acc.toString().trim();
        // 若已包含多个 COMMIT（极少数模型误加），保留最后一个
        sql = sql.replaceAll("(?i)(?s)COMMIT;\\s*(?=.+COMMIT;)", "");
        if (!sql.toUpperCase().contains("COMMIT;")) {
            if (!sql.endsWith(";")) sql += ";";
            sql += "\nCOMMIT;";
        }
        sql = SqlOutputUtil.sanitizeToSql(sql);
        SqlOutputUtil.assertLooksLikeSql(sql);
        return sql;
    }
}
