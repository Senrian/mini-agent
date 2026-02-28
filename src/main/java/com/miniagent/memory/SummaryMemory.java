package com.miniagent.memory;

import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 摘要记忆 - 自动总结对话要点
 * 对标 Qwen-Agent 的 virtual_memory_agent
 */
@Data
public class SummaryMemory {

    private String summary;
    private List<MemoryItem> items;
    private int maxItems;

    public SummaryMemory(int maxItems) {
        this.maxItems = maxItems;
        this.items = new ArrayList<>();
    }

    public SummaryMemory() {
        this(10);
    }

    /**
     * 添加记忆项
     */
    public void add(String content) {
        MemoryItem item = new MemoryItem(content);
        items.add(item);
        
        // 裁剪
        while (items.size() > maxItems) {
            // 移除最老的
            items.sort(Comparator.comparingLong(MemoryItem::getTimestamp));
            if (!items.isEmpty()) {
                items.remove(0);
            }
        }
    }

    /**
     * 更新摘要
     */
    public void updateSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 获取所有记忆 (用于构建prompt)
     */
    public String getFormattedMemory() {
        StringBuilder sb = new StringBuilder();
        
        if (summary != null && !summary.isEmpty()) {
            sb.append("Summary:\n").append(summary).append("\n\n");
        }
        
        if (!items.isEmpty()) {
            sb.append("Recent interactions:\n");
            for (MemoryItem item : items) {
                sb.append("- ").append(item.getContent()).append("\n");
            }
        }
        
        return sb.toString();
    }

    /**
     * 清除记忆
     */
    public void clear() {
        items.clear();
        summary = null;
    }

    @Data
    public static class MemoryItem {
        private final String content;
        private final long timestamp;

        public MemoryItem(String content) {
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
