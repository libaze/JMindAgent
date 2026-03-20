package com.itheima.jmindagent.entity.dto.request;

import lombok.Data;

/**
 * RAG 知识库列表查询请求 DTO
 */
@Data
public class RagKbListRequest {

    /**
     * 页码，默认第 1 页（选填）
     */
    private Integer pageNum = 1;

    /**
     * 每页条数，默认 10 条（选填）
     */
    private Integer pageSize = 10;
}
