package com.itheima.jmindagent.entity.dto.request;

import lombok.Data;

/**
 * RAG 知识库更新请求 DTO
 */
@Data
public class RagKbUpdateRequest {

    /**
     * 知识库名称（选填）
     */
    private String kbName;

    /**
     * 知识库描述（选填）
     */
    private String kbDesc;

    /**
     * 知识库状态：1-启用，0-禁用（选填）
     */
    private Integer kbStatus;
}
