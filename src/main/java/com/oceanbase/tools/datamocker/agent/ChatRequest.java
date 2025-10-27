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

/**
 * LLM API 请求数据模型。封装向 DeepSeek API 发送聊天完成请求的参数信息。
 */
public class ChatRequest {
    public String model;
    public java.util.List<Message> messages;
    public Double temperature;
    public Boolean stream; // 是否流式SSE

    public static class Message {
        public String role;    // "user"/"system"/"assistant"
        public String content; // 文本
        public Message() {}
        public Message(String role, String content) {
            this.role = role; this.content = content;
        }
    }
}
