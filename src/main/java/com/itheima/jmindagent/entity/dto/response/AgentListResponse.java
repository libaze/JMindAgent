package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自定义Agent 列表响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentListResponse {

    /**
     * Agent 列表
     */
    private List<AgentListItemResponse> list;

    /**
     * 总记录数
     */
    private long total;

    /**
     * Agent 列表项内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentListItemResponse {
        /**
         * Agent 唯一 ID
         */
        private String agentId;

        /**
         * Agent 名称
         */
        private String agentName;

        /**
         * Agent 描述
         */
        private String agentDesc;

        /**
         * 专属提示词
         */
        private String agentPrompt;

        /**
         * 绑定的工具包列表
         */
        private List<AgentCreateResponse.ToolPackageInfo> toolList;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;
    }
}
