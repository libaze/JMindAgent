package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RAG 知识库列表项响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagKbListItemResponse {

    /**
     * 知识库唯一 ID
     */
    private String kbId;

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
     * 知识库状态
     */
    private Integer kbStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;
}
