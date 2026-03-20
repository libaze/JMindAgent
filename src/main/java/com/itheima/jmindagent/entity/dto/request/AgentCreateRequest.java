package com.itheima.jmindagent.entity.dto.request;

import lombok.Data;
import java.util.List;

/**
 * 自定义Agent 创建请求 DTO
 */
@Data
public class AgentCreateRequest {

    /**
     * Agent 名称（必填）
     */
    private String agentName;

    /**
     * Agent 描述（选填）
     */
    private String agentDesc;

    /**
     * 绑定的工具 ID 集合（必填）
     */
    private List<String> toolIds;

    /**
     * Agent 专属提示词（选填）
     */
    private String prompt;
}
