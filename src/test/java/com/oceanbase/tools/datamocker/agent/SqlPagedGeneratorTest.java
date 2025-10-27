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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.oceanbase.tools.datamocker.config.TaskConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

/**
 * SqlPagedGenerator 集成测试。使用 WireMock 模拟 DeepSeek API，验证分页 SQL 生成功能。
 */
public class SqlPagedGeneratorTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    private DeepSeekService deepSeekService;
    private TaskConfig taskConfig;

    @Before
    public void setUp() {
        // 初始化 DeepSeek 服务，使用 WireMock 模拟的 API 端点
        deepSeekService = new DeepSeekService(
                "test-api-key",
                "http://localhost:8089/v1/chat/completions",
                30
        );

        // 构建任务配置
        taskConfig = new TaskConfig();
        taskConfig.taskName = "test_task";

        // 设置输出配置
        TaskConfig.Output output = new TaskConfig.Output();
        output.dir = "/tmp";
        output.filePrefix = "test_sql";
        output.rows = 100;
        output.batchSize = 50;
        output.schema = "test_schema";
        output.table = "test_table";
        taskConfig.output = output;

        // 设置表配置
        TaskConfig.Table table = new TaskConfig.Table();
        table.name = "test_table";
        table.uniqueConstraints = Arrays.asList(
                Arrays.asList("id")
        );

        // 设置列配置
        TaskConfig.Column col1 = new TaskConfig.Column();
        col1.name = "id";
        col1.type = "INT";
        col1.nullable = false;
        col1.comment = "主键";

        TaskConfig.Column col2 = new TaskConfig.Column();
        col2.name = "name";
        col2.type = "VARCHAR(50)";
        col2.nullable = false;
        col2.comment = "姓名";

        table.columns = Arrays.asList(col1, col2);
        taskConfig.table = table;
    }

    /**
     * 测试基本的 SQL 生成流程：首段 -> 末段
     */
    @Test
    public void testBasicSqlGeneration() throws Exception {
        // 首段响应：包含 BEGIN 和第一批 INSERT
        String firstSegmentResponse = createChatResponse(
                "BEGIN;\n" +
                "INSERT INTO `test_schema`.`test_table`(`id`,`name`) VALUES\n" +
                "(1,'Alice'),\n" +
                "(2,'Bob');\n"
        );

        // 末段响应：包含剩余 INSERT 和 COMMIT
        String finalSegmentResponse = createChatResponse(
                "INSERT INTO `test_schema`.`test_table`(`id`,`name`) VALUES\n" +
                "(3,'Charlie'),\n" +
                "(4,'David');\n" +
                "COMMIT;\n"
        );

        // 模拟 API 调用
        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(firstSegmentResponse)
                        .withFixedDelay(100)));

        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(finalSegmentResponse)
                        .withFixedDelay(100)));

        // 执行生成
        String result = SqlPagedGenerator.generate(
                taskConfig,
                deepSeekService,
                "deepseek-chat",
                15000
        );

        // 验证结果
        assertNotNull(result);
        assertTrue(result.contains("BEGIN;"));
        assertTrue(result.contains("INSERT INTO"));
        assertTrue(result.contains("COMMIT;"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("Charlie"));
    }

    /**
     * 测试多段生成：首段 -> 中段 -> 末段
     */
    @Test
    public void testMultiSegmentGeneration() throws Exception {
        // 首段
        String firstResponse = createChatResponse(
                "BEGIN;\n" +
                "INSERT INTO `test_schema`.`test_table`(`id`,`name`) VALUES\n" +
                "(1,'Alice'),\n" +
                "(2,'Bob');\n"
        );

        // 中段
        String middleResponse = createChatResponse(
                "INSERT INTO `test_schema`.`test_table`(`id`,`name`) VALUES\n" +
                "(3,'Charlie'),\n" +
                "(4,'David'),\n" +
                "(5,'Eve');\n"
        );

        // 末段
        String finalResponse = createChatResponse(
                "INSERT INTO `test_schema`.`test_table`(`id`,`name`) VALUES\n" +
                "(6,'Frank');\n" +
                "COMMIT;\n"
        );

        // 设置期望 100 行数据
        taskConfig.output.rows = 100;

        // 多次调用返回不同内容
        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .inScenario("multi-segment")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(firstResponse))
                .willSetStateTo("FirstDone"));

        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .inScenario("multi-segment")
                .whenScenarioStateIs("FirstDone")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(middleResponse))
                .willSetStateTo("MiddleDone"));

        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .inScenario("multi-segment")
                .whenScenarioStateIs("MiddleDone")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(finalResponse))
                .willSetStateTo("Done"));

        // 执行生成
        String result = SqlPagedGenerator.generate(
                taskConfig,
                deepSeekService,
                "deepseek-chat",
                15000
        );

        // 验证结果包含所有段的数据
        assertNotNull(result);
        assertTrue(result.contains("BEGIN;"));
        assertTrue(result.contains("COMMIT;"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("Frank"));
    }

    /**
     * 测试首段验证失败：缺少 BEGIN
     */
    @Test
    public void testFirstSegmentValidationFailure() throws Exception {
        String invalidResponse = createChatResponse(
                "INSERT INTO `test_schema`.`test_table`(`id`,`name`) VALUES\n" +
                "(1,'Alice');\n"
                // 缺少 BEGIN
        );

        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(invalidResponse)));

        // 应该抛出异常
        Exception exception = null;
        try {
            SqlPagedGenerator.generate(
                    taskConfig,
                    deepSeekService,
                    "deepseek-chat",
                    15000
            );
        } catch (Exception e) {
            exception = e;
        }

        assertNotNull(exception);
        assertTrue(exception instanceof IllegalStateException);
        assertTrue(exception.getMessage().contains("首段不符合预期"));
    }

    /**
     * 测试 API 错误处理
     */
    @Test
    public void testApiErrorHandling() throws Exception {
        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // 应该抛出异常
        Exception exception = null;
        try {
            SqlPagedGenerator.generate(
                    taskConfig,
                    deepSeekService,
                    "deepseek-chat",
                    15000
            );
        } catch (Exception e) {
            exception = e;
        }

        assertNotNull(exception);
        assertTrue(exception instanceof IOException || exception instanceof IllegalStateException);
    }

    /**
     * 测试 Markdown 响应的清洗
     */
    @Test
    public void testMarkdownResponseCleaning() throws Exception {
        String markdownResponse = createChatResponse(
                "```sql\n" +
                "BEGIN;\n" +
                "INSERT INTO `test_schema`.`test_table`(`id`,`name`) VALUES\n" +
                "(1,'Alice'),\n" +
                "(2,'Bob');\n" +
                "```\n" +
                "```sql\n" +
                "INSERT INTO `test_schema`.`test_table`(`id`,`name`) VALUES\n" +
                "(3,'Charlie');\n" +
                "COMMIT;\n" +
                "```"
        );

        taskConfig.output.rows = 100;

        // 第一次调用返回首段
        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .inScenario("markdown")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(markdownResponse))
                .willSetStateTo("FirstDone"));

        // 第二次调用返回末段
        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .inScenario("markdown")
                .whenScenarioStateIs("FirstDone")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(markdownResponse))
                .willSetStateTo("Done"));

        // 执行生成
        String result = SqlPagedGenerator.generate(
                taskConfig,
                deepSeekService,
                "deepseek-chat",
                15000
        );

        // 验证 Markdown 标记被移除，SQL 内容被保留
        assertNotNull(result);
        assertFalse(result.contains("```"));
        assertTrue(result.contains("BEGIN;"));
        assertTrue(result.contains("COMMIT;"));
    }

    /**
     * 测试动态行数调整
     */
    @Test
    public void testDynamicRowsAdjustment() throws Exception {
        // 模拟返回很长的响应（接近预算上限）
        String longResponse = createChatResponse(generateLongSqlResponse(90000));

        // 模拟返回很短的响应（远低于预算）
        String shortResponse = createChatResponse(
                "INSERT INTO `test_schema`.`test_table`(`id`,`name`) VALUES\n" +
                "(100,'User');\n" +
                "COMMIT;\n"
        );

        taskConfig.output.rows = 100;

        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .inScenario("dynamic")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("BEGIN;\n" + longResponse))
                .willSetStateTo("LongDone"));

        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .inScenario("dynamic")
                .whenScenarioStateIs("LongDone")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(shortResponse))
                .willSetStateTo("Done"));

        // 执行生成
        String result = SqlPagedGenerator.generate(
                taskConfig,
                deepSeekService,
                "deepseek-chat",
                100000
        );

        // 验证结果有效
        assertNotNull(result);
        assertTrue(result.contains("BEGIN;"));
        assertTrue(result.contains("COMMIT;"));
    }

    /**
     * 测试 COMMIT 补齐
     */
    @Test
    public void testCommitCompletion() throws Exception {
        String responseWithoutCommit = createChatResponse(
                "BEGIN;\n" +
                "INSERT INTO `test_schema`.`test_table`(`id`,`name`) VALUES\n" +
                "(1,'Alice'),\n" +
                "(2,'Bob'),\n" +
                "(3,'Charlie');\n"
                // 缺少 COMMIT
        );

        taskConfig.output.rows = 100;

        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseWithoutCommit)));

        // 执行生成
        String result = SqlPagedGenerator.generate(
                taskConfig,
                deepSeekService,
                "deepseek-chat",
                15000
        );

        // 验证 COMMIT 被自动添加
        assertNotNull(result);
        assertTrue(result.contains("COMMIT;"));
        // 应该只有一个 COMMIT
        int commitCount = result.split("COMMIT;").length - 1;
        assertEquals(1, commitCount);
    }

    /**
     * 辅助方法：创建 ChatResponse JSON
     */
    private String createChatResponse(String content) {
        return "{\"choices\":[{\"message\":{\"content\":\"" +
                content.replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r") +
                "\"}}]}";
    }

    /**
     * 辅助方法：生成很长的 SQL 响应
     */
    private String generateLongSqlResponse(int minLength) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("INSERT INTO `test_schema`.`test_table`(`id`,`name`) VALUES\n");
            for (int j = 0; j < 50; j++) {
                sb.append("(").append(i * 50 + j).append(",'User").append(i * 50 + j).append("')");
                if (j < 49) sb.append(",");
            }
            sb.append(";\n");
        }
        return sb.toString();
    }
}
