package com.itheima.jmindagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.jmindagent.context.UserContextHolder;
import com.itheima.jmindagent.entity.TCustomAgent;
import com.itheima.jmindagent.entity.dto.request.AgentCreateRequest;
import com.itheima.jmindagent.entity.dto.request.AgentUpdateRequest;
import com.itheima.jmindagent.entity.dto.response.AgentCreateResponse;
import com.itheima.jmindagent.entity.dto.response.AgentListResponse;
import com.itheima.jmindagent.entity.dto.response.ToolInfoResponse;
import com.itheima.jmindagent.mapper.TCustomAgentMapper;
import com.itheima.jmindagent.service.ILocalToolService;
import com.itheima.jmindagent.service.ITCustomAgentService;
import com.itheima.jmindagent.service.ITMcpToolService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author
 * @since 2026-03-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TCustomAgentServiceImpl extends ServiceImpl<TCustomAgentMapper, TCustomAgent> implements ITCustomAgentService {

    private final ITMcpToolService toolService;

    @Autowired
    private ILocalToolService localToolService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentCreateResponse createAgent(AgentCreateRequest request) {
        // 从 ThreadLocal 获取当前用户ID
        Long userId = UserContextHolder.getUserId();
        // 1. 构建 Agent 实体
        TCustomAgent agent = new TCustomAgent();
        agent.setAgentId(UUID.randomUUID().toString().replace("-", "").toUpperCase());
        agent.setUserId(userId);
        agent.setAgentName(request.getAgentName());
        agent.setAgentDesc(request.getAgentDesc());
        agent.setAgentPrompt(request.getPrompt());

        // 处理工具 IDs（逗号分隔，现在是工具包 ID）
        if (request.getToolIds() != null && !request.getToolIds().isEmpty()) {
            String toolIdsStr = String.join(",", request.getToolIds());
            agent.setToolIds(toolIdsStr);
        }

        agent.setCreateTime(LocalDateTime.now());

        // 2. 保存到数据库
        boolean saved = this.save(agent);
        if (!saved) {
            throw new RuntimeException("Agent 保存失败");
        }

        // 3. 获取绑定的工具包信息
        List<AgentCreateResponse.ToolPackageInfo> toolList = getToolPackageList(request.getToolIds());

        // 4. 构建响应结果
        return AgentCreateResponse.builder()
                .agentId(agent.getAgentId())
                .agentName(agent.getAgentName())
                .agentDesc(agent.getAgentDesc())
                .toolList(toolList)
                .build();
    }

    @Override
    public AgentListResponse getAgentListByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        // 1. 构建查询条件：按用户 ID 过滤，按创建时间倒序
        LambdaQueryWrapper<TCustomAgent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TCustomAgent::getUserId, userId)
               .orderByDesc(TCustomAgent::getCreateTime);

        // 2. 查询 Agent 列表
        List<TCustomAgent> agentList = this.list(wrapper);

        // 3. 转换为响应对象
        List<AgentListResponse.AgentListItemResponse> list = agentList.stream()
                .map(agent -> {
                    // 工具包
                    String toolIdsStr = agent.getToolIds();
                    List<AgentCreateResponse.ToolPackageInfo> tools = null;
                    if (toolIdsStr == null || toolIdsStr.isEmpty()) {
                        tools = List.of();
                    }
                    else {
                        tools = getToolPackageList(Arrays.asList(toolIdsStr.split(",")));
                    }
                    return AgentListResponse.AgentListItemResponse.builder()
                            .agentId(agent.getAgentId())
                            .agentName(agent.getAgentName())
                            .agentDesc(agent.getAgentDesc() != null ? agent.getAgentDesc() : "")
                            .agentPrompt(agent.getAgentPrompt() != null ? agent.getAgentPrompt() : "")
                            .toolList(tools)
                            .createTime(agent.getCreateTime())
                            .build();
                })
                .collect(Collectors.toList());

        // 4. 构建响应结果
        return AgentListResponse.builder()
                .list(list)
                .total(list.size())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgent(String agentId, Long userId) {
        // 1. 参数校验
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("Agent ID 不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID 不能为空");
        }

        // 2. 查询 Agent 信息，验证归属权
        TCustomAgent agent = this.getById(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Agent 不存在");
        }
        if (!agent.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除该 Agent");
        }

        log.info("开始删除 Agent：agentId={}, userId={}", agentId, userId);

        // 3. 删除 Agent
        try {
            this.removeById(agentId);
            log.info("已删除 Agent：agentId={}", agentId);
        } catch (Exception e) {
            log.error("删除 Agent 失败：agentId={}, error={}", agentId, e.getMessage());
            throw new RuntimeException("删除 Agent 失败", e);
        }

        log.info("Agent 删除完成：agentId={}, userId={}", agentId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentCreateResponse updateAgent(String agentId, Long userId, AgentUpdateRequest request) {
        // 1. 参数校验
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("Agent ID 不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID 不能为空");
        }
        if (request == null) {
            throw new IllegalArgumentException("更新请求参数不能为空");
        }

        // 2. 查询 Agent 信息，验证归属权
        TCustomAgent agent = this.getById(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Agent 不存在");
        }
        if (!agent.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改该 Agent");
        }

        log.info("开始更新 Agent：agentId={}, userId={}", agentId, userId);

        // 3. 选择性更新字段（只更新非空字段）
        boolean hasUpdate = false;

        if (request.getAgentName() != null && !request.getAgentName().trim().isEmpty()) {
            agent.setAgentName(request.getAgentName());
            hasUpdate = true;
        }

        if (request.getAgentDesc() != null) {
            agent.setAgentDesc(request.getAgentDesc());
            hasUpdate = true;
        }

        if (request.getPrompt() != null) {
            agent.setAgentPrompt(request.getPrompt());
            hasUpdate = true;
        }

        if (request.getToolIds() != null) {
            // 处理工具 IDs（逗号分隔，现在是工具包 ID）
            if (request.getToolIds().isEmpty()) {
                agent.setToolIds(null);
            } else {
                String toolIdsStr = String.join(",", request.getToolIds());
                agent.setToolIds(toolIdsStr);
            }
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new IllegalArgumentException("至少需要更新一个字段");
        }

        // 4. 保存到数据库
        boolean updated = this.updateById(agent);
        if (!updated) {
            throw new RuntimeException("Agent 更新失败");
        }

        // 5. 获取绑定的工具包信息
        List<AgentCreateResponse.ToolPackageInfo> toolList = getToolPackageList(request.getToolIds());

        // 6. 构建响应结果
        return AgentCreateResponse.builder()
                .agentId(agent.getAgentId())
                .agentName(agent.getAgentName())
                .agentDesc(agent.getAgentDesc())
                .toolList(toolList)
                .build();
    }

    /**
     * 获取工具包列表信息
     * @param toolPackageIds 工具包 ID 列表
     * @return 工具包信息列表
     */
    private List<AgentCreateResponse.ToolPackageInfo> getToolPackageList(List<String> toolPackageIds) {
        if (toolPackageIds == null || toolPackageIds.isEmpty()) {
            return List.of();
        }
        log.info("加载工具包列表：{}", toolPackageIds);

        return toolPackageIds.stream()
                .map(packageId -> {
                    ToolInfoResponse toolPackage = localToolService.getToolPackageById(packageId);
                    if (toolPackage != null) {
                        return AgentCreateResponse.ToolPackageInfo.builder()
                                .toolPackageId(toolPackage.getToolPackageId())
                                .toolAlias(toolPackage.getToolAlias())
                                .toolDesc(toolPackage.getToolDescription())
                                .methodCount(toolPackage.getMethods() != null ? toolPackage.getMethods().size() : 0)
                                .build();
                    }
                    return null;
                })
                .filter(toolInfo -> toolInfo != null)
                .collect(Collectors.toList());
    }
}
