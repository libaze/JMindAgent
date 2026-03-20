package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RAG 知识库详情响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagKbDetailResponse {

    /**
     * 知识库唯一 ID
     */
    private String kbId;

    /**
     * 所属用户 ID
     */
    private Long userId;

    /**
     * 知识库名称
     */
    private String kbName;

    /**
     * 知识库描述
     */
    private String kbDesc;

    /**
     * 文档数量
     */
    private Integer docCount;

    /**
     * 知识库状态：1-启用，0-禁用
     */
    private Integer kbStatus;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
