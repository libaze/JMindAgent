package com.itheima.jmindagent.service;

import com.itheima.jmindagent.entity.TRagChunkVector;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author
 * @since 2026-03-16
 */
public interface ITRagChunkVectorService extends IService<TRagChunkVector> {

    /**
     * 基于向量相似度检索相关片段（带最小相似度阈值）
     * @param kbId 知识库 ID
     * @param query 查询文本
     * @param topK 返回数量
     * @param minSimilarity 最小相似度阈值（0.0-1.0，默认 0.5）
     * @return 带相似度分数的片段列表
     */
    List<TRagChunkVector> retrieveChunks(String kbId, String query, int topK, double minSimilarity);

}
