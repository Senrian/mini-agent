package com.miniagent.config;

import com.miniagent.service.AiService;
import com.miniagent.skill.SkillRegistry;
import com.miniagent.skill.SkillService;
import com.miniagent.skill.builtin.BuiltinSkills;
import com.miniagent.tool.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Skill自动配置 - 启动时注册内置Skills和Tools
 */
@Configuration
public class SkillAutoConfiguration {

    @Bean
    public SkillRegistry skillRegistry() {
        SkillRegistry registry = new SkillRegistry();
        
        // 注册内置Skills
        BuiltinSkills.registerAll(registry);
        
        return registry;
    }

    @Bean
    public SkillService skillService(SkillRegistry skillRegistry) {
        return new SkillService(skillRegistry);
    }

    @Bean
    public Map<String, FnCallAgent.ToolExecutor> toolExecutors() {
        Map<String, FnCallAgent.ToolExecutor> tools = new HashMap<>();
        
        // 注册工具
        tools.put("python_sandbox", new PythonSandbox());
        tools.put("python_executor", new PythonExecutor());
        tools.put("web_search", new WebSearchTool());
        tools.put("web_extractor", new WebExtractorTool());
        
        return tools;
    }

    @Bean
    public CommandLineRunner printBanner() {
        return args -> {
            System.out.println("========================================");
            System.out.println("  Mini Agent Framework v0.0.1");
            System.out.println("========================================");
            System.out.println("  Skills registered: Builtin");
            System.out.println("  Tools available: python_sandbox, web_search, web_extractor");
            System.out.println("========================================");
        };
    }
}
