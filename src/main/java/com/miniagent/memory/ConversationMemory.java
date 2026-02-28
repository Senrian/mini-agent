package com.miniagent.memory;

import com.miniagent.model.ChatRequest;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 对话记忆 - 管理对话历史
 * 对标 Qwen-Agent 的 memory 模块
 */
@Data
public class ConversationMemory {

    private List<Message> messages;
    private int maxSize;
    private String summary;

    public ConversationMemory(int maxSize) {
        this.maxSize = maxSize;
        this.messages = new LinkedList<>();
    }

    public ConversationMemory() {
        this(100);
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
    public void addAssistantMessage(String content) {
        addMessage("assistant", content);
    }

    /**
     * 添加系统消息
     */
    public void addSystemMessage(String content) {
        addMessage("system", content);
    }

    /**
     * 添加消息
     */
    private void addMessage(String role, String content) {
        Message msg = new Message(role, content);
        messages.add(msg);
        
        // 裁剪
        while (messages.size() > maxSize) {
            messages.remove(0);
        }
    }

    /**
     * 获取消息列表 (转换为ChatRequest格式)
     */
    public List<ChatRequest.Message> toChatMessages() {
        List<ChatRequest.Message> result = new ArrayList<>();
        for (Message msg : messages) {
            ChatRequest.Message m = new ChatRequest.Message();
            m.setRole(msg.getRole());
            m.setContent(msg.getContent());
            result.add(m);
        }
        return result;
    }

    /**
     * 获取最近N条消息
     */
    public List<ChatRequest.Message> getRecentMessages(int count) {
        List<ChatRequest.Message> result = new ArrayList<>();
        int start = Math.max(0, messages.size() - count);
        for (int i = start; i < messages.size(); i++) {
            Message msg = messages.get(i);
            ChatRequest.Message m = new ChatRequest.Message();
            m.setRole(msg.getRole());
            m.setContent(msg.getContent());
            result.add(m);
        }
        return result;
    }

    /**
     * 清除记忆
     */
    public void clear() {
        messages.clear();
        summary = null;
    }

    /**
     * 获取大小
     */
    public int size() {
        return messages.size();
    }

    /**
     * 消息类
     */
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
}
