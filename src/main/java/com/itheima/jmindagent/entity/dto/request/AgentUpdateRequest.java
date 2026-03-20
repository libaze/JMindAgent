package com.itheima.jmindagent.entity.dto.request;

import lombok.Data;
import java.util.List;

/**
 * 自定义Agent 更新请求 DTO
 */
@Data
public class AgentUpdateRequest {

    /**
     * Agent 名称（选填）
     */
    private String agentName;

    /**
     * Agent 描述（选填）
     */
    private String agentDesc;

    /**
     * Agent 专属提示词（选填）
     */
    private String prompt;

    /**
     * 绑定的工具 ID 集合（选填）
     */
    private List<String> toolIds;
}
