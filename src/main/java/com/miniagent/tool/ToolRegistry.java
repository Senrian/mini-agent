package com.miniagent.tool;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 工具注册表 - 管理可用的工具
 */
public class ToolRegistry {

    private final Map<String, ToolMethod> tools = new HashMap<>();

    /**
     * 注册工具
     */
    public void register(String name, String description, Function<Map<String, Object>, Object> handler) {
        tools.put(name, new ToolMethod(name, description, handler));
    }

    /**
     * 注册Bean的方法作为工具
     */
    public void registerBean(String name, String description, Object bean, Method method) {
        method.setAccessible(true);
        tools.put(name, new ToolMethod(name, description, (params) -> {
            try {
                return method.invoke(bean, params);
            } catch (Exception e) {
                throw new RuntimeException("Tool execution failed: " + name, e);
            }
        }));
    }

    /**
     * 获取工具
     */
    public ToolMethod get(String name) {
        return tools.get(name);
    }

    /**
     * 获取所有工具
     */
    public Map<String, ToolMethod> getAll() {
        return new HashMap<>(tools);
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    /**
     * 工具方法封装
     */
    public static class ToolMethod {
        private final String name;
        private final String description;
        private final Function<Map<String, Object>, Object> handler;

        public ToolMethod(String name, String description, Function<Map<String, Object>, Object> handler) {
            this.name = name;
            this.description = description;
            this.handler = handler;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public Object execute(Map<String, Object> params) { return handler.apply(params); }
    }
}
