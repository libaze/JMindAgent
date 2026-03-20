package com.itheima.jmindagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.jmindagent.entity.TRagChunkVector;
import com.itheima.jmindagent.mapper.TRagChunkVectorMapper;
import com.itheima.jmindagent.service.ITRagChunkVectorService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author
 * @since 2026-03-16
 */
@Slf4j
@Service
public class TRagChunkVectorServiceImpl extends ServiceImpl<TRagChunkVectorMapper, TRagChunkVector> implements ITRagChunkVectorService {

    private final TRagChunkVectorMapper chunkVectorMapper;
    private final EmbeddingModel embeddingModel;

    public TRagChunkVectorServiceImpl(TRagChunkVectorMapper chunkVectorMapper,
                                          EmbeddingModel embeddingModel) {
        this.chunkVectorMapper = chunkVectorMapper;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<TRagChunkVector> retrieveChunks(String kbId, String query, int topK, double minSimilarity) {
        if (kbId == null || kbId.isBlank()) {
            log.warn("知识库 ID 为空：kbId={}", kbId);
            return List.of();
        }

        if (query == null || query.isBlank()) {
            log.warn("查询内容为空：query={}", query);
            return List.of();
        }

        // 校验相似度阈值范围
        if (minSimilarity < 0.0 || minSimilarity > 1.0) {
            log.warn("相似度阈值超出范围 [0.0, 1.0]，使用默认值 0.5：minSimilarity={}", minSimilarity);
            minSimilarity = 0.5;
        }

        try {
            // 1. 生成查询向量
            log.info("开始生成查询向量：query={}, minSimilarity={}", query.substring(0, Math.min(50, query.length())), minSimilarity);
            float[] queryVector = generateQueryEmbedding(query);
            String queryVectorStr = toPgVector(queryVector);

            // 2. 使用 PostgreSQL pgvector 进行相似度搜索（带阈值过滤）
            log.info("执行相似度搜索：kbId={}, topK={}, minSimilarity={}", kbId, topK, minSimilarity);
            List<TRagChunkVector> results = chunkVectorMapper.selectBySimilarity(queryVectorStr, kbId, topK, minSimilarity);

            if (results.isEmpty()) {
                log.info("未检索到相关内容（可能低于阈值）：kbId={}, query={}, minSimilarity={}", kbId, query, minSimilarity);
            } else {
                log.info("相似度检索成功：kbId={}, query={}, 找到{}个片段，minSimilarity={}", kbId, query, results.size(), minSimilarity);
            }

            return results;

        } catch (Exception e) {
            log.error("相似度检索异常：kbId={}, query={}, error={}", kbId, query, e.getMessage(), e);
            // 降级为关键词检索
            return fallbackToKeywordSearch(kbId, query, topK);
        }
    }

    /**
     * 生成查询向量
     */
    private float[] generateQueryEmbedding(String query) {
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(query));
            if (response != null && response.getResult() != null) {
                return response.getResult().getOutput();
            }
            throw new RuntimeException("Embedding 响应为空");
        } catch (Exception e) {
            log.error("生成查询向量失败：query={}, error={}", query, e.getMessage());
            throw new RuntimeException("向量化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将 float 数组转换为 PostgreSQL vector 格式字符串
     */
    private String toPgVector(float[] vector) {
        if (vector == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(String.format("%.6f", vector[i]));
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 关键词检索（降级方案）
     */
    private List<TRagChunkVector> keywordSearch(String kbId, String query, int limit) {
        try {
            LambdaQueryWrapper<TRagChunkVector> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TRagChunkVector::getKbId, kbId)
                    .eq(TRagChunkVector::getVectorStatus, 1)
                    .like(TRagChunkVector::getChunkContent, query)
                    .last("LIMIT " + limit);

            return chunkVectorMapper.selectList(wrapper);
        } catch (Exception e) {
            log.error("关键词检索失败：kbId={}, query={}, error={}", kbId, query, e.getMessage());
            return List.of();
        }
    }

    /**
     * 降级到关键词检索
     */
    private List<TRagChunkVector> fallbackToKeywordSearch(String kbId, String query, int topK) {
        log.warn("降级到关键词检索：kbId={}, query={}", kbId, query);
        return keywordSearch(kbId, query, topK);
    }
}
