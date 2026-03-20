package com.itheima.jmindagent.service;

import com.itheima.jmindagent.entity.TRagDocument;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.jmindagent.entity.dto.request.RagDocUploadRequest;
import com.itheima.jmindagent.entity.dto.response.RagDocListResponse;
import com.itheima.jmindagent.entity.dto.response.RagDocUploadResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author
 * @since 2026-03-16
 */
public interface ITRagDocumentService extends IService<TRagDocument> {

    /**
     * 上传文档至知识库（SSE 流式）
     * @param request 文档上传请求参数
     * @return SSE 发射器，用于实时推送处理进度
     */
    org.springframework.web.servlet.mvc.method.annotation.SseEmitter uploadDocumentWithProgress(RagDocUploadRequest request);

    /**
     * 查询知识库中文档列表
     * @param kbId 知识库 ID
     * @param userId 用户 ID
     * @return 文档列表响应
     */
    RagDocListResponse getDocumentList(String kbId, Long userId);

    /**
     * 删除知识库中的文档（包括向量数据和本地文件）
     * @param docId 文档 ID
     * @param kbId 知识库 ID
     * @param userId 用户 ID
     */
    void deleteDocument(String docId, String kbId, Long userId);

    /**
     * 批量删除知识库中的所有文档（包括向量数据和本地文件）
     * @param kbId 知识库 ID
     * @param userId 用户 ID
     */
    void deleteAllDocumentsByKbId(String kbId, Long userId);

    /**
     * 删除知识库（包括所有文档、向量数据和本地资源文件）
     * @param kbId 知识库 ID
     * @param userId 用户 ID
     */
    void deleteKnowledgeWithDocuments(String kbId, Long userId);
}
