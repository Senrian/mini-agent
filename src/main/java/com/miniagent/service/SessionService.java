package com.miniagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniagent.model.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理服务
 */
@Slf4j
@Service
public class SessionService {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final int DEFAULT_MAX_MESSAGES = 100;

    /**
     * 创建会话
     */
    public Session createSession(String systemPrompt) {
        Session session = new Session(UUID.randomUUID().toString(), systemPrompt);
        sessions.put(session.getId(), session);
        log.info("Created session: {}", session.getId());
        return session;
    }

    /**
     * 获取会话
     */
    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 添加消息
     */
    public void addMessage(String sessionId, ChatRequest.Message message) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.addMessage(message);
        }
    }

    /**
     * 获取消息历史
     */
    public List<ChatRequest.Message> getMessages(String sessionId) {
        Session session = sessions.get(sessionId);
        return session != null ? session.getMessages() : Collections.emptyList();
    }

    /**
     * 清除会话
     */
    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("Cleared session: {}", sessionId);
    }

    /**
     * 会话类
     */
    public static class Session {
        private final String id;
        private final String systemPrompt;
        private final List<ChatRequest.Message> messages;
        private final long createdAt;
        private long lastActiveAt;

        public Session(String id, String systemPrompt) {
            this.id = id;
            this.systemPrompt = systemPrompt;
            this.messages = new ArrayList<>();
            this.createdAt = System.currentTimeMillis();
            this.lastActiveAt = System.currentTimeMillis();
        }

        public void addMessage(ChatRequest.Message message) {
            messages.add(message);
            lastActiveAt = System.currentTimeMillis();
            
            // 限制消息数量
            while (messages.size() > DEFAULT_MAX_MESSAGES) {
                messages.remove(0);
            }
        }

        // Getters
        public String getId() { return id; }
        public String getSystemPrompt() { return systemPrompt; }
        public List<ChatRequest.Message> getMessages() { return new ArrayList<>(messages); }
        public long getCreatedAt() { return createdAt; }
        public long getLastActiveAt() { return lastActiveAt; }
    }
}
