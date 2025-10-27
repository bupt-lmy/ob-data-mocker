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
 * LLM API 响应数据模型。封装从 DeepSeek API 返回的聊天完成结果。
 */
public class ChatResponse {
    public java.util.List<Choice> choices;

    public static class Choice {
        public Message message; // 非流式
        public Delta delta;     // 流式
    }
    public static class Message {
        public String content;
    }
    public static class Delta {
        public String content;
    }
}
