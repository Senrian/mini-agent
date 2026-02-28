package com.miniagent.agent;

import com.miniagent.model.ChatRequest;
import com.miniagent.model.ChatResponse;
import com.miniagent.service.AiService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Function Calling Agent - 支持工具调用的Agent
 * 对标 Qwen-Agent 的 FnCall Agent
 */
@Slf4j
@Data
public class FnCallAgent {

    private String id;
    private String name;
    private String systemPrompt;
    private List<ChatRequest.Message> history;
    private AiService aiService;
    private Map<String, ToolExecutor> tools;
    private Integer maxIterations;

    public FnCallAgent(AiService aiService, String systemPrompt, Map<String, ToolExecutor> tools) {
        this.id = UUID.randomUUID().toString();
        this.aiService = aiService;
        this.systemPrompt = systemPrompt;
        this.history = new ArrayList<>();
        this.tools = tools;
        this.maxIterations = 5;
    }

    /**
     * 执行对话（带工具调用）
     */
    public AgentResult chat(String userMessage) {
        addMessage("user", userMessage);
        
        int iteration = 0;
        String lastContent = null;
        
        while (iteration < maxIterations) {
            iteration++;
            
            // 构建请求
            ChatRequest request = new ChatRequest();
            request.setMessages(new ArrayList<>(history));
            request.setSystemPrompt(buildFnCallSystemPrompt());
            request.setTools(convertTools(tools));
            
            // 发送请求
            ChatResponse response = aiService.chat(request);
            
            String content = response.getContent();
            List<ChatResponse.ToolCall> toolCalls = response.getToolCalls();
            
            // 如果有工具调用
            if (toolCalls != null && !toolCalls.isEmpty()) {
                // 添加助手消息(带tool calls)
                ChatRequest.Message assistantMsg = new ChatRequest.Message();
                assistantMsg.setRole("assistant");
                assistantMsg.setContent(content);
                history.add(assistantMsg);
                
                // 执行工具
                for (ChatResponse.ToolCall toolCall : toolCalls) {
                    String toolName = toolCall.getName();
                    String args = toolCall.getArguments();
                    
                    ToolExecutor executor = tools.get(toolName);
                    if (executor != null) {
                        try {
                            Object result = executor.execute(parseJson(args));
                            
                            // 添加工具结果
                            ChatRequest.Message toolMsg = new ChatRequest.Message();
                            toolMsg.setRole("tool");
                            toolMsg.setToolCallId(toolCall.getId());
                            toolMsg.setContent(formatToolResult(toolName, result));
                            history.add(toolMsg);
                            
                            log.info("Tool executed: {} -> {}", toolName, result);
                        } catch (Exception e) {
                            log.error("Tool execution failed: {}", toolName, e);
                            
                            ChatRequest.Message errorMsg = new ChatRequest.Message();
                            errorMsg.setRole("tool");
                            errorMsg.setToolCallId(toolCall.getId());
                            errorMsg.setContent("Error: " + e.getMessage());
                            history.add(errorMsg);
                        }
                    } else {
                        log.warn("Tool not found: {}", toolName);
                    }
                }
                
                // 继续循环，让AI根据工具结果生成回复
                lastContent = null;
            } else {
                // 没有工具调用，直接返回结果
                lastContent = content;
                addMessage("assistant", content);
                break;
            }
        }
        
        if (lastContent == null) {
            lastContent = "已达到最大迭代次数";
        }
        
        return new AgentResult(lastContent, iteration);
    }

    /**
     * 构建函数调用系统提示
     */
    private String buildFnCallSystemPrompt() {
        return systemPrompt + "\n\n" +
            "You have access to the following tools:\n\n" +
            buildToolsDescription() + "\n\n" +
            "When you need to call a function, use the tool_calls format.\n" +
            "After getting the tool results, generate your final response.";
    }

    /**
     * 构建工具描述
     */
    private String buildToolsDescription() {
        if (tools == null || tools.isEmpty()) {
            return "No tools available.";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ToolExecutor> entry : tools.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ")
              .append(entry.getValue().getDescription()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 转换工具定义
     */
    private List<ChatRequest.ToolDefinition> convertTools(Map<String, ToolExecutor> tools) {
        List<ChatRequest.ToolDefinition> result = new ArrayList<>();
        if (tools == null) return result;
        
        for (Map.Entry<String, ToolExecutor> entry : tools.entrySet()) {
            ChatRequest.ToolDefinition def = new ChatRequest.ToolDefinition();
            def.setName(entry.getKey());
            def.setDescription(entry.getValue().getDescription());
            def.setParameters(entry.getValue().getParameters());
            result.add(def);
        }
        return result;
    }

    /**
     * 解析JSON
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse JSON: {}", json);
            return Map.of();
        }
    }

    /**
     * 格式化工具结果
     */
    private String formatToolResult(String toolName, Object result) {
        return "Tool " + toolName + " result: " + (result != null ? result.toString() : "null");
    }

    /**
     * 添加消息
     */
    private void addMessage(String role, String content) {
        ChatRequest.Message msg = new ChatRequest.Message();
        msg.setRole(role);
        msg.setContent(content);
        history.add(msg);
    }

    /**
     * 清除历史
     */
    public void clearHistory() {
        history.clear();
    }

    /**
     * 工具执行器接口
     */
    public interface ToolExecutor {
        Object execute(Map<String, Object> params) throws Exception;
        String getDescription();
        Map<String, ChatRequest.ToolParameter> getParameters();
    }

    /**
     * Agent执行结果
     */
    @Data
    public static class AgentResult {
        private final String response;
        private final int iterations;
    }
}
