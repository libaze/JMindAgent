package com.itheima.jmindagent.service;

import com.itheima.jmindagent.entity.TCustomAgent;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.jmindagent.entity.dto.request.AgentCreateRequest;
import com.itheima.jmindagent.entity.dto.request.AgentUpdateRequest;
import com.itheima.jmindagent.entity.dto.response.AgentCreateResponse;
import com.itheima.jmindagent.entity.dto.response.AgentListResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author
 * @since 2026-03-16
 */
public interface ITCustomAgentService extends IService<TCustomAgent> {

    /**
     * 创建自定义Agent
     * @param request Agent 创建请求参数
     * @return Agent 创建响应结果
     */
    AgentCreateResponse createAgent(AgentCreateRequest request);

    /**
     * 查询用户创建的 Agent列表
     * @param userId 用户ID
     * @return Agent列表响应结果
     */
    AgentListResponse getAgentListByUserId(Long userId);

    /**
     * 删除自定义Agent
     * @param agentId Agent ID
     * @param userId 用户ID
     */
    void deleteAgent(String agentId, Long userId);

    /**
     * 更新自定义Agent 信息
     * @param agentId Agent ID
     * @param userId 用户ID
     * @param request 更新请求参数
     * @return 更新后的 Agent 信息
     */
    AgentCreateResponse updateAgent(String agentId, Long userId, AgentUpdateRequest request);

}
