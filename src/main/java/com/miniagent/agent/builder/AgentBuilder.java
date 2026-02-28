package com.miniagent.agent.builder;

import com.miniagent.graph.StateGraph;
import com.miniagent.model.ChatRequest;
import com.miniagent.model.ChatResponse;
import com.miniagent.service.AiService;
import com.miniagent.tool.ToolRegistry;

import java.util.*;

/**
 * Agent构建器 - 对标LangChain的create_agent
 * 
 * 特性:
 * - 链式调用
 * - 工具绑定
 * - 状态管理
 * - 流式输出
 */
public class AgentBuilder {

    private String model;
    private String systemPrompt;
    private List<Tool> tools = new ArrayList<>();
    private StateGraph graph;
    private AiService aiService;
    private int maxIterations = 10;
    private boolean streamEnabled = false;

    public AgentBuilder(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * 设置模型
     */
    public AgentBuilder model(String model) {
        this.model = model;
        return this;
    }

    /**
     * 设置系统提示
     */
    public AgentBuilder systemPrompt(String prompt) {
        this.systemPrompt = prompt;
        return this;
    }

    /**
     * 添加工具
     */
    public AgentBuilder tool(Tool tool) {
        this.tools.add(tool);
        return this;
    }

    /**
     * 添加工具(从ToolRegistry)
     */
    public AgentBuilder tools(ToolRegistry registry) {
        for (String name : registry.getAll().keySet()) {
            this.tools.add(createToolFromRegistry(name, registry));
        }
        return this;
    }

    /**
     * 设置最大迭代次数
     */
    public AgentBuilder maxIterations(int max) {
        this.maxIterations = max;
        return this;
    }

    /**
     * 启用流式输出
     */
    public AgentBuilder withStreaming() {
        this.streamEnabled = true;
        return this;
    }

    /**
     * 构建可执行的Agent
     */
    public BuiltAgent build() {
        // 创建StateGraph
        this.graph = new StateGraph("agent-" + System.currentTimeMillis());
        
        // 添加核心节点
        graph.addNode("think", new ThinkNode());
        graph.addNode("act", new ActNode());
        graph.addNode("observe", new ObserveNode());
        
        // 添加边
        graph.addEdge("think", "act");
        graph.addEdge("act", "observe");
        
        // 设置起点和终点
        graph.setStart("think");
        graph.addEnd("observe");
        
        return new BuiltAgent(this);
    }

    private Tool createToolFromRegistry(String name, ToolRegistry registry) {
        ToolRegistry.ToolMethod method = registry.get(name);
        Tool tool = new Tool();
        tool.setName(name);
        tool.setDescription(method.getDescription());
        return tool;
    }

    // 内置节点
    private class ThinkNode implements StateGraph.Node {
        @Override
        public Map<String, Object> execute(Map<String, Object> state) {
            String userMessage = (String) state.get("user_message");
            
            ChatRequest request = new ChatRequest();
            request.setSystemPrompt(systemPrompt);
            request.setMessages(List.of(createMessage("user", userMessage)));
            
            ChatResponse response = aiService.chat(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("ai_thought", response.getContent());
            result.put("has_tool_calls", response.getToolCalls() != null && !response.getToolCalls().isEmpty());
            
            return result;
        }
    }

    private class ActNode implements StateGraph.Node {
        @Override
        public Map<String, Object> execute(Map<String, Object> state) {
            Boolean hasToolCalls = (Boolean) state.get("has_tool_calls");
            
            if (Boolean.TRUE.equals(hasToolCalls)) {
                // 执行工具
                // ...
            }
            
            return Map.of("action_taken", true);
        }
    }

    private class ObserveNode implements StateGraph.Node {
        @Override
        public Map<String, Object> execute(Map<String, Object> state) {
            return Map.of("observation", "completed");
        }
    }

    private ChatRequest.Message createMessage(String role, String content) {
        ChatRequest.Message msg = new ChatRequest.Message();
        msg.setRole(role);
        msg.setContent(content);
        return msg;
    }

    /**
     * 构建后的Agent
     */
    public static class BuiltAgent {
        private final AgentBuilder builder;

        public BuiltAgent(AgentBuilder builder) {
            this.builder = builder;
        }

        /**
         * 执行Agent
         */
        public AgentResult invoke(String message) {
            Map<String, Object> input = new HashMap<>();
            input.put("user_message", message);
            
            StateGraph.CompiledGraph compiled = builder.graph.compile();
            Map<String, Object> result = compiled.invoke(input);
            
            AgentResult agentResult = new AgentResult();
            agentResult.setResponse((String) result.get("ai_thought"));
            agentResult.setIterations(1);
            agentResult.setToolCalls((List) result.get("tool_calls"));
            
            return agentResult;
        }

        /**
         * 流式执行
         */
        public void invokeStream(String message, StreamCallback callback) {
            // 实现流式
        }
    }

    @Data
    public static class AgentResult {
        private String response;
        private int iterations;
        private List<Object> toolCalls;
    }

    public interface StreamCallback {
        void onChunk(String chunk);
        void onComplete(AgentResult result);
        void onError(String error);
    }

    /**
     * 工具定义 - 对标LangChain的Tool
     */
    @Data
    public static class Tool {
        private String name;
        private String description;
        private String schema; // JSON Schema
        private boolean returnDirect = false;
    }
}
