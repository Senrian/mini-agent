package com.miniagent.rag;

import java.util.*;

/**
 * 文档加载器 - 对标LangChain的DocumentLoader
 */
public interface DocumentLoader {
    List<VectorStore.Document> load();
}

/**
 * 文本加载器
 */
class TextLoader implements DocumentLoader {
    private final String content;
    private final Map<String, Object> metadata;

    public TextLoader(String content) {
        this(content, new HashMap<>());
    }

    public TextLoader(String content, Map<String, Object> metadata) {
        this.content = content;
        this.metadata = metadata;
    }

    @Override
    public List<VectorStore.Document> load() {
        return List.of(new VectorStore.Document(
            UUID.randomUUID().toString(),
            content,
            metadata
        ));
    }
}

/**
 * 文本分割器 - 对标LangChain的TextSplitter
 */
class TextSplitter {
    private final int chunkSize;
    private final int chunkOverlap;

    public TextSplitter(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public TextSplitter() {
        this(1000, 200);
    }

    public List<String> splitText(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunk = text.substring(start, end);
            chunks.add(chunk);
            
            start += chunkSize - chunkOverlap;
            if (start >= text.length()) break;
        }

        return chunks;
    }

    /**
     * 按段落分割
     */
    public List<String> splitByParagraphs(String text) {
        String[] paragraphs = text.split("\\n\\n+");
        List new ArrayList<>();
        
<String> chunks =        StringBuilder current = new StringBuilder();
        for (String para : paragraphs) {
            if (current.length() + para.length() > chunkSize && current.length() > 0) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(para).append("\n\n");
        }
        
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        
        return chunks;
    }
}
