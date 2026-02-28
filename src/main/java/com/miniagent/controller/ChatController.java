package com.miniagent.controller;

import com.miniagent.model.ChatRequest;
import com.miniagent.model.ChatResponse;
import com.miniagent.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AiService aiService;
    
    // 简单的会话存储 (生产环境应使用数据库)
    private final ConcurrentHashMap<String, ChatRequest> sessions = new ConcurrentHashMap<>();

    public ChatController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * 聊天接口
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            log.info("Chat request: sessionId={}", request.getSessionId());
            
            ChatResponse response = aiService.chat(request);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Chat error", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 流式聊天接口 (SSE)
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Void> chatStream(@RequestBody ChatRequest request,
                                           org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter) {
        try {
            log.info("Stream chat request: sessionId={}", request.getSessionId());
            
            aiService.chatStream(request, new AiService.StreamCallback() {
                @Override
                public void onMessage(String data) {
                    try {
                        emitter.send(data);
                    } catch (Exception e) {
                        log.error("SSE send error", e);
                    }
                }

                @Override
                public void onComplete() {
                    try {
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("SSE complete error", e);
                    }
                }

                @Override
                public void onError(String error) {
                    log.error("SSE error: {}", error);
                    try {
                        emitter.completeWithError(new RuntimeException(error));
                    } catch (Exception e) {
                        log.error("SSE error complete error", e);
                    }
                }
            });
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Stream chat error", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 清除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearSession(@PathVariable String sessionId) {
        sessions.remove(sessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Session cleared: " + sessionId);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ChatRequest> getSession(@PathVariable String sessionId) {
        ChatRequest session = sessions.get(sessionId);
        if (session != null) {
            return ResponseEntity.ok(session);
        }
        return ResponseEntity.notFound().build();
    }
}
