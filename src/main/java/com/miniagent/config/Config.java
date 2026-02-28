package com.miniagent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用配置属性
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class Config {
}
