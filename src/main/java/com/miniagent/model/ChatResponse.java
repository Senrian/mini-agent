package com.miniagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI聊天响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * AI回复内容
     */
    private String content;

    /**
     * 消耗的输入token
     */
    private Integer inputTokens;

    /**
     * 消耗的输出token
     */
    private Integer outputTokens;

    /**
     * 总token消耗
     */
    private Integer totalTokens;

    /**
     * 完成原因 (stop, length, etc.)
     */
    private String finishReason;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 工具调用列表
     */
    private List<ToolCall> toolCalls;

    /**
     * 思考过程 (如果启用)
     */
    private String thinking;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {
        private String id;
        private String name;
        private String arguments;
    }
}
