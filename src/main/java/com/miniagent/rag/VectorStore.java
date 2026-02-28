package com.miniagent.rag;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单向量存储 - 对标LangChain的VectorStore
 * 
 * 功能:
 * - 文档分块
 * - 向量化(模拟)
 * - 相似度检索
 * - 持久化
 */
@Slf4j
public class VectorStore {

    private final Map<String, Document> documents = new ConcurrentHashMap<>();
    private final Map<String, float[]> embeddings = new ConcurrentHashMap<>();
    private final String id;

    public VectorStore() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 添加文档
     */
    public String addDocument(String content) {
        return addDocument(content, new HashMap<>());
    }

    /**
     * 添加文档(带元数据)
     */
    public String addDocument(String content, Map<String, Object> metadata) {
        String id = UUID.randomUUID().toString();
        
        Document doc = new Document(id, content, metadata);
        documents.put(id, doc);
        
        // 生成embedding (模拟)
        embeddings.put(id, generateEmbedding(content));
        
        log.debug("Added document: {}", id);
        return id;
    }

    /**
     * 批量添加文档
     */
    public List<String> addDocuments(List<String> contents) {
        List<String> ids = new ArrayList<>();
        for (String content : contents) {
            ids.add(addDocument(content));
        }
        return ids;
    }

    /**
     * 相似度检索
     */
    public List<Document> similaritySearch(String query, int topK) {
        return similaritySearch(query, topK, 0.0);
    }

    /**
     * 相似度检索(带阈值)
     */
    public List<Document> similaritySearch(String query, int topK, double threshold) {
        float[] queryEmbedding = generateEmbedding(query);
        
        // 计算相似度
        List<SimScore> scores = new ArrayList<>();
        for (Map.Entry<String, float[]> entry : embeddings.entrySet()) {
            double similarity = cosineSimilarity(queryEmbedding, entry.getValue());
            if (similarity >= threshold) {
                scores.add(new SimScore(entry.getKey(), similarity));
            }
        }
        
        // 排序
        scores.sort((a, b) -> Double.compare(b.score, a.score));
        
        // 取topK
        List<Document> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scores.size()); i++) {
            Document doc = documents.get(scores.get(i).id);
            if (doc != null) {
                doc.setScore(scores.get(i).score);
                results.add(doc);
            }
        }
        
        return results;
    }

    /**
     * MMR检索 (最大边际相关)
     */
    public List<Document> maxMarginalRelevanceSearch(String query, int topK, double lambda) {
        float[] queryEmbedding = generateEmbedding(query);
        List<Document> results = new ArrayList<>();
        Set<String> selected = new HashSet<>();
        
        for (int i = 0; i < topK; i++) {
            double bestScore = -1;
            String bestId = null;
            
            for (Map.Entry<String, float[]> entry : embeddings.entrySet()) {
                if (selected.contains(entry.getKey())) continue;
                
                double relevance = cosineSimilarity(queryEmbedding, entry.getValue());
                double diversity = 1 - cosineSimilarity(entry.getValue(), queryEmbedding);
                double mmr = lambda * relevance + (1 - lambda) * diversity;
                
                if (mmr > bestScore) {
                    bestScore = mmr;
                    bestId = entry.getKey();
                }
            }
            
            if (bestId != null) {
                Document doc = documents.get(bestId);
                if (doc != null) {
                    doc.setScore(bestScore);
                    results.add(doc);
                    selected.add(bestId);
                }
            }
        }
        
        return results;
    }

    /**
     * 删除文档
     */
    public boolean deleteDocument(String id) {
        documents.remove(id);
        embeddings.remove(id);
        return true;
    }

    /**
     * 获取文档
     */
    public Document getDocument(String id) {
        return documents.get(id);
    }

    /**
     * 获取所有文档
     */
    public Collection<Document> getAllDocuments() {
        return documents.values();
    }

    /**
     * 文档数量
     */
    public int count() {
        return documents.size();
    }

    /**
     * 生成embedding (简化版 - 实际应该调用embedding模型)
     */
    private float[] generateEmbedding(String text) {
        // 简单hash作为模拟
        int hash = text.hashCode();
        Random random = new Random(hash);
        float[] embedding = new float[384];
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = (float) (random.nextDouble() * 2 - 1);
        }
        return embedding;
    }

    /**
     * 余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Data
    public static class Document {
        private final String id;
        private final String content;
        private final Map<String, Object> metadata;
        private double score;

        public Document(String id, String content, Map<String, Object> metadata) {
            this.id = id;
            this.content = content;
            this.metadata = metadata;
        }
    }

    private static class SimScore {
        String id;
        double score;
        
        SimScore(String id, double score) {
            this.id = id;
            this.score = score;
        }
    }
}
