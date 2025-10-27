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

// SqlStreamSanitizer.java
package com.oceanbase.tools.datamocker.agent;

/**
 * 流式内容清洗工具。清洗 LLM 流式返回的增量内容中的 Markdown 标记和噪声。
 */
public final class SqlStreamSanitizer {
    private SqlStreamSanitizer(){}

    /** 增量清洗：去掉```代码围栏、markdown提示符等 */
    public static String sanitizeDelta(String delta) {
        if (delta == null || delta.isEmpty()) return "";
        String s = delta;

        // 去掉markdown围栏
        s = s.replace("```sql", "")
                .replace("```SQL", "")
                .replace("```", "");

        // 去掉常见解释性前缀（尽量不破坏SQL）
        s = s.replaceAll("(?i)^(\\s*SQL\\s*script\\s*:)\\s*", "");

        return s;
    }
}
