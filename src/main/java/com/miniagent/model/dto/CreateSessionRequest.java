package com.miniagent.model.dto;

import lombok.Data;

/**
 * 创建会话请求
 */
@Data
public class CreateSessionRequest {
    
    /**
     * 系统提示
     */
    private String systemPrompt;
    
    /**
     * 会话名称
     */
    private String name;
    
    /**
     * 最大历史消息数
     */
    private Integer maxHistory;
}
