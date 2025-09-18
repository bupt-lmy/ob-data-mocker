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

import java.io.File;
import java.io.FileWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlOutputUtil {

    // 清洗: 去除```和非SQL噪声, 保留BEGIN/COMMIT/INSERT/SET/--注释等
    public static String sanitizeToSql(String raw) {
        if (raw == null) return "";
        String s = raw.trim();

        // 去掉Markdown围栏
        s = s.replaceAll("(?s)```sql", "");
        s = s.replaceAll("(?s)```", "");

        // 可选: 去掉常见前后缀说明性文字(若LLM不守规，可兜底)
        s = s.replaceAll("(?i)^\\s*(以下为.*?SQL.*?:|仅供参考.*?:)\\s*", "");
        s = s.replaceAll("(?i)\\s*(--\\s*END\\s*)?$", "");

        // 只保留以BEGIN/COMMIT/INSERT/UPDATE/DELETE/SET/--开头的行或含分号的语句
        StringBuilder keep = new StringBuilder();
        String[] lines = s.split("\\r?\\n");
        Pattern allow = Pattern.compile("^\\s*(BEGIN|COMMIT|INSERT|UPDATE|DELETE|SET|--|/\\*|\\*/|LOCK|UNLOCK)\\b", Pattern.CASE_INSENSITIVE);
        for (String line : lines) {
            String L = line.trim();
            if (L.isEmpty()) continue;
            if (allow.matcher(L).find() || L.endsWith(";")) {
                keep.append(line).append("\n");
            }
        }
        return keep.toString().trim();
    }

    public static void writeSqlToFile(String sql, File outFile) throws Exception {
        outFile.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(outFile, false)) {
            fw.write(sql);
            fw.write("\n");
        }
    }

    // 简单校验: 至少包含1次INSERT与BEGIN/COMMIT
    public static void assertLooksLikeSql(String sql) {
        if (sql == null || !sql.toUpperCase().contains("INSERT INTO")) {
            throw new IllegalArgumentException("返回内容不包含INSERT语句, 请检查提示词或网络响应");
        }
        if (!sql.toUpperCase().contains("BEGIN") || !sql.toUpperCase().contains("COMMIT")) {
            throw new IllegalArgumentException("返回内容未包含BEGIN/COMMIT事务包裹, 请检查提示词");
        }
    }
}

