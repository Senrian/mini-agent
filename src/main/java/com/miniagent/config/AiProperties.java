package com.miniagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI配置属性 - 支持多模型
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String apiKey;
    private String baseUrl = "";
    private String model = "deepseek-chat";
    private Integer maxTokens = 4096;
    private Double temperature = 0.7;
    private Integer timeout = 120000;
    private String provider = "deepseek";
    private String systemPrompt = "You are a helpful AI assistant.";

    public String getEffectiveBaseUrl() {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl;
        }
        switch (provider.toLowerCase()) {
            case "deepseek": return "https://api.deepseek.com/v1";
            case "openai": return "https://api.openai.com/v1";
            case "minimax": return "https://api.minimaxi.com/v1";
            default: return "https://api.deepseek.com/v1";
        }
    }

    public String getChatEndpoint() {
        return getEffectiveBaseUrl() + "/chat/completions";
    }
}
