package com.miniagent.tool;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * 天气查询工具
 */
@Slf4j
public class WeatherTool implements FnCallAgent.ToolExecutor {

    private final HttpClient httpClient;
    
    public WeatherTool() {
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        String city = (String) params.get("city");
        if (city == null || city.isEmpty()) {
            return Map.of("error", "City is required");
        }
        
        // 使用wttr.in (免费天气API)
        String url = "https://wttr.in/" + city + "?format=j1";
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseWeatherResponse(response.body());
            } else {
                return Map.of("error", "Weather service unavailable");
            }
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    private Object parseWeatherResponse(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> data = mapper.readValue(json, Map.class);
            
            List<Object> current = (List<Object>) data.get("current_condition");
            if (current != null && !current.isEmpty()) {
                Map<Object, Object> cond = (Map<Object, Object>) current.get(0);
                return Map.of(
                    "city", data.get("nearest_area") != null ? 
                        ((List)data.get("nearest_area")).get(0) : "Unknown",
                    "temperature", cond.get("temp_C"),
                    "condition", cond.get("weatherDesc"),
                    "humidity", cond.get("humidity"),
                    "wind", cond.get("windspeedKmph") + " km/h"
                );
            }
            return Map.of("error", "No data");
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }

    @Override
    public String getDescription() {
        return "Get weather information for a city. " +
               "Input: {'city': 'city name'}. " +
               "Output: {'temperature': '...', 'condition': '...', 'humidity': '...'}";
    }

    @Override
    public Map<String, com.miniagent.model.ChatRequest.ToolParameter> getParameters() {
        Map<String, com.miniagent.model.ChatRequest.ToolParameter> params = new HashMap<>();
        
        com.miniagent.model.ChatRequest.ToolParameter cityParam = new com.miniagent.model.ChatRequest.ToolParameter();
        cityParam.setType("string");
        cityParam.setDescription("City name");
        cityParam.setRequired(true);
        params.put("city", cityParam);
        
        return params;
    }
}
