package com.miniagent.memory;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 对话记忆 - 对标LangGraph/LangChain的ConversationMemory
 * 
 * 支持:
 * - 短时记忆 (当前会话)
 * - 长时记忆 (持久化)
 * - 消息摘要
 * - 窗口管理
 */
@Slf4j
public class ChatMemory {

    private final List<Message> messages = new CopyOnWriteArrayList<>();
    private final Map<String, Object> context = new ConcurrentHashMap<>();
    private final int maxMessages;
    private final boolean summarize;
    private String summary = "";
    
    // 长时记忆
    private final List<MemoryItem> longTermMemory = new CopyOnWriteArrayList<>();

    public ChatMemory(int maxMessages) {
        this.maxMessages = maxMessages;
        this.summarize = true;
    }

    public ChatMemory(int maxMessages, boolean summarize) {
        this.maxMessages = maxMessages;
        this.summarize = summarize;
    }

    /**
     * 添加消息
     */
    public void addMessage(String role, String content) {
        Message msg = new Message(role, content);
        messages.add(msg);
        
        // 添加到长时记忆
        if (role.equals("user") || role.equals("assistant")) {
            addToLongTermMemory(role, content);
        }
        
        // 裁剪
        if (messages.size() > maxMessages) {
            if (summarize) {
                summarizeOldMessages();
            } else {
                trimMessages();
            }
        }
    }

    /**
     * 添加用户消息
     */
    public void addUserMessage(String content) {
        addMessage("user", content);
    }

    /**
     * 添加助手消息
     */
    public void addAIResponse(String content) {
        addMessage("assistant", content);
    }

    /**
     * 添加系统消息
     */
    public void addSystemMessage(String content) {
        addMessage("system", content);
    }

    /**
     * 获取消息列表
     */
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * 获取最近N条消息
     */
    public List<Message> getRecentMessages(int count) {
        int size = messages.size();
        int from = Math.max(0, size - count);
        return new ArrayList<>(messages.subList(from, size));
    }

    /**
     * 获取带摘要的消息
     */
    public List<Message> getMessagesWithSummary() {
        List<Message> result = new ArrayList<>();
        
        if (summary != null && !summary.isEmpty()) {
            result.add(new Message("system", "[Summary of previous conversation]: " + summary));
        }
        
        result.addAll(messages);
        return result;
    }

    /**
     * 获取长时记忆
     */
    public List<MemoryItem> getLongTermMemory() {
        return new ArrayList<>(longTermMemory);
    }

    /**
     * 搜索长时记忆
     */
    public List<MemoryItem> searchLongTermMemory(String query) {
        List<MemoryItem> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        for (MemoryItem item : longTermMemory) {
            if (item.getContent().toLowerCase().contains(lowerQuery)) {
                results.add(item);
            }
        }
        
        return results;
    }

    /**
     * 添加到长时记忆
     */
    private void addToLongTermMemory(String role, String content) {
        // 只保留重要信息
        if (content.length() > 20) {
            MemoryItem item = new MemoryItem(role, content);
            longTermMemory.add(item);
        }
    }

    /**
     * 摘要旧消息
     */
    private void summarizeOldMessages() {
        if (messages.size() < maxMessages) return;
        
        // 保留最近的一半
        int keepCount = maxMessages / 2;
        List<Message> toSummarize = messages.subList(0, messages.size() - keepCount);
        
        // 简单摘要 (实际应该调用LLM)
        StringBuilder sb = new StringBuilder();
        for (Message msg : toSummarize) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent().substring(0, Math.min(50, msg.getContent().length()))).append("...; ");
        }
        
        summary = sb.toString();
        
        // 移除已摘要的消息
        toSummarize.clear();
    }

    /**
     * 裁剪消息
     */
    private void trimMessages() {
        int removeCount = messages.size() - maxMessages + 1;
        for (int i = 0; i < removeCount && !messages.isEmpty(); i++) {
            messages.remove(0);
        }
    }

    /**
     * 清除所有记忆
     */
    public void clear() {
        messages.clear();
        summary = "";
    }

    /**
     * 清除长时记忆
     */
    public void clearLongTermMemory() {
        longTermMemory.clear();
    }

    /**
     * 获取大小
     */
    public int size() {
        return messages.size();
    }

    /**
     * 获取令牌数(估算)
     */
    public int estimateTokens() {
        int tokens = 0;
        for (Message msg : messages) {
            tokens += msg.getContent().length() / 4; // 简单估算
        }
        if (summary != null) {
            tokens += summary.length() / 4;
        }
        return tokens;
    }

    // 数据类
    @Data
    public static class Message {
        private final String role;
        private final String content;
        private final long timestamp;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }

    @Data
    public static class MemoryItem {
        private final String role;
        private final String content;
        private final long timestamp;
        private final Map<String, Object> metadata = new HashMap<>();

        public MemoryItem(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }

        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }
    }
}
