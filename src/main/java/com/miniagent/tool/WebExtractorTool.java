package com.miniagent.tool;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 网页抓取工具
 * 对标 Qwen-Agent 的 web_extractor
 */
@Slf4j
public class WebExtractorTool implements FnCallAgent.ToolExecutor {

    private final HttpClient httpClient;
    
    public WebExtractorTool() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
    }

    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        String url = (String) params.get("url");
        if (url == null || url.isEmpty()) {
            return Map.of("error", "No URL provided");
        }
        
        String selector = (String) params.get("selector"); // CSS选择器
        Integer timeout = params.get("timeout") != null ? 
            (Integer) params.get("timeout") : 30;
        
        return extract(url, selector, timeout);
    }

    /**
     * 抓取网页
     */
    private Map<String, Object> extract(String url, String selector, int timeout) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .timeout(java.time.Duration.ofSeconds(timeout))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String html = response.body();
                
                // 简单提取 (生产环境应使用Jsoup等HTML解析器)
                String title = extractTitle(html);
                String content = extractContent(html, selector);
                
                return Map.of(
                    "url", url,
                    "title", title != null ? title : "",
                    "content", content != null ? content : html.substring(0, Math.min(html.length(), 5000)),
                    "statusCode", response.statusCode()
                );
            } else {
                return Map.of(
                    "error", "HTTP " + response.statusCode(),
                    "url", url
                );
            }
        } catch (Exception e) {
            log.error("Web extraction error", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 提取标题
     */
    private String extractTitle(String html) {
        Pattern pattern = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 提取内容 (简化版)
     */
    private String extractContent(String html, String selector) {
        if (selector == null || selector.isEmpty()) {
            // 默认提取 body
            Pattern pattern = Pattern.compile("<body[^>]*>([\\s\\S]*?)</body>", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return cleanHtml(matcher.group(1));
            }
        }
        // CSS选择器需要HTML解析库如Jsoup
        return null;
    }

    /**
     * 清理HTML标签
     */
    private String cleanHtml(String html) {
        // 移除脚本和样式
        html = html.replaceAll("<script[^>]*>[\\s\\S]*?</script>", "");
        html = html.replaceAll("<style[^>]*>[\\s\\S]*?</style>", "");
        // 移除HTML标签
        html = html.replaceAll("<[^>]+>", "");
        // 合并空白
        html = html.replaceAll("\\s+", " ").trim();
        return html;
    }

    @Override
    public String getDescription() {
        return "Extract content from a webpage. " +
               "Input: {'url': 'https://...', 'selector': 'optional CSS selector'}. " +
               "Output: {'title': '...', 'content': '...'}";
    }

    @Override
    public Map<String, com.miniagent.model.ChatRequest.ToolParameter> getParameters() {
        Map<String, com.miniagent.model.ChatRequest.ToolParameter> params = new HashMap<>();
        
        com.miniagent.model.ChatRequest.ToolParameter urlParam = new com.miniagent.model.ChatRequest.ToolParameter();
        urlParam.setType("string");
        urlParam.setDescription("URL to extract from");
        urlParam.setRequired(true);
        params.put("url", urlParam);
        
        com.miniagent.model.ChatRequest.ToolParameter selectorParam = new com.miniagent.model.ChatRequest.ToolParameter();
        selectorParam.setType("string");
        selectorParam.setDescription("CSS selector (optional)");
        selectorParam.setRequired(false);
        params.put("selector", selectorParam);
        
        return params;
    }
}
