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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * SqlStreamSanitizer 单元测试。测试流式内容清洗功能。
 */
public class SqlStreamSanitizerTest {

    /**
     * 测试空值和空字符串的处理
     */
    @Test
    public void testSanitizeDeltaWithNullAndEmpty() {
        // null 应该返回空字符串
        assertEquals("", SqlStreamSanitizer.sanitizeDelta(null));
        
        // 空字符串应该返回空字符串
        assertEquals("", SqlStreamSanitizer.sanitizeDelta(""));
        
        // 仅空格应该返回空字符串
        assertEquals("", SqlStreamSanitizer.sanitizeDelta("   "));
    }

    /**
     * 测试 Markdown 代码围栏的移除
     */
    @Test
    public void testRemoveMarkdownFences() {
        // 移除 ```sql 围栏
        String input1 = "```sql\nSELECT * FROM table;\n```";
        String expected1 = "\nSELECT * FROM table;\n";
        assertEquals(expected1, SqlStreamSanitizer.sanitizeDelta(input1));
        
        // 移除 ```SQL 围栏（大写）
        String input2 = "```SQL\nINSERT INTO t VALUES(1);\n```";
        String expected2 = "\nINSERT INTO t VALUES(1);\n";
        assertEquals(expected2, SqlStreamSanitizer.sanitizeDelta(input2));
        
        // 移除 ``` 围栏
        String input3 = "```\nBEGIN;\n```";
        String expected3 = "\nBEGIN;\n";
        assertEquals(expected3, SqlStreamSanitizer.sanitizeDelta(input3));
    }

    /**
     * 测试 SQL 前缀的移除
     */
    @Test
    public void testRemoveSqlPrefix() {
        // 移除 "SQL script:" 前缀
        String input1 = "SQL script: BEGIN;\nINSERT INTO t VALUES(1);";
        String expected1 = "BEGIN;\nINSERT INTO t VALUES(1);";
        assertEquals(expected1, SqlStreamSanitizer.sanitizeDelta(input1));
        
        // 移除 "sql script:" 前缀（小写）
        String input2 = "sql script: COMMIT;";
        String expected2 = "COMMIT;";
        assertEquals(expected2, SqlStreamSanitizer.sanitizeDelta(input2));
        
        // 处理带空格的前缀
        String input3 = "  SQL  script  : UPDATE table SET x=1;";
        String expected3 = "UPDATE table SET x=1;";
        assertEquals(expected3, SqlStreamSanitizer.sanitizeDelta(input3));
    }

    /**
     * 测试复杂场景：多个清洗操作
     */
    @Test
    public void testComplexSanitization() {
        String input = "```sql\nSQL script: INSERT INTO emp(id,name) VALUES\n(1,'Alice'),\n(2,'Bob');\n```";
        String result = SqlStreamSanitizer.sanitizeDelta(input);
        
        // 应该去掉 ``` 和 SQL script: 前缀
        assertTrue(result.contains("INSERT INTO emp"));
        assertFalse(result.contains("```"));
        assertFalse(result.contains("SQL script:"));
    }

    /**
     * 测试纯 SQL 内容不被破坏
     */
    @Test
    public void testPureSqlPreserved() {
        String sql = "BEGIN;\nINSERT INTO users(id,name,email) VALUES\n(1,'Alice','alice@example.com'),\n(2,'Bob','bob@example.com');\nCOMMIT;";
        String result = SqlStreamSanitizer.sanitizeDelta(sql);
        
        // 内容应该保持不变
        assertEquals(sql, result);
    }

    /**
     * 测试只有前缀没有 SQL 的情况
     */
    @Test
    public void testOnlyPrefixNoSql() {
        String input = "SQL script: ";
        String result = SqlStreamSanitizer.sanitizeDelta(input);
        
        // 应该只剩下空字符串或空格
        assertTrue(result.trim().isEmpty());
    }

    /**
     * 测试多个围栏的处理
     */
    @Test
    public void testMultipleFences() {
        String input = "```sql\nBEGIN;\n``` some text ```\nCOMMIT;\n```";
        String result = SqlStreamSanitizer.sanitizeDelta(input);
        
        // 所有 ``` 都应该被移除
        assertFalse(result.contains("```"));
        assertTrue(result.contains("BEGIN;"));
        assertTrue(result.contains("COMMIT;"));
    }

    /**
     * 测试保留 SQL 注释
     */
    @Test
    public void testPreserveSqlComments() {
        String input = "-- 这是注释\nINSERT INTO t VALUES(1); -- 行末注释";
        String result = SqlStreamSanitizer.sanitizeDelta(input);
        
        // 注释应该被保留
        assertEquals(input, result);
    }

    /**
     * 测试多行输入
     */
    @Test
    public void testMultilineInput() {
        String input = "```sql\n" +
                "BEGIN;\n" +
                "INSERT INTO emp(id,name) VALUES\n" +
                "(1,'Alice'),\n" +
                "(2,'Bob');\n" +
                "COMMIT;\n" +
                "```";
        String result = SqlStreamSanitizer.sanitizeDelta(input);
        
        // 应该移除围栏但保留 SQL 内容
        assertFalse(result.contains("```"));
        assertTrue(result.contains("BEGIN;"));
        assertTrue(result.contains("INSERT INTO"));
        assertTrue(result.contains("COMMIT;"));
    }
}
