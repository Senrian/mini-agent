package com.miniagent.graph;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 状态图 - 对标LangGraph的StateGraph
 * 
 * 核心概念:
 * - Node: 执行单元
 * - Edge: 连接边
 * - State: 状态数据
 * - Router: 路由决策
 */
@Slf4j
public class StateGraph {

    private final String name;
    private final Map<String, Node> nodes = new ConcurrentHashMap<>();
    private final Map<String, List<Edge>> edges = new ConcurrentHashMap<>();
    private final Map<String, Object> state = new ConcurrentHashMap<>();
    private String startNode;
    private Set<String> endNodes = new HashSet<>();

    public StateGraph(String name) {
        this.name = name;
    }

    /**
     * 添加节点
     */
    public StateGraph addNode(String name, Node node) {
        nodes.put(name, node);
        edges.putIfAbsent(name, new ArrayList<>());
        log.debug("Added node: {}", name);
        return this;
    }

    /**
     * 添加边 (无条件)
     */
    public StateGraph addEdge(String from, String to) {
        edges.computeIfAbsent(from, k -> new ArrayList<>()).add(new Edge(to, null));
        return this;
    }

    /**
     * 添加条件边 (对标LangGraph的conditional_edge)
     */
    public StateGraph addConditionalEdge(String from, Router router, Map<String, String> routes) {
        edges.computeIfAbsent(from, k -> new ArrayList<>()).add(new Edge(null, router, routes));
        return this;
    }

    /**
     * 设置起点
     */
    public StateGraph setStart(String nodeName) {
        this.startNode = nodeName;
        return this;
    }

    /**
     * 设置终点
     */
    public StateGraph addEnd(String nodeName) {
        this.endNodes.add(nodeName);
        return this;
    }

    /**
     * 编译图 (对标compile())
     */
    public CompiledGraph compile() {
        if (startNode == null) {
            throw new IllegalStateException("Start node not set");
        }
        if (!nodes.containsKey(startNode)) {
            throw new IllegalStateException("Start node not found: " + startNode);
        }
        return new CompiledGraph(this);
    }

    /**
     * 执行图
     */
    public Map<String, Object> invoke(Map<String, Object> input) {
        state.putAll(input);
        String current = startNode;
        
        int maxIterations = 50;
        int iter = 0;
        
        while (current != null && iter < maxIterations) {
            iter++;
            Node node = nodes.get(current);
            
            if (node == null) {
                log.error("Node not found: {}", current);
                break;
            }
            
            log.debug("Executing node: {}", current);
            Map<String, Object> result = node.execute(state);
            
            if (result != null) {
                state.putAll(result);
            }
            
            // 检查是否是终点
            if (endNodes.contains(current)) {
                break;
            }
            
            // 找下一节点
            current = findNextNode(current);
        }
        
        return new HashMap<>(state);
    }

    private String findNextNode(String current) {
        List<Edge> edgeList = edges.get(current);
        if (edgeList == null || edgeList.isEmpty()) {
            return null;
        }
        
        Edge edge = edgeList.get(0);
        
        // 条件边
        if (edge.router != null) {
            String routeKey = edge.router.route(state);
            return edge.routes.get(routeKey);
        }
        
        // 无条件边
        return edge.target;
    }

    // Getters
    public String getName() { return name; }
    public Map<String, Node> getNodes() { return new HashMap<>(nodes); }
    public String getStartNode() { return startNode; }
    public Set<String> getEndNodes() { return new HashSet<>(endNodes); }

    /**
     * 节点接口
     */
    public interface Node {
        Map<String, Object> execute(Map<String, Object> state);
    }

    /**
     * 路由接口
     */
    public interface Router {
        String route(Map<String, Object> state);
    }

    /**
     * 边
     */
    @Data
    public static class Edge {
        private final String target;
        private final Router router;
        private final Map<String, String> routes;

        public Edge(String target, Router router, Map<String, String> routes) {
            this.target = target;
            this.router = router;
            this.routes = routes;
        }
    }

    /**
     * 编译后的图
     */
    public static class CompiledGraph {
        private final StateGraph graph;

        public CompiledGraph(StateGraph graph) {
            this.graph = graph;
        }

        public Map<String, Object> invoke(Map<String, Object> input) {
            return graph.invoke(input);
        }

        public Map<String, Object> invokeWithHistory(Map<String, Object> input, List<Map<String, Object>> history) {
            Map<String, Object> state = new HashMap<>();
            state.put("history", history);
            state.putAll(input);
            return graph.invoke(state);
        }
    }
}
