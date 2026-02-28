package com.miniagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniagent.config.AiProperties;
import com.miniagent.model.ChatRequest;
import com.miniagent.model.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * AI服务 - 支持多种LLM provider
 */
@Slf4j
@Service
public class AiService {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(aiProperties.getTimeout()))
                .build();
    }

    /**
     * 发送聊天请求
     */
    public ChatResponse chat(ChatRequest request) {
        try {
            Map<String, Object> body = buildRequestBody(request);
            String requestBody = objectMapper.writeValueAsString(body);
            
            String endpoint = aiProperties.getChatEndpoint();
            log.debug("Calling AI: {} with model: {}", endpoint, aiProperties.getModel());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMillis(aiProperties.getTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseResponse(response.body());
            } else {
                log.error("AI API error: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("AI API error: " + response.statusCode() + " - " + response.body());
            }
            
        } catch (Exception e) {
            log.error("AI service error", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getModel());
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : aiProperties.getMaxTokens());
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : aiProperties.getTemperature());
        
        // DeepSeek支持
        if ("deepseek".equalsIgnoreCase(aiProperties.getProvider())) {
            body.put("stream", false);
        }
        
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // 系统提示
        String systemPrompt = request.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            systemPrompt = aiProperties.getSystemPrompt();
        }
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }
        
        // 用户消息
        if (request.getMessages() != null) {
            for (ChatRequest.Message msg : request.getMessages()) {
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("role", msg.getRole());
                messageMap.put("content", msg.getContent());
                messages.add(messageMap);
            }
        }
        
        body.put("messages", messages);
        
        // 添加工具
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", convertTools(request.getTools()));
            if ("deepseek".equalsIgnoreCase(aiProperties.getProvider())) {
                body.put("parallel_tool_calls", true);
            }
        }
        
        return body;
    }

    /**
     * 发送流式请求
     */
    public void chatStream(ChatRequest request, StreamCallback callback) {
        try {
            Map<String, Object> body = buildRequestBody(request);
            body.put("stream", true);

            String requestBody = objectMapper.writeValueAsString(body);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(aiProperties.getChatEndpoint()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMillis(aiProperties.getTimeout()))
                    .build();

            httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines())
                    .thenAccept(response -> {
                        response.body().forEach(line -> {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);
                                if (!data.equals("[DONE]")) {
                                    try {
                                        callback.onMessage(data);
                                    } catch (Exception e) {
                                        log.error("Stream callback error", e);
                                    }
                                }
                            }
                        });
                        callback.onComplete();
                    })
                    .exceptionally(e -> {
                        callback.onError(e.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            log.error("AI stream service error", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * 解析响应
     */
    private ChatResponse parseResponse(String responseBody) throws Exception {
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
        
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setModel((String) response.get("model"));
        
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            
            // 处理DeepSeek的reasoning_content
            if (message.containsKey("reasoning_content")) {
                chatResponse.setThinking((String) message.get("reasoning_content"));
            }
            
            chatResponse.setContent((String) message.get("content"));
            chatResponse.setFinishReason((String) choice.get("finish_reason"));
        }
        
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        if (usage != null) {
            chatResponse.setInputTokens((Integer) usage.get("prompt_tokens"));
            chatResponse.setOutputTokens((Integer) usage.get("completion_tokens"));
            chatResponse.setTotalTokens((Integer) usage.get("total_tokens"));
        }
        
        return chatResponse;
    }

    /**
     * 转换工具定义
     */
    private List<Map<String, Object>> convertTools(List<ChatRequest.ToolDefinition> tools) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatRequest.ToolDefinition tool : tools) {
            Map<String, Object> toolMap = new HashMap<>();
            Map<String, Object> function = new HashMap<>();
            function.put("name", tool.getName());
            function.put("description", tool.getDescription());
            
            if (tool.getParameters() != null) {
                Map<String, Object> params = new HashMap<>();
                params.put("type", "object");
                params.put("properties", tool.getParameters());
                function.put("parameters", params);
            }
            
            toolMap.put("type", "function");
            toolMap.put("function", function);
            result.add(toolMap);
        }
        return result;
    }

    public interface StreamCallback {
        void onMessage(String data);
        void onComplete();
        void onError(String error);
    }
}
