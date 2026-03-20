package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 知识库文档列表响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagDocListResponse {

    /**
     * 文档列表
     */
    private List<RagDocListItemResponse> list;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 文档列表项内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagDocListItemResponse {
        /**
         * 文档唯一 ID
         */
        private String docId;

        /**
         * 原始文档名称
         */
        private String docName;

        /**
         * 文档类型（txt/pdf/md）
         */
        private String docType;

        /**
         * 文档大小（字节）
         */
        private Long docSize;

        /**
         * 文档处理状态：1-处理中，2-已完成，3-失败
         */
        private Integer docStatus;

        /**
         * 状态描述
         */
        private String statusDesc;

        /**
         * 分块总数
         */
        private Integer chunkCount;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;

        /**
         * 更新时间
         */
        private LocalDateTime updateTime;
    }
}
