package com.miniagent.agent;

import com.miniagent.model.ChatRequest;
import com.miniagent.model.ChatResponse;
import com.miniagent.service.AiService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Agent核心类 - 负责管理对话上下文和AI交互
 */
@Slf4j
@Data
public class MiniAgent {

    private String id;
    private String name;
    private String systemPrompt;
    private List<ChatRequest.Message> history;
    private AiService aiService;
    private Integer maxHistory;

    public MiniAgent(AiService aiService, String systemPrompt) {
        this.id = UUID.randomUUID().toString();
        this.aiService = aiService;
        this.systemPrompt = systemPrompt;
        this.history = new ArrayList<>();
        this.maxHistory = 10;
    }

    /**
     * 发送消息并获取回复
     */
    public String chat(String userMessage) {
        // 添加用户消息
        addMessage("user", userMessage);
        
        // 构建请求
        ChatRequest request = new ChatRequest();
        request.setMessages(new ArrayList<>(history));
        request.setSystemPrompt(systemPrompt);
        
        // 发送请求
        ChatResponse response = aiService.chat(request);
        
        // 添加助手回复
        addMessage("assistant", response.getContent());
        
        return response.getContent();
    }

    /**
     * 带工具调用的对话
     */
    public String chatWithTools(String userMessage, List<ChatRequest.ToolDefinition> tools) {
        addMessage("user", userMessage);
        
        ChatRequest request = new ChatRequest();
        request.setMessages(new ArrayList<>(history));
        request.setSystemPrompt(systemPrompt);
        request.setTools(tools);
        
        // 这里简化处理，实际需要循环调用直到没有tool call
        ChatResponse response = aiService.chat(request);
        
        // 处理工具调用
        if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
            for (ChatResponse.ToolCall toolCall : response.getToolCalls()) {
                // 添加助手消息(含tool calls)
                addMessage("assistant", response.getContent());
                
                // TODO: 执行工具并添加结果
            }
        }
        
        addMessage("assistant", response.getContent());
        return response.getContent();
    }

    /**
     * 添加消息到历史
     */
    private void addMessage(String role, String content) {
        ChatRequest.Message msg = new ChatRequest.Message();
        msg.setRole(role);
        msg.setContent(content);
        history.add(msg);
        
        // 裁剪历史
        while (history.size() > maxHistory * 2) {
            history.remove(0);
        }
    }

    /**
     * 清除历史
     */
    public void clearHistory() {
        history.clear();
    }

    /**
     * 获取历史消息数
     */
    public int getHistorySize() {
        return history.size();
    }
}
