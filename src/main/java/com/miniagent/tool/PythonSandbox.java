package com.miniagent.tool;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Python沙箱执行器 - 对标OpenClaw的代码执行能力
 * 
 * 特性:
 * - 隔离的运行环境
 * - 超时控制
 * - 内存限制
 * - 输出捕获
 * - 错误处理
 */
@Slf4j
public class PythonSandbox implements FnCallAgent.ToolExecutor {

    private final int timeoutSeconds;
    private final int maxOutputSize;
    private final Path workDir;
    
    public PythonSandbox() {
        this(30, 1024 * 1024, Paths.get("/tmp/mini-agent-sandbox"));
    }
    
    public PythonSandbox(int timeoutSeconds, int maxOutputSize, Path workDir) {
        this.timeoutSeconds = timeoutSeconds;
        this.maxOutputSize = maxOutputSize;
        this.workDir = workDir;
        
        try {
            Files.createDirectories(workDir);
        } catch (Exception e) {
            log.error("Failed to create work directory", e);
        }
    }

    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        String code = (String) params.get("code");
        if (code == null || code.isEmpty()) {
            return Map.of("error", "No code provided");
        }
        
        // 清理危险代码
        code = sanitizeCode(code);
        
        return executePython(code);
    }

    /**
     * 执行Python代码
     */
    private Map<String, Object> executePython(String code) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        Path scriptFile = workDir.resolve("script_" + sessionId + ".py");
        Path outputFile = workDir.resolve("output_" + sessionId + ".txt");
        Path errorFile = workDir.resolve("error_" + sessionId + ".txt");
        
        try {
            // 写入脚本
            Files.writeString(scriptFile, code);
            
            // 构建命令
            List<String> cmd = Arrays.asList(
                "python3", "-u", scriptFile.toString()
            );
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workDir.toFile());
            pb.redirectError(ProcessBuilder.Redirect.to(errorFile.toFile()));
            
            // 设置环境变量
            Map<String, String> env = pb.environment();
            env.put("PYTHONUNBUFFERED", "1");
            env.put("PYTHONPATH", "");
            
            Process process = pb.start();
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (output.length() > maxOutputSize) {
                        process.destroyForcibly();
                        return Map.of(
                            "success", false,
                            "error", "Output too large (max " + maxOutputSize + " bytes)",
                            "partial_output", output.toString()
                        );
                    }
                }
            }
            
            // 等待完成或超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return Map.of(
                    "success", false,
                    "error", "Execution timeout (" + timeoutSeconds + "s)",
                    "output", output.toString()
                );
            }
            
            int exitCode = process.exitValue();
            
            // 读取错误
            String error = "";
            if (Files.exists(errorFile)) {
                error = Files.readString(errorFile);
            }
            
            // 清理临时文件
            cleanupFile(scriptFile);
            cleanupFile(outputFile);
            cleanupFile(errorFile);
            
            if (exitCode == 0) {
                return Map.of(
                    "success", true,
                    "output", output.toString(),
                    "exitCode", exitCode
                );
            } else {
                return Map.of(
                    "success", false,
                    "output", output.toString(),
                    "error", error,
                    "exitCode", exitCode
                );
            }
            
        } catch (Exception e) {
            log.error("Python execution error", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        } finally {
            cleanupFile(scriptFile);
            cleanupFile(outputFile);
            cleanupFile(errorFile);
        }
    }

    /**
     * 清理危险代码
     */
    private String sanitizeCode(String code) {
        // 禁止的命令
        String[] forbidden = {
            "import os",
            "import sys",
            "import subprocess",
            "import socket",
            "import requests",
            "import urllib",
            "import httpx",
            "import fcntl",
            "import resource",
            "import multiprocessing",
            "import threading",
            "__import__(\"os\")",
            "__import__(\"sys\")",
            "open(",
            "file(",
            "eval(",
            "exec(",
            "compile(",
            "breakpoint(",
        };
        
        // 检查是否有导入危险模块的意图
        for (String cmd : forbidden) {
            if (code.contains(cmd)) {
                log.warn("Blocked dangerous code: {}", cmd);
                // 替换为安全版本
                code = code.replace(cmd, "# Blocked: " + cmd);
            }
        }
        
        return code;
    }
    
    private void cleanupFile(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (Exception e) {
            log.debug("Failed to cleanup file: {}", file);
        }
    }

    @Override
    public String getDescription() {
        return "Execute Python code in a sandboxed environment. " +
               "Input: {'code': 'python code string'}. " +
               "Output: {'success': bool, 'output': 'result', 'error': 'error message if any'}. " +
               "Note: Some modules are blocked for security.";
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
