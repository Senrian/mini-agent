package com.miniagent.agent.coder;

import com.miniagent.model.ChatRequest;
import com.miniagent.model.ChatResponse;
import com.miniagent.service.AiService;
import com.miniagent.tool.PythonSandbox;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Code Agent - 专业的代码助手
 * 
 * 特性:
 * - 代码编写和调试
 * - 代码审查
 * - 解释代码
 * - 代码优化建议
 */
@Slf4j
@Data
public class CodeAgent {

    private String id;
    private String name;
    private String systemPrompt;
    private List<ChatRequest.Message> history;
    private AiService aiService;
    private PythonSandbox sandbox;
    private String language;

    public CodeAgent(AiService aiService) {
        this.id = UUID.randomUUID().toString();
        this.name = "CodeAgent";
        this.aiService = aiService;
        this.sandbox = new PythonSandbox();
        this.systemPrompt = getDefaultPrompt();
        this.history = new ArrayList<>();
        this.language = "python";
    }

    /**
     * 代码审查
     */
    public CodeReviewResult reviewCode(String code, String language) {
        addMessage("user", "Review this " + language + " code:\n" + code);
        
        ChatRequest request = new ChatRequest();
        request.setMessages(new ArrayList<>(history));
        request.setSystemPrompt(getReviewPrompt(language));
        
        ChatResponse response = aiService.chat(request);
        
        CodeReviewResult result = new CodeReviewResult();
        result.setCode(code);
        result.setLanguage(language);
        result.setReview(response.getContent());
        
        // 如果是Python，尝试执行
        if ("python".equalsIgnoreCase(language)) {
            try {
                Map<String, Object> execResult = sandbox.execute(
                    Map.of("code", code)
                );
                result.setExecutionResult(execResult);
            } catch (Exception e) {
                result.setExecutionError(e.getMessage());
            }
        }
        
        addMessage("assistant", response.getContent());
        return result;
    }

    /**
     * 执行代码
     */
    public ExecutionResult execute(String code, String language) {
        ExecutionResult result = new ExecutionResult();
        
        try {
            if ("python".equalsIgnoreCase(language)) {
                Object execResult = sandbox.execute(Map.of("code", code));
                result.setSuccess(true);
                result.setOutput(execResult.toString());
            } else {
                result.setSuccess(false);
                result.setError("Language not supported: " + language);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        
        return result;
    }

    /**
     * 解释代码
     */
    public String explainCode(String code, String language) {
        addMessage("user", "Explain this " + language + " code:\n" + code);
        
        ChatRequest request = new ChatRequest();
        request.setMessages(new ArrayList<>(history));
        request.setSystemPrompt(getExplainPrompt(language));
        
        ChatResponse response = aiService.chat(request);
        addMessage("assistant", response.getContent());
        
        return response.getContent();
    }

    /**
     * 优化代码
     */
    public String optimizeCode(String code, String language) {
        addMessage("user", "Optimize this " + language + " code:\n" + code);
        
        ChatRequest request = new ChatRequest();
        request.setMessages(new ArrayList<>(history));
        request.setSystemPrompt(getOptimizePrompt(language));
        
        ChatResponse response = aiService.chat(request);
        addMessage("assistant", response.getContent());
        
        return response.getContent();
    }

    /**
     * 生成测试
     */
    public String generateTests(String code, String language) {
        addMessage("user", "Generate unit tests for this " + language + " code:\n" + code);
        
        ChatRequest request = new ChatRequest();
        request.setMessages(new ArrayList<>(history));
        request.setSystemPrompt(getTestPrompt(language));
        
        ChatResponse response = aiService.chat(request);
        addMessage("assistant", response.getContent());
        
        return response.getContent();
    }

    /**
     * 对话
     */
    public String chat(String message) {
        addMessage("user", message);
        
        ChatRequest request = new ChatRequest();
        request.setMessages(new ArrayList<>(history));
        request.setSystemPrompt(systemPrompt);
        
        ChatResponse response = aiService.chat(request);
        addMessage("assistant", response.getContent());
        
        return response.getContent();
    }

    private void addMessage(String role, String content) {
        ChatRequest.Message msg = new ChatRequest.Message();
        msg.setRole(role);
        msg.setContent(content);
        history.add(msg);
    }

    private String getDefaultPrompt() {
        return "You are an expert programming assistant. " +
               "Help users write, debug, and understand code. " +
               "Provide clear explanations and efficient solutions.";
    }

    private String getReviewPrompt(String language) {
        return "You are a code reviewer. " +
               "Review the following " + language + " code and provide feedback on:\n" +
               "- Potential bugs\n" +
               "- Code style issues\n" +
               "- Performance improvements\n" +
               "- Security concerns\n" +
               "- Best practices";
    }

    private String getExplainPrompt(String language) {
        return "You are a code explainer. " +
               "Explain the following " + language + " code in clear, simple terms. " +
               "Focus on what the code does and how it works.";
    }

    private String getOptimizePrompt(String language) {
        return "You are a code optimizer. " +
               "Optimize the following " + language + " code for:\n" +
               "- Performance\n" +
               "- Readability\n" +
               "- Memory usage\n" +
               "- Best practices";
    }

    private String getTestPrompt(String language) {
        return "You are a testing expert. " +
               "Generate comprehensive unit tests for the following " + language + " code. " +
               "Use appropriate testing frameworks and cover edge cases.";
    }

    public void clearHistory() {
        history.clear();
    }

    // 结果类
    @Data
    public static class CodeReviewResult {
        private String code;
        private String language;
        private String review;
        private Object executionResult;
        private String executionError;
    }

    @Data
    public static class ExecutionResult {
        private boolean success;
        private String output;
        private String error;
    }
}
