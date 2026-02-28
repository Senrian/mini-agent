package com.miniagent.agent.browser;

import com.miniagent.model.ChatRequest;
import com.miniagent.model.ChatResponse;
import com.miniagent.service.AiService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Browser Agent - 对标Qwen-Agent的Browser功能
 * 
 * 能力:
 * - 网页浏览
 * - 元素交互
 * - 截图
 * - 执行JavaScript
 */
@Slf4j
@Data
public class BrowserAgent {

    private String id;
    private String name;
    private String systemPrompt;
    private List<ChatRequest.Message> history;
    private AiService aiService;
    private Map<String, Object> config;
    private HttpClient httpClient;
    
    // 简单浏览器模拟 (生产环境应使用Selenium/Playwright)
    private boolean headless = true;

    public BrowserAgent(AiService aiService, String systemPrompt) {
        this.id = UUID.randomUUID().toString();
        this.aiService = aiService;
        this.systemPrompt = systemPrompt;
        this.history = new ArrayList<>();
        this.httpClient = HttpClient.newHttpClient();
        this.config = new HashMap<>();
        config.put("timeout", 30000);
        config.put("userAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
    }

    /**
     * 导航到URL
     */
    public BrowserResult navigate(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", (String) config.get("userAgent"))
                    .timeout(java.time.Duration.ofMillis((Integer) config.get("timeout")))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());

            return new BrowserResult(
                true, 
                "Navigated to " + url,
                url,
                extractTitle(response.body()),
                extractLinks(response.body()),
                extractContent(response.body())
            );
        } catch (Exception e) {
            log.error("Navigation error", e);
            return new BrowserResult(false, e.getMessage(), url, null, List.of(), "");
        }
    }

    /**
     * 获取页面内容
     */
    public BrowserResult getContent(String url) {
        return navigate(url);
    }

    /**
     * 搜索
     */
    public BrowserResult search(String query) {
        // 简单使用DuckDuckGo
        String searchUrl = "https://html.duckduckgo.com/html/?q=" + 
            java.net.URLEncoder.encode(query, "UTF-8");
        return navigate(searchUrl);
    }

    /**
     * 执行对话
     */
    public String chat(String userMessage) {
        addMessage("user", userMessage);
        
        ChatRequest request = new ChatRequest();
        request.setMessages(new ArrayList<>(history));
        request.setSystemPrompt(buildBrowserPrompt());
        
        ChatResponse response = aiService.chat(request);
        addMessage("assistant", response.getContent());
        
        return response.getContent();
    }

    private String buildBrowserPrompt() {
        return systemPrompt + "\n\n" +
            "You are a browser assistant. You can:\n" +
            "- Navigate to URLs\n" +
            "- Search the web\n" +
            "- Extract page content\n" +
            "- Find links and information\n\n" +
            "Use the browser methods available to you.";
    }

    private void addMessage(String role, String content) {
        ChatRequest.Message msg = new ChatRequest.Message();
        msg.setRole(role);
        msg.setContent(content);
        history.add(msg);
    }

    private String extractTitle(String html) {
        if (html == null) return null;
        int start = html.indexOf("<title>");
        int end = html.indexOf("</title>");
        if (start >= 0 && end > start) {
            return html.substring(start + 7, end).trim();
        }
        return null;
    }

    private List<String> extractLinks(String html) {
        List<String> links = new ArrayList<>();
        if (html == null) return links;
        
        int pos = 0;
        while (pos < html.length()) {
            int hrefStart = html.indexOf("href=\"", pos);
            if (hrefStart < 0) break;
            int hrefEnd = html.indexOf("\"", hrefStart + 6);
            if (hrefEnd < 0) break;
            String link = html.substring(hrefStart + 6, hrefEnd);
            if (link.startsWith("http")) {
                links.add(link);
            }
            pos = hrefEnd;
            if (links.size() >= 20) break;
        }
        return links;
    }

    private String extractContent(String html) {
        if (html == null) return "";
        // 简单提取
        return html.replaceAll("<script[^>]*>[\\s\\S]*?</script>", "")
                   .replaceAll("<style[^>]*>[\\s\\S]*?</style>", "")
                   .replaceAll("<[^>]+>", "")
                   .replaceAll("\\s+", " ")
                   .trim()
                   .substring(0, Math.min(html.length(), 5000));
    }

    @Data
    public static class BrowserResult {
        private final boolean success;
        private final String message;
        private final String url;
        private final String title;
        private final List<String> links;
        private final String content;
    }
}
