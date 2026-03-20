package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 知识库文档上传响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagDocUploadResponse {

    /**
     * 文档唯一 ID
     */
    private String docId;

    /**
     * 上传状态：1-成功，0-失败
     */
    private Integer uploadStatus;

    /**
     * 向量化状态：1-完成，0-未完成
     */
    private Integer vectorStatus;
}
