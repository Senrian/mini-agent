package com.miniagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * AI聊天请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 系统提示
     */
    private String systemPrompt;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 最大token数
     */
    private Integer maxTokens;

    /**
     * 会话ID (可选)
     */
    private String sessionId;

    /**
     * 工具列表 (可选)
     */
    private List<ToolDefinition> tools;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role; // "user", "assistant", "system"
        private String content;
        private List<ToolCall> toolCalls;
        private String toolCallId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {
        private String id;
        private String name;
        private String arguments; // JSON string
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolDefinition {
        private String name;
        private String description;
        private Map<String, ToolParameter> parameters;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolParameter {
        private String type;
        private String description;
        private Boolean required;
    }
}
