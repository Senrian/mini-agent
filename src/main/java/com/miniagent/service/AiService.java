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
 * AI服务 - 负责与AI API交互
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
            // 构建请求体
            Map<String, Object> body = new HashMap<>();
            body.put("model", aiProperties.getModel());
            body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : aiProperties.getMaxTokens());
            body.put("temperature", request.getTemperature() != null ? request.getTemperature() : aiProperties.getTemperature());
            
            // 构建消息列表
            List<Map<String, Object>> messages = new ArrayList<>();
            
            // 添加系统提示
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
                Map<String, Object> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", request.getSystemPrompt());
                messages.add(systemMsg);
            }
            
            // 添加用户消息
            if (request.getMessages() != null) {
                for (ChatRequest.Message msg : request.getMessages()) {
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("role", msg.getRole());
                    messageMap.put("content", msg.getContent());
                    messages.add(messageMap);
                }
            }
            
            body.put("messages", messages);
            
            // 添加工具定义 (如果有用)
            if (request.getTools() != null && !request.getTools().isEmpty()) {
                body.put("tools", convertTools(request.getTools()));
            }

            String requestBody = objectMapper.writeValueAsString(body);
            
            // 发送请求
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(aiProperties.getBaseUrl() + "/chat/completions"))
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
                throw new RuntimeException("AI API error: " + response.statusCode());
            }
            
        } catch (Exception e) {
            log.error("AI service error", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    /**
     * 发送流式聊天请求 (SSE)
     */
    public void chatStream(ChatRequest request, StreamCallback callback) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", aiProperties.getModel());
            body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : aiProperties.getMaxTokens());
            body.put("temperature", request.getTemperature() != null ? request.getTemperature() : aiProperties.getTemperature());
            body.put("stream", true);
            
            List<Map<String, Object>> messages = new ArrayList<>();
            if (request.getSystemPrompt() != null) {
                messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
            }
            if (request.getMessages() != null) {
                for (ChatRequest.Message msg : request.getMessages()) {
                    messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
                }
            }
            body.put("messages", messages);

            String requestBody = objectMapper.writeValueAsString(body);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(aiProperties.getBaseUrl() + "/chat/completions"))
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
     * 解析API响应
     */
    private ChatResponse parseResponse(String responseBody) throws Exception {
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
        
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setModel((String) response.get("model"));
        
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
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

    /**
     * 流式回调接口
     */
    public interface StreamCallback {
        void onMessage(String data);
        void onComplete();
        void onError(String error);
    }
}
