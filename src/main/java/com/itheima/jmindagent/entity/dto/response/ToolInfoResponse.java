package com.itheima.jmindagent.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工具信息响应对象（支持工具包结构）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolInfoResponse {

    /**
     * 工具包 ID（用于 Agent 绑定）
     */
    private String toolPackageId;

    /**
     * 工具包中文别名
     */
    private String toolAlias;

    /**
     * 工具包总体描述
     */
    private String toolDescription;

    /**
     * 工具类型：local-本地工具，mcp-MCP 远程工具
     */
    private String toolType;

    /**
     * 包含的具体工具方法列表
     */
    private List<ToolMethodItem> methods;

    /**
     * 具体工具方法内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolMethodItem {
        /**
         * 工具方法 ID（实际调用时的 ID）
         */
        private String methodId;

        /**
         * 工具方法名称
         */
        private String methodName;

        /**
         * 工具方法描述
         */
        private String methodDescription;
    }
}
