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
 * ReAct Agent - 推理+行动 Agent
 * 对标 Qwen-Agent 的 ReAct Chat
 * 
 * 采用 ReAct (Reasoning + Acting) 模式:
 * 1. Thought: 思考当前情况
 * 2. Action: 执行工具
 * 3. Observation: 观察结果
 * 4. 重复直到完成任务
 */
@Slf4j
@Data
public class ReActAgent {

    private String id;
    private String name;
    private String systemPrompt;
    private List<ChatRequest.Message> history;
    private List<ReActStep> reasoningTrace;
    private AiService aiService;
    private Map<String, FnCallAgent.ToolExecutor> tools;
    private Integer maxIterations;

    public ReActAgent(AiService aiService, String systemPrompt, Map<String, FnCallAgent.ToolExecutor> tools) {
        this.id = UUID.randomUUID().toString();
        this.aiService = aiService;
        this.systemPrompt = systemPrompt;
        this.history = new ArrayList<>();
        this.reasoningTrace = new ArrayList<>();
        this.tools = tools;
        this.maxIterations = 10;
    }

    /**
     * 执行ReAct对话
     */
    public ReActResult chat(String userMessage) {
        reasoningTrace.clear();
        addMessage("user", userMessage);
        
        String userPrompt = buildReActPrompt(userMessage);
        
        int iteration = 0;
        String finalAnswer = null;
        
        while (iteration < maxIterations) {
            iteration++;
            
            // 构建请求
            ChatRequest request = new ChatRequest();
            request.setMessages(buildMessages(userPrompt));
            request.setSystemPrompt(getReActSystemPrompt());
            
            // 发送请求
            ChatResponse response = aiService.chat(request);
            String content = response.getContent();
            
            // 解析ReAct步骤
            ReActStep step = parseReActStep(content);
            
            if (step == null) {
                // 没有有效的ReAct步骤，可能是最终答案
                finalAnswer = content;
                addMessage("assistant", content);
                break;
            }
            
            reasoningTrace.add(step);
            
            if ("finish".equalsIgnoreCase(step.getAction())) {
                // 完成任务
                finalAnswer = step.getObservation();
                addMessage("assistant", finalAnswer);
                break;
            }
            
            // 执行工具
            if (tools != null && tools.containsKey(step.getAction())) {
                try {
                    Map<String, Object> params = parseActionInput(step.getActionInput());
                    Object result = tools.get(step.getAction()).execute(params);
                    step.setObservation("Result: " + (result != null ? result.toString() : "null"));
                } catch (Exception e) {
                    step.setObservation("Error: " + e.getMessage());
                }
            } else if (!"finish".equalsIgnoreCase(step.getAction())) {
                step.setObservation("Unknown action: " + step.getAction());
            }
            
            // 添加思考到历史
            userPrompt += "\n\n" + formatReActStep(step);
        }
        
        if (finalAnswer == null) {
            finalAnswer = "已达到最大迭代次数";
        }
        
        return new ReActResult(finalAnswer, new ArrayList<>(reasoningTrace), iteration);
    }

    /**
     * 构建ReAct提示
     */
    private String buildReActPrompt(String userMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(userMessage).append("\n\n");
        
        // 添加之前的思考
        for (ReActStep step : reasoningTrace) {
            sb.append(formatReActStep(step));
        }
        
        return sb.toString();
    }

    /**
     * 构建消息列表
     */
    private List<ChatRequest.Message> buildMessages(String userPrompt) {
        List<ChatRequest.Message> messages = new ArrayList<>();
        
        // 系统消息
        ChatRequest.Message systemMsg = new ChatRequest.Message();
        systemMsg.setRole("system");
        systemMsg.setContent(getReActSystemPrompt());
        messages.add(systemMsg);
        
        // 用户消息
        ChatRequest.Message userMsg = new ChatRequest.Message();
        userMsg.setRole("user");
        userMsg.setContent(userPrompt);
        messages.add(userMsg);
        
        return messages;
    }

    /**
     * ReAct系统提示
     */
    private String getReActSystemPrompt() {
        return systemPrompt + "\n\n" +
            "You are a ReAct agent. Follow this format:\n\n" +
            "Thought: [your reasoning about what to do next]\n" +
            "Action: [tool name to use, or 'finish' if done]\n" +
            "Action Input: [input to the tool in JSON format]\n" +
            "Observation: [result from the tool]\n\n" +
            "Available tools: " + getAvailableTools() + "\n\n" +
            "Repeat Thought->Action->Action Input->Observation until you can answer the question.\n" +
            "When done, use 'finish' as the action with your final answer in the observation.";
    }

    /**
     * 获取可用工具列表
     */
    private String getAvailableTools() {
        if (tools == null || tools.isEmpty()) {
            return "none";
        }
        return String.join(", ", tools.keySet());
    }

    /**
     * 解析ReAct步骤
     */
    private ReActStep parseReActStep(String content) {
        if (content == null) return null;
        
        ReActStep step = new ReActStep();
        
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Thought:")) {
                step.setThought(line.substring("Thought:".length()).trim());
            } else if (line.startsWith("Action:")) {
                step.setAction(line.substring("Action:".length()).trim());
            } else if (line.startsWith("Action Input:")) {
                step.setActionInput(line.substring("Action Input:".length()).trim());
            } else if (line.startsWith("Observation:")) {
                step.setObservation(line.substring("Observation:".length()).trim());
            }
        }
        
        // 必须有action才算有效步骤
        if (step.getAction() == null || step.getAction().isEmpty()) {
            return null;
        }
        
        return step;
    }

    /**
     * 格式化ReAct步骤
     */
    private String formatReActStep(ReActStep step) {
        StringBuilder sb = new StringBuilder();
        if (step.getThought() != null) {
            sb.append("Thought: ").append(step.getThought()).append("\n");
        }
        if (step.getAction() != null) {
            sb.append("Action: ").append(step.getAction()).append("\n");
        }
        if (step.getActionInput() != null) {
            sb.append("Action Input: ").append(step.getActionInput()).append("\n");
        }
        if (step.getObservation() != null) {
            sb.append("Observation: ").append(step.getObservation()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 解析action input
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseActionInput(String input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(input, Map.class);
        } catch (Exception e) {
            return Map.of("input", input);
        }
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
     * ReAct步骤
     */
    @Data
    public static class ReActStep {
        private String thought;
        private String action;
        private String actionInput;
        private String observation;
    }

    /**
     * 执行结果
     */
    @Data
    public static class ReActResult {
        private final String answer;
        private final List<ReActStep> reasoningTrace;
        private final int iterations;
    }
}
