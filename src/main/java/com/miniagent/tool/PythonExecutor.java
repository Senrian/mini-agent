package com.miniagent.tool;

import lombok.extern.slf4j.Slf4j;

import javax.tools.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Python代码执行器
 * 对标 Qwen-Agent 的 python_executor
 */
@Slf4j
public class PythonExecutor implements FnCallAgent.ToolExecutor {

    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        String code = (String) params.get("code");
        if (code == null || code.isEmpty()) {
            return Map.of("error", "No code provided");
        }
        
        // 注意: 这是一个模拟实现
        // 生产环境需要使用 Docker 容器或专门的代码执行服务
        return executePython(code);
    }

    private Object executePython(String code) {
        try {
            // 检查是否有python3
            ProcessBuilder pb = new ProcessBuilder("python3", "-c", code);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringWriter output = new StringWriter();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                return Map.of(
                    "success", true,
                    "output", output.toString()
                );
            } else {
                return Map.of(
                    "success", false,
                    "error", output.toString(),
                    "exitCode", exitCode
                );
            }
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    @Override
    public String getDescription() {
        return "Execute Python code and return the output. " +
               "Input: {'code': 'python code string'}. " +
               "Output: {'success': bool, 'output': 'result or error'}";
    }

    @Override
    public Map<String, com.miniagent.model.ChatRequest.ToolParameter> getParameters() {
        Map<String, com.miniagent.model.ChatRequest.ToolParameter> params = new HashMap<>();
        
        com.miniagent.model.ChatRequest.ToolParameter codeParam = new com.miniagent.model.ChatRequest.ToolParameter();
        codeParam.setType("string");
        codeParam.setDescription("Python code to execute");
        codeParam.setRequired(true);
        params.put("code", codeParam);
        
        return params;
    }
}
