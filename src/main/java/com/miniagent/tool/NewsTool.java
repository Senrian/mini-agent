package com.miniagent.tool;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * 新闻获取工具
 */
@Slf4j
public class NewsTool implements FnCallAgent.ToolExecutor {

    private final HttpClient httpClient;
    
    public NewsTool() {
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        String category = (String) params.getOrDefault("category", "general");
        String country = (String) params.getOrDefault("country", "cn");
        
        // 使用公开的新闻API
        String url = "https://news.google.com/rss/topics/" + getTopicId(category);
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseRssFeed(response.body());
            } else {
                // 返回默认新闻
                return getDefaultNews(category);
            }
        } catch (Exception e) {
            return getDefaultNews(category);
        }
    }

    private String getTopicId(String category) {
        switch (category.toLowerCase()) {
            case "tech": return "CAAqJggKIiBDQkFTRWdvSUwyMHZNRGx1YlY4U0FtVnVHZ0pWVXlnQVAB";
            case "business": return "CAAqJggKIiBDQkFTRWdvSUwyMHZNRGx1YlY4U0FtVnVHZ0pWVXlnQVAB";
            case "sports": return "CAAqJggKIiBDQkFTRWdvSUwyMHZNRGx1YlY4U0FtVnVHZ0pWVXlnQVAB";
            case "entertainment": return "CAAqJggKIiBDQkFTRWdvSUwyMHZNRGx1YlY4U0FtVnVHZ0pWVXlnQVAB";
            default: return "CAAqJggKIiBDQkFTRWdvSUwyMHZNRGx1YlY4U0FtVnVHZ0pWVXlnQVAB";
        }
    }

    private Object parseRssFeed(String xml) {
        List<Map<String, String>> news = new ArrayList<>();
        String[] items = xml.split("<item>");
        
        for (int i = 1; i < Math.min(items.length, 10); i++) {
            String item = items[i];
            Map<String, String> newsItem = new HashMap<>();
            
            String title = extractTag(item, "title");
            String link = extractTag(item, "link");
            String pubDate = extractTag(item, "pubDate");
            
            if (title != null && !title.isEmpty()) {
                newsItem.put("title", title);
                newsItem.put("link", link);
                newsItem.put("date", pubDate);
                news.add(newsItem);
            }
        }
        
        return Map.of("news", news, "count", news.size());
    }

    private String extractTag(String xml, String tag) {
        int start = xml.indexOf("<" + tag + ">");
        if (start < 0) return null;
        start += tag.length() + 2;
        int end = xml.indexOf("</" + tag + ">");
        if (end < 0) return null;
        return xml.substring(start, end).trim();
    }

    private Object getDefaultNews(String category) {
        return Map.of(
            "news", List.of(
                Map.of("title", "News API unavailable", "date", new Date().toString()),
                Map.of("title", "Please try again later", "date", new Date().toString())
            ),
            "category", category
        );
    }

    @Override
    public String getDescription() {
        return "Get latest news headlines. " +
               "Input: {'category': 'general|tech|business|sports|entertainment'}. " +
               "Output: {'news': [...], 'count': n}";
    }

    @Override
    public Map<String, com.miniagent.model.ChatRequest.ToolParameter> getParameters() {
        Map<String, com.miniagent.model.ChatRequest.ToolParameter> params = new HashMap<>();
        
        com.miniagent.model.ChatRequest.ToolParameter catParam = new com.miniagent.model.ChatRequest.ToolParameter();
        catParam.setType("string");
        catParam.setDescription("News category");
        params.put("category", catParam);
        
        return params;
    }
}
