package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 自定义Agent创建响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentCreateResponse {

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
     * 绑定的工具包列表
     */
    private List<ToolPackageInfo> toolList;

    /**
     * 工具包信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolPackageInfo {
        /**
         * 工具包 ID（用于 Agent 绑定）
         */
        private String toolPackageId;

        /**
         * 工具包中文别名
         */
        private String toolAlias;

        /**
         * 工具包描述
         */
        private String toolDesc;

        /**
         * 包含的工具方法数量
         */
        private Integer methodCount;
    }
}
