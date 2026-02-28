package com.miniagent.controller;

import com.miniagent.agent.AgentService;
import com.miniagent.agent.MiniAgent;
import com.miniagent.model.ChatRequest;
import com.miniagent.model.ChatResponse;
import com.miniagent.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;
    private final AiService aiService;

    public AgentController(AgentService agentService, AiService aiService) {
        this.agentService = agentService;
        this.aiService = aiService;
    }

    /**
     * 创建Agent
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAgent(@RequestBody CreateAgentRequest request) {
        try {
            String name = request.getName() != null ? request.getName() : "agent-" + UUID.randomUUID().toString().substring(0, 8);
            String systemPrompt = request.getSystemPrompt() != null ? 
                request.getSystemPrompt() : "You are a helpful AI assistant.";
            
            MiniAgent agent = agentService.createAgent(name, systemPrompt);
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", agent.getId());
            result.put("name", agent.getName());
            result.put("systemPrompt", agent.getSystemPrompt());
            result.put("historySize", agent.getHistorySize());
            result.put("createdAt", new Date());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Create agent error", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取Agent详情
     */
    @GetMapping("/{agentId}")
    public ResponseEntity<Map<String, Object>> getAgent(@PathVariable String agentId) {
        MiniAgent agent = agentService.getAgent(agentId);
        if (agent == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", agent.getId());
        result.put("name", agent.getName());
        result.put("systemPrompt", agent.getSystemPrompt());
        result.put("historySize", agent.getHistorySize());
        result.put("maxHistory", agent.getMaxHistory());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 与Agent对话
     */
    @PostMapping("/{agentId}/chat")
    public ResponseEntity<Map<String, Object>> chat(@PathVariable String agentId, 
                                                     @RequestBody ChatRequest request) {
        try {
            String response = agentService.chat(agentId, request.getMessages().get(0).getContent());
            
            Map<String, Object> result = new HashMap<>();
            result.put("response", response);
            result.put("agentId", agentId);
            result.put("timestamp", new Date());
            
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Chat error", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 流式对话
     */
    @PostMapping(value = "/{agentId}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Void> chatStream(@PathVariable String agentId,
                                          @RequestBody ChatRequest request,
                                          org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter) {
        try {
            MiniAgent agent = agentService.getAgent(agentId);
            if (agent == null) {
                emitter.completeWithError(new IllegalArgumentException("Agent not found"));
                return ResponseEntity.badRequest().build();
            }
            
            // 构建请求
            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setMessages(request.getMessages());
            chatRequest.setSystemPrompt(agent.getSystemPrompt());
            
            aiService.chatStream(chatRequest, new AiService.StreamCallback() {
                @Override
                public void onMessage(String data) {
                    try { emitter.send(data); } catch (Exception e) { log.error("SSE error", e); }
                }

                @Override
                public void onComplete() {
                    try { emitter.complete(); } catch (Exception e) { log.error("SSE complete error", e); }
                }

                @Override
                public void onError(String error) {
                    log.error("SSE error: {}", error);
                    try { emitter.completeWithError(new RuntimeException(error)); } catch (Exception e) {}
                }
            });
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Stream chat error", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 清除Agent历史
     */
    @DeleteMapping("/{agentId}/history")
    public ResponseEntity<Map<String, Object>> clearHistory(@PathVariable String agentId) {
        MiniAgent agent = agentService.getAgent(agentId);
        if (agent == null) {
            return ResponseEntity.notFound().build();
        }
        
        agent.clearHistory();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "History cleared");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 删除Agent
     */
    @DeleteMapping("/{agentId}")
    public ResponseEntity<Map<String, Object>> deleteAgent(@PathVariable String agentId) {
        agentService.removeAgent(agentId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Agent deleted: " + agentId);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 列出所有Agent
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listAgents() {
        List<Map<String, Object>> agents = agentService.listAgents().values().stream()
            .map(agent -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", agent.getId());
                map.put("name", agent.getName());
                map.put("historySize", agent.getHistorySize());
                return map;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(agents);
    }

    /**
     * 更新Agent配置
     */
    @PatchMapping("/{agentId}")
    public ResponseEntity<Map<String, Object>> updateAgent(@PathVariable String agentId,
                                                          @RequestBody UpdateAgentRequest request) {
        MiniAgent agent = agentService.getAgent(agentId);
        if (agent == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (request.getName() != null) {
            agent.setName(request.getName());
        }
        if (request.getSystemPrompt() != null) {
            agent.setSystemPrompt(request.getSystemPrompt());
        }
        if (request.getMaxHistory() != null) {
            agent.setMaxHistory(request.getMaxHistory());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("agent", Map.of(
            "id", agent.getId(),
            "name", agent.getName(),
            "systemPrompt", agent.getSystemPrompt(),
            "maxHistory", agent.getMaxHistory()
        ));
        
        return ResponseEntity.ok(result);
    }

    // Request DTOs
    public static class CreateAgentRequest {
        private String name;
        private String systemPrompt;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    }

    public static class UpdateAgentRequest {
        private String name;
        private String systemPrompt;
        private Integer maxHistory;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public Integer getMaxHistory() { return maxHistory; }
        public void setMaxHistory(Integer maxHistory) { this.maxHistory = maxHistory; }
    }
}
