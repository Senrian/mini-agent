package com.miniagent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt模板服务 - 支持变量替换和模板继承
 */
@Slf4j
@Service
public class PromptTemplateService {

    // 模板模式: {{variable}}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    /**
     * 渲染模板
     */
    public String render(String template, Map<String, String> variables) {
        if (template == null) {
            return null;
        }
        
        StringBuilder result = new StringBuilder();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = variables.get(varName);
            if (value != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
            } else {
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 渲染模板 (使用对象属性)
     */
    public String render(String template, Object context) {
        if (template == null) {
            return null;
        }
        
        Map<String, String> variables = new HashMap<>();
        
        // 反射获取对象的getter方法
        for (var method : context.getClass().getMethods()) {
            if (method.getName().startsWith("get") && 
                method.getParameterCount() == 0 &&
                !method.getName().equals("getClass")) {
                try {
                    Object value = method.invoke(context);
                    if (value != null) {
                        String varName = method.getName().substring(3);
                        varName = varName.substring(0, 1).toLowerCase() + varName.substring(1);
                        variables.put(varName, value.toString());
                    }
                } catch (Exception e) {
                    log.warn("Failed to get property: {}", method.getName());
                }
            }
        }
        
        return render(template, variables);
    }

    /**
     * 提取所有变量名
     */
    public Set<String> extractVariables(String template) {
        Set<String> variables = new HashSet<>();
        if (template == null) {
            return variables;
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        
        return variables;
    }

    /**
     * 检查模板是否包含变量
     */
    public boolean hasVariables(String template) {
        if (template == null) {
            return false;
        }
        return VARIABLE_PATTERN.matcher(template).find();
    }

    /**
     * 内置模板: 系统提示
     */
    public static class Templates {
        public static final String DEFAULT_ASSISTANT = 
            "You are a helpful AI assistant. " +
            "Your name is {{agentName}}. " +
            "Current time: {{currentTime}}. " +
            "Always provide accurate and helpful responses.";
        
        public static final String CODE_REVIEWER =
            "You are an expert code reviewer. " +
            "Your task is to review code and provide constructive feedback. " +
            "Focus on: {{focusAreas}}. " +
            "Be specific and actionable in your feedback.";
        
        public static final String DATA_ANALYST =
            "You are a data analyst. " +
            "Analyze the provided data and explain insights clearly. " +
            "Use visualizations when appropriate. " +
            "Current data context: {{dataContext}}";
    }
}
