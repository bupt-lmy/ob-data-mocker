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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DeepSeek LLM API 服务类。提供非流式和流式两种方式调用 LLM API 生成数据。
 */
public class DeepSeekService {

    private final String apiKey;
    private final String apiEndpoint; // 例如:https://api.deepseek.com/chat/completions
    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

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

        // 添加网络层异常捕获
        try (Response resp = client.newCall(http).execute()) {
            // 4. 优化 API 错误处理和日志记录
            if (!resp.isSuccessful()) {
                String errorBody = (resp.body() != null) ? resp.body().string() : "No response body";
                log.error("DeepSeek non-streaming request failed. HTTP Status: {}, Body: {}", resp.code(), errorBody);
                throw new IOException("DeepSeek request failed, http=" + resp.code());
            }
            String respStr = resp.body().string();
            ChatResponse cr = gson.fromJson(respStr, ChatResponse.class);
            if (cr != null && cr.choices != null && !cr.choices.isEmpty() && cr.choices.get(0).message != null) {
                return cr.choices.get(0).message.content;
            }
            log.warn("DeepSeek response was successful but contained no valid choices or message content.");
            return "";
        } catch (IOException e) {
            // 捕获 OkHttp 相关的网络异常 (如超时)
            log.error("Network error during DeepSeek chatCompletion: {}", e.getMessage(), e);
            throw e; // 重新抛出，让上层知道发生了错误
        }
    }

    // 流式调用API生成数据
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

        // 添加网络层异常捕获
        try (Response resp = client.newCall(http).execute()) {
            // 优化 API 错误处理和日志记录
            if (!resp.isSuccessful()) {
                String errorBody = (resp.body() != null) ? resp.body().string() : "No response body";
                log.error("DeepSeek streaming request failed. HTTP Status: {}, Body: {}", resp.code(), errorBody);
                throw new IOException("DeepSeek streaming request failed, http=" + resp.code());
            }

            // 内部的 try-with-resources 会在退出时自动关闭 reader
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.body().byteStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) {
                        // 记录被跳过的行，但级别设为 TRACE
                        log.trace("Skipping line without 'data: ' prefix: {}", line);
                        continue;
                    }
                    String payload = line.substring(6).trim();
                    if ("[DONE]".equals(payload)) {
                        log.debug("Stream finished with [DONE] message.");
                        break;
                    }

                    try {
                        ChatResponse chunk = gson.fromJson(payload, ChatResponse.class);
                        if (chunk != null && chunk.choices != null && !chunk.choices.isEmpty()
                                && chunk.choices.get(0).delta != null) {
                            String piece = chunk.choices.get(0).delta.content;
                            if (piece != null && cb != null) {
                                cb.onDelta(piece);
                            }
                        }
                    } catch (JsonSyntaxException e) {
                        // 5. 优化被忽略的 JSON 解析异常
                        log.trace("Failed to parse stream chunk, ignoring (heartbeat or comment?): {}", line, e);
                    }
                }
            }
            // BufferedReader 抛出的 IOException 会被下面的 catch 块捕获
        } catch (IOException e) {
            // 捕获 OkHttp 网络异常或 BufferedReader 读取异常
            log.error("Network or stream read error during DeepSeek streamChatCompletionRaw: {}", e.getMessage(), e);
            throw e; // 重新抛出
        }
    }


    public interface StreamCallback {
        void onDelta(String text);
    }
}
