package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * RAG 知识库创建响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagKbCreateResponse {

    /**
     * 知识库唯一 ID
     */
    private String kbId;

    /**
     * 知识库名称
     */
    private String kbName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
