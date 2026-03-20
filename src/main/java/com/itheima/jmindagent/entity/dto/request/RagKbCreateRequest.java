package com.itheima.jmindagent.entity.dto.request;

import lombok.Data;

/**
 * RAG 知识库创建请求 DTO
 */
@Data
public class RagKbCreateRequest {
    /**
     * 知识库名称（必填）
     */
    private String kbName;

    /**
     * 知识库描述（选填）
     */
    private String kbDesc;
}
