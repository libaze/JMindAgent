package com.itheima.jmindagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.jmindagent.entity.TRagChunkVector;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// ... existing code ...
public interface TRagChunkVectorMapper extends BaseMapper<TRagChunkVector> {

    /**
     * 批量插入向量记录
     * @param chunkVectors 向量列表
     * @return 是否成功
     */
    boolean insertBatch(@Param("list") List<TRagChunkVector> chunkVectors);

    /**
     * 基于向量相似度搜索
     * @param queryVector 查询向量（PostgreSQL vector 格式字符串）
     * @param kbId 知识库 ID
     * @param topK 返回数量
     * @return 带相似度分数的结果列表
     */
    List<TRagChunkVector> selectBySimilarity(
            @Param("queryVector") String queryVector,
            @Param("kbId") String kbId,
            @Param("topK") int topK,
            @Param("minSimilarity") double minSimilarity
    );
}
