package com.miniagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API基础URL
     */
    private String baseUrl = "https://api.minimaxi.com/v1";

    /**
     * 模型名称
     */
    private String model = "MiniMax-M2.5";

    /**
     * 最大token数
     */
    private Integer maxTokens = 4096;

    /**
     * 温度参数 (0-2)
     */
    private Double temperature = 0.7;

    /**
     * 请求超时时间(毫秒)
     */
    private Integer timeout = 60000;
}
