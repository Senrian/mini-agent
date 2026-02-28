package com.miniagent.agent;

import com.miniagent.config.AiProperties;
import com.miniagent.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent服务 - 管理多个Agent实例
 */
@Slf4j
@Service
public class AgentService {

    private final AiService aiService;
    private final Map<String, MiniAgent> agents = new ConcurrentHashMap<>();

    public AgentService(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * 创建Agent
     */
    public MiniAgent createAgent(String name, String systemPrompt) {
        MiniAgent agent = new MiniAgent(aiService, systemPrompt);
        agent.setName(name);
        agents.put(agent.getId(), agent);
        log.info("Created agent: {} ({})", name, agent.getId());
        return agent;
    }

    /**
     * 获取Agent
     */
    public MiniAgent getAgent(String agentId) {
        return agents.get(agentId);
    }

    /**
     * 删除Agent
     */
    public void removeAgent(String agentId) {
        agents.remove(agentId);
        log.info("Removed agent: {}", agentId);
    }

    /**
     * 对话
     */
    public String chat(String agentId, String message) {
        MiniAgent agent = agents.get(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Agent not found: " + agentId);
        }
        return agent.chat(message);
    }

    /**
     * 列出所有Agent
     */
    public Map<String, MiniAgent> listAgents() {
        return new ConcurrentHashMap<>(agents);
    }
}
