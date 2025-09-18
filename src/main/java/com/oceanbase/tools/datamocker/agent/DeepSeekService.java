// DeepSeekService.java
package com.oceanbase.tools.datamocker.agent;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class DeepSeekService {

    private final String apiKey;
    private final String apiEndpoint; // 例如:https://api.deepseek.com/chat/completions
    private final OkHttpClient client;
    private final Gson gson = new Gson();

    public DeepSeekService(String apiKey, String apiEndpoint, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.apiEndpoint = apiEndpoint;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    /** 非流式:一次性拿完整文本 */
    public String chatCompletion(String model, String userContent, double temperature) throws IOException {
        ChatRequest req = new ChatRequest();
        req.model = model;
        req.temperature = temperature;
        req.stream = Boolean.FALSE;
        req.messages = Collections.singletonList(new ChatRequest.Message("user", userContent));

        String json = gson.toJson(req);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request http = new Request.Builder()
                .url(apiEndpoint)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response resp = client.newCall(http).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("DeepSeek请求失败, http=" + resp.code() + " body=" + (resp.body() != null ? resp.body().string() : ""));
            }
            String respStr = resp.body().string();
            ChatResponse cr = gson.fromJson(respStr, ChatResponse.class);
            if (cr != null && cr.choices != null && !cr.choices.isEmpty() && cr.choices.get(0).message != null) {
                return cr.choices.get(0).message.content;
            }
            return "";
        }
    }

    /** 旧的流式(保留) */
    public void streamChatCompletion(String model, String userContent, double temperature, StreamCallback cb) throws IOException {
        ChatRequest req = new ChatRequest();
        req.model = model;
        req.temperature = temperature;
        req.stream = Boolean.TRUE;
        req.messages = Collections.singletonList(new ChatRequest.Message("user", userContent));

        String json = gson.toJson(req);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request http = new Request.Builder()
                .url(apiEndpoint)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "text/event-stream")
                .post(body)
                .build();

        try (Response resp = client.newCall(http).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("DeepSeek流式请求失败, http=" + resp.code() + " body=" + (resp.body() != null ? resp.body().string() : ""));
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.body().byteStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String payload = line.substring(6).trim();
                    if ("[DONE]".equals(payload)) break;

                    try {
                        ChatResponse chunk = gson.fromJson(payload, ChatResponse.class);
                        if (chunk != null && chunk.choices != null && !chunk.choices.isEmpty()
                                && chunk.choices.get(0).delta != null) {
                            String piece = chunk.choices.get(0).delta.content;
                            if (piece != null && cb != null) cb.onDelta(piece);
                        }
                    } catch (JsonSyntaxException ignore) {
                        // 忽略心跳/注释行
                    }
                }
            }
        }
    }

    // ✅ 新增:流式→返回最终SQL的版本(边播边收集,收尾清洗)
    public String streamChatCompletionSql(String model, String userContent, double temperature, StreamCallback cb) throws IOException {
        ChatRequest req = new ChatRequest();
        req.model = model;
        req.temperature = temperature;
        req.stream = Boolean.TRUE;
        req.messages = Collections.singletonList(new ChatRequest.Message("user", userContent));

        String json = gson.toJson(req);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request http = new Request.Builder()
                .url(apiEndpoint)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "text/event-stream")
                .post(body)
                .build();

        StringBuilder sb = new StringBuilder(32 * 1024);
        try (Response resp = client.newCall(http).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("DeepSeek流式请求失败, http=" + resp.code() + " body=" + (resp.body() != null ? resp.body().string() : ""));
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.body().byteStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String payload = line.substring(6).trim();
                    if ("[DONE]".equals(payload)) break;

                    try {
                        ChatResponse chunk = gson.fromJson(payload, ChatResponse.class);
                        if (chunk != null && chunk.choices != null && !chunk.choices.isEmpty()
                                && chunk.choices.get(0).delta != null) {
                            String piece = chunk.choices.get(0).delta.content;
                            if (piece != null && !piece.isEmpty()) {
                                sb.append(piece);
                                if (cb != null) cb.onDelta(piece); // 实时回显
                            }
                        }
                    } catch (JsonSyntaxException ignore) {
                        // 忽略心跳/注释
                    }
                }
            }
        }

        // 清洗为纯SQL
        String raw = sb.toString();
        String sql = SqlOutputUtil.sanitizeToSql(raw);
        SqlOutputUtil.assertLooksLikeSql(sql);
        return sql;
    }

    /** 生成SQL脚本的提示词(仍可复用) */
    public String generateSqlByDDL(String ddl, int rows, String tableName, String locale) throws IOException {
        String prompt = ""
                + "你是专业的数据库数据生成器，请基于以下DDL为" + tableName + "生成" + rows + "行模拟数据的SQL脚本。\n"
                + "【目标】只输出一个可直接执行的.sql脚本内容，不要任何解释、不要JSON、不要Markdown代码块。\n"
                + "【语言环境】" + locale + "\n"
                + "【输入DDL】\n" + ddl + "\n\n"
                + "【输出要求】\n"
                + "1. 仅输出SQL文本，严禁出现说明文字、JSON、以及```之类的Markdown标记。\n"
                + "2. 使用事务包裹:\n"
                + "   BEGIN;\n"
                + "   -- 多条INSERT\n"
                + "   COMMIT;\n"
                + "3. 对于标识符，使用反引号包裹(例如`" + tableName + "`,`col_name`)以避免关键字冲突。\n"
                + "4. 每条语句以分号;结尾，换行分隔，UTF-8编码。\n"
                + "5. 值格式：\n"
                + "   - 字符串使用单引号并正确转义单引号(用两个单引号''表示一个引号)。\n"
                + "   - 日期/时间按'YYYY-MM-DD'或'YYYY-MM-DD HH:MM:SS'，不要写函数(NOW()/CURRENT_TIMESTAMP)。\n"
                + "   - DECIMAL/NUMBER使用纯数字(不要引号)。\n"
                + "6. 严格满足DDL中的约束:\n"
                + "   - PRIMARY KEY/UNIQUE必须唯一且不为空。\n"
                + "   - NOT NULL列必须给值。\n"
                + "   - 长度/精度不得超限。\n"
                + "7. 尽量生成有语义的数据(如邮箱/人名/日期/部门等)，但不能违反约束。\n"
                + "8. 性能友好：多行VALUES分组写法(每条INSERT可包含多组VALUES，建议每条INSERT不超过100行)。\n\n"
                + "【再次强调】只输出SQL脚本文本，不要多余内容。\n";

        return chatCompletion("deepseek-chat", prompt, 0.3);
    }

    // DeepSeekService.java (新增一个更通用的流式方法)
    public void streamChatCompletionRaw(String model, String userContent, double temperature,
                                        StreamCallback cb) throws IOException {
        ChatRequest req = new ChatRequest();
        req.model = model;
        req.temperature = temperature;
        req.stream = Boolean.TRUE;
        req.messages = Collections.singletonList(new ChatRequest.Message("user", userContent));

        String json = gson.toJson(req);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request http = new Request.Builder()
                .url(apiEndpoint)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "text/event-stream")
                .post(body)
                .build();

        try (Response resp = client.newCall(http).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("DeepSeek流式请求失败, http=" + resp.code() + " body=" + (resp.body() != null ? resp.body().string() : ""));
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.body().byteStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String payload = line.substring(6).trim();
                    if ("[DONE]".equals(payload)) break;

                    try {
                        ChatResponse chunk = gson.fromJson(payload, ChatResponse.class);
                        if (chunk != null && chunk.choices != null && !chunk.choices.isEmpty()
                                && chunk.choices.get(0).delta != null) {
                            String piece = chunk.choices.get(0).delta.content;
                            if (piece != null && cb != null) cb.onDelta(piece);
                        }
                    } catch (JsonSyntaxException ignore) {
                        // 心跳/注释忽略
                    }
                }
            }
        }
    }


    public interface StreamCallback {
        void onDelta(String text);
    }
}
