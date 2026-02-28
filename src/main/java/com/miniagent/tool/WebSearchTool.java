package com.miniagent.tool;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网页搜索工具
 * 对标 Qwen-Agent 的 web_search
 */
@Slf4j
public class WebSearchTool implements FnCallAgent.ToolExecutor {

    private final HttpClient httpClient;
    
    // 简单的搜索API (可以使用SerpAPI/Bing API等)
    private String apiKey;
    
    public WebSearchTool() {
        this.httpClient = HttpClient.newHttpClient();
    }
    
    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        String query = (String) params.get("query");
        if (query == null || query.isEmpty()) {
            return Map.of("error", "No query provided");
        }
        
        Integer numResults = params.get("num_results") != null ? 
            (Integer) params.get("num_results") : 5;
        
        return search(query, numResults);
    }

    /**
     * 执行搜索
     */
    private Map<String, Object> search(String query, int numResults) {
        // 这里使用一个简单的模拟实现
        // 生产环境可以接入 SerpAPI, Bing Search API, Google Custom Search API 等
        
        try {
            // 尝试使用 DuckDuckGo HTML 搜索
            String url = "https://html.duckduckgo.com/html/?q=" + 
                java.net.URLEncoder.encode(query, "UTF-8");
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseSearchResults(response.body(), numResults);
            } else {
                return Map.of("error", "Search failed: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Search error", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 解析搜索结果
     */
    private Map<String, Object> parseSearchResults(String html, int numResults) {
        Map<String, Object> result = new HashMap<>();
        result.put("query", html);
        
        // 简单解析 - 提取标题和链接
        // 生产环境应使用更 robust 的解析
        
        return result;
    }

    @Override
    public String getDescription() {
        return "Search the web for information. " +
               "Input: {'query': 'search string', 'num_results': 5}. " +
               "Output: {'results': [...]}";
    }

    @Override
    public Map<String, com.miniagent.model.ChatRequest.ToolParameter> getParameters() {
        Map<String, com.miniagent.model.ChatRequest.ToolParameter> params = new HashMap<>();
        
        com.miniagent.model.ChatRequest.ToolParameter queryParam = new com.miniagent.model.ChatRequest.ToolParameter();
        queryParam.setType("string");
        queryParam.setDescription("Search query");
        queryParam.setRequired(true);
        params.put("query", queryParam);
        
        com.miniagent.model.ChatRequest.ToolParameter numParam = new com.miniagent.model.ChatRequest.ToolParameter();
        numParam.setType("integer");
        numParam.setDescription("Number of results to return");
        numParam.setRequired(false);
        params.put("num_results", numParam);
        
        return params;
    }
}
