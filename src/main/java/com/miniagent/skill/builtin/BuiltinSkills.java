package com.miniagent.skill.builtin;

import com.miniagent.skill.SkillRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 内置技能工厂 - 对标OpenClaw的skill系统
 * 
 * 提供常用的内置技能:
 * - web_search: 网页搜索
 * - web_extract: 网页抓取
 * - python_execute: Python代码执行
 * - file_read: 读取文件
 * - file_write: 写入文件
 * - calculator: 计算器
 * - weather: 天气查询
 * - news: 新闻获取
 */
@Slf4j
public class BuiltinSkills {

    public static void registerAll(SkillRegistry registry) {
        registry.register(createWebSearchSkill());
        registry.register(createWebExtractSkill());
        registry.register(createPythonExecuteSkill());
        registry.register(createCalculatorSkill());
        registry.register(createFileReadSkill());
        registry.register(createFileWriteSkill());
        
        log.info("Registered {} builtin skills", 6);
    }

    private static SkillRegistry.Skill createWebSearchSkill() {
        SkillRegistry.Skill skill = new SkillRegistry.Skill();
        skill.setName("web_search");
        skill.setDescription("Search the web for information using search engines");
        skill.setTools(Arrays.asList("http_get", "parse_html"));
        
        skill.setSystemPrompt(
            "You have access to web_search skill. " +
            "When users ask for current information, news, or anything that might have changed, " +
            "use the web_search tool to find up-to-date information."
        );
        
        skill.setHandler(params -> {
            String query = (String) params.get("query");
            return Map.of("query", query, "status", "Use WebSearchTool");
        });
        
        return skill;
    }

    private static SkillRegistry.Skill createWebExtractSkill() {
        SkillRegistry.Skill skill = new SkillRegistry.Skill();
        skill.setName("web_extract");
        skill.setDescription("Extract content from a specific URL");
        skill.setTools(Arrays.asList("http_get", "parse_html"));
        
        skill.setSystemPrompt(
            "You have access to web_extract skill. " +
            "Use this when you need to extract specific content from a webpage."
        );
        
        skill.setHandler(params -> {
            String url = (String) params.get("url");
            return Map.of("url", url, "status", "Use WebExtractorTool");
        });
        
        return skill;
    }

    private static SkillRegistry.Skill createPythonExecuteSkill() {
        SkillRegistry.Skill skill = new SkillRegistry.Skill();
        skill.setName("python_execute");
        skill.setDescription("Execute Python code in a sandboxed environment");
        skill.setTools(Arrays.asList("python_sandbox"));
        
        skill.setSystemPrompt(
            "You have access to python_execute skill. " +
            "Use this for:\n" +
            "- Mathematical calculations\n" +
            "- Data analysis\n" +
            "- Text processing\n" +
            "- Code testing\n" +
            "\n" +
            "Note: Some modules are blocked for security."
        );
        
        skill.setHandler(params -> {
            String code = (String) params.get("code");
            return Map.of("code", code, "status", "Use PythonSandbox");
        });
        
        return skill;
    }

    private static SkillRegistry.Skill createCalculatorSkill() {
        SkillRegistry.Skill skill = new SkillRegistry.Skill();
        skill.setName("calculator");
        skill.setDescription("Perform mathematical calculations");
        skill.setTools(Arrays.asList("python_sandbox"));
        
        skill.setSystemPrompt(
            "You have access to calculator skill. " +
            "Use this for complex mathematical calculations that are difficult to do mentally."
        );
        
        skill.setHandler(params -> {
            Object a = params.get("a");
            Object b = params.get("b");
            String op = (String) params.getOrDefault("op", "add");
            
            double result = 0;
            switch (op) {
                case "add": result = toDouble(a) + toDouble(b); break;
                case "sub": result = toDouble(a) - toDouble(b); break;
                case "mul": result = toDouble(a) * toDouble(b); break;
                case "div": result = toDouble(a) / toDouble(b); break;
                default: return Map.of("error", "Unknown operation: " + op);
            }
            
            return Map.of("result", result, "operation", op);
        });
        
        return skill;
    }

    private static SkillRegistry.Skill createFileReadSkill() {
        SkillRegistry.Skill skill = new SkillRegistry.Skill();
        skill.setName("file_read");
        skill.setDescription("Read content from a file");
        skill.setTools(Arrays.asList("file_system"));
        
        skill.setSystemPrompt(
            "You have access to file_read skill. " +
            "Use this to read the content of files when needed."
        );
        
        skill.setHandler(params -> {
            String path = (String) params.get("path");
            return Map.of("path", path, "status", "File read not implemented in demo");
        });
        
        return skill;
    }

    private static SkillRegistry.Skill createFileWriteSkill() {
        SkillRegistry.Skill skill = new SkillRegistry.Skill();
        skill.setName("file_write");
        skill.setDescription("Write content to a file");
        skill.setTools(Arrays.asList("file_system"));
        
        skill.setSystemPrompt(
            "You have access to file_write skill. " +
            "Use this to create or update files."
        );
        
        skill.setHandler(params -> {
            String path = (String) params.get("path");
            String content = (String) params.get("content");
            return Map.of("path", path, "status", "File write not implemented in demo");
        });
        
        return skill;
    }

    private static double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }
}
