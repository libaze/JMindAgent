package com.itheima.jmindagent.controller;

import com.itheima.jmindagent.context.UserContextHolder;
import com.itheima.jmindagent.entity.Result;
import com.itheima.jmindagent.entity.dto.request.AgentCreateRequest;
import com.itheima.jmindagent.entity.dto.request.AgentUpdateRequest;
import com.itheima.jmindagent.entity.dto.response.AgentCreateResponse;
import com.itheima.jmindagent.entity.dto.response.AgentListResponse;
import com.itheima.jmindagent.service.ITCustomAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 自定义Agent 管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final ITCustomAgentService agentService;

    /**
     * 创建自定义Agent 接口
     * 接口地址：/api/agent/create
     * 请求方式：POST
     * 接口描述：创建专属自定义Agent，配置基础信息和绑定工具
     *
     * @param request Agent 创建请求参数
     * @return 统一响应结果，包含 agentId、agentName、agentDesc、toolList
     */
    @PostMapping("/create")
    public Result createAgent(@RequestBody AgentCreateRequest request) {
        // 1. 参数校验
        if (request == null) {
            return Result.paramError("请求参数不能为空");
        }

        // if (request.getUserId() == null) {
        //     return Result.paramError("用户ID 不能为空");
        // }

        if (request.getAgentName() == null || request.getAgentName().trim().isEmpty()) {
            return Result.paramError("Agent 名称不能为空");
        }

        // if (request.getToolIds() == null || request.getToolIds().isEmpty()) {
        //     return Result.paramError("绑定的工具 ID 集合不能为空");
        // }

        try {
            // 2. 调用服务层创建 Agent
            AgentCreateResponse response = agentService.createAgent(request);

            // 3. 返回成功响应
            return Result.success(response);
        } catch (IllegalArgumentException e) {
            // 4. 处理业务异常
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            // 5. 处理其他异常
            return Result.serverError("创建 Agent 失败：" + e.getMessage());
        }
    }

    /**
     * 自定义Agent列表查询接口
     * 接口地址：/api/agent/list
     * 请求方式：GET
     * 接口描述：查询当前用户创建的所有自定义Agent
     *
     * @return 统一响应结果，包含 Agent列表
     */
    @GetMapping("/list")
    public Result getAgentList() {
        try {
            // 1. 参数校验
            // if (userId == null) {
            //     return Result.paramError("用户 ID 不能为空");
            // }
            // 从 ThreadLocal 获取当前用户ID
            Long userId = UserContextHolder.getUserId();

            // 2. 调用服务层查询列表
            AgentListResponse response = agentService.getAgentListByUserId(userId);

            // 3. 返回成功响应
            return Result.success(response);

        } catch (IllegalArgumentException e) {
            // 4. 处理业务异常
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            // 5. 处理其他异常
            return Result.serverError("查询 Agent列表失败：" + e.getMessage());
        }
    }

    /**
     * 删除自定义Agent 接口
     * 接口地址：/api/agent/delete/{agentId}
     * 请求方式：DELETE
     * 接口描述：删除指定的自定义Agent
     *
     * @param agentId Agent ID
     * @return 统一响应结果
     */
    @DeleteMapping("/delete/{agentId}")
    public Result deleteAgent(@PathVariable String agentId) {
        try {
            // 1. 参数校验
            if (agentId == null || agentId.isBlank()) {
                return Result.paramError("Agent ID 不能为空");
            }

            // 2. 从 ThreadLocal 获取当前用户ID
            Long userId = UserContextHolder.getUserId();

            log.info("删除 Agent 请求：userId={}, agentId={}", userId, agentId);

            // 3. 调用服务层删除 Agent
            agentService.deleteAgent(agentId, userId);

            // 4. 返回成功响应
            return Result.success("Agent 删除成功", null);

        } catch (IllegalArgumentException e) {
            // 5. 处理业务异常
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            // 6. 处理其他异常
            return Result.serverError("删除 Agent 失败：" + e.getMessage());
        }
    }

    /**
     * 更新自定义Agent 信息接口
     * 接口地址：/api/agent/update/{agentId}
     * 请求方式：PUT
     * 接口描述：更新自定义Agent 的基础信息（名称、描述、提示词、绑定工具）
     *
     * @param agentId Agent ID
     * @param request Agent 更新请求参数
     * @return 统一响应结果，包含更新后的 Agent 信息
     */
    @PutMapping("/update/{agentId}")
    public Result updateAgent(@PathVariable String agentId, @RequestBody AgentUpdateRequest request) {
        try {
            // 1. 参数校验
            if (agentId == null || agentId.isBlank()) {
                return Result.paramError("Agent ID 不能为空");
            }
            if (request == null) {
                return Result.paramError("请求参数不能为空");
            }

            // 2. 从 ThreadLocal 获取当前用户ID
            Long userId = UserContextHolder.getUserId();

            log.info("更新 Agent 请求：userId={}, agentId={}", userId, agentId);

            // 3. 调用服务层更新 Agent
            AgentCreateResponse response = agentService.updateAgent(agentId, userId, request);

            // 4. 返回成功响应
            return Result.success("Agent 更新成功", response);

        } catch (IllegalArgumentException e) {
            // 5. 处理业务异常
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            // 6. 处理其他异常
            return Result.serverError("更新 Agent 失败：" + e.getMessage());
        }
    }
}
