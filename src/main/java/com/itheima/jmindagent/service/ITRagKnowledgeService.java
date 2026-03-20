package com.itheima.jmindagent.service;

import com.itheima.jmindagent.entity.TRagKnowledge;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.jmindagent.entity.dto.request.RagKbCreateRequest;
import com.itheima.jmindagent.entity.dto.request.RagKbListRequest;
import com.itheima.jmindagent.entity.dto.request.RagKbUpdateRequest;
import com.itheima.jmindagent.entity.dto.response.RagKbCreateResponse;
import com.itheima.jmindagent.entity.dto.response.RagKbDetailResponse;
import com.itheima.jmindagent.entity.dto.response.RagKbListResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author
 * @since 2026-03-16
 */
public interface ITRagKnowledgeService extends IService<TRagKnowledge> {

    /**
     * 创建 RAG 知识库
     * @param request 知识库创建请求参数
     * @return 知识库创建响应
     */
    RagKbCreateResponse createKnowledge(RagKbCreateRequest request);

    /**
     * 根据知识库 ID 获取详细信息
     * @param kbId 知识库 ID
     * @return 知识库详情
     */
    RagKbDetailResponse getKbDetail(String kbId);

    /**
     * 分页获取用户知识库列表
     * @param userId 用户 ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页知识库列表
     */
    RagKbListResponse getUserKbList(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 更新知识库信息
     * @param kbId 知识库 ID
     * @param userId 用户 ID
     * @param request 更新请求参数
     * @return 更新后的知识库详情
     */
    RagKbDetailResponse updateKnowledge(String kbId, Long userId, RagKbUpdateRequest request);

}
