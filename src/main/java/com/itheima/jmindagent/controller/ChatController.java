package com.itheima.jmindagent.controller;

import com.itheima.jmindagent.context.UserContextHolder;
import com.itheima.jmindagent.entity.Result;
import com.itheima.jmindagent.entity.dto.request.ChatHistoryRequest;
import com.itheima.jmindagent.entity.dto.request.ChatMessageRequest;
import com.itheima.jmindagent.entity.dto.request.ChatSessionCreateRequest;
import com.itheima.jmindagent.entity.dto.request.ChatSessionListRequest;
import com.itheima.jmindagent.entity.dto.response.ChatHistoryItemResponse;
import com.itheima.jmindagent.entity.dto.response.ChatSessionCreateResponse;
import com.itheima.jmindagent.entity.dto.response.ChatSessionListResponse;
import com.itheima.jmindagent.service.IChatService;
import com.itheima.jmindagent.service.ITChatHistoryService;
import com.itheima.jmindagent.service.ITChatSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 聊天会话控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/session")
public class ChatController {

    @Autowired
    private ITChatSessionService chatSessionService;
    @Autowired
    private IChatService chatService;
    @Autowired
    private ITChatHistoryService chatHistoryService;

    /**
     * 创建新的对话会话
     * 接口地址：/api/chat/session/create
     * 请求方式：POST
     * 接口描述：创建新的对话会话，生成唯一会话ID，初始化会话记忆
     * 请求参数：sessionName(字符串，选填，会话名称)
     * 响应参数：sessionId(字符串，会话ID)、sessionName、createTime
     *
     * @param request 会话创建请求
     * @return 统一响应结果
     */
    @PostMapping("/create")
    public Result createChatSession(@RequestBody ChatSessionCreateRequest request) {
        try {
            // if (request.getUserId() == null) {
            //     return Result.paramError("用户 ID 不能为空");
            // }
            // 从 ThreadLocal 获取当前用户ID
            Long userId = UserContextHolder.getUserId();

            ChatSessionCreateResponse response = chatSessionService.createSession(
                    userId,
                    request.getSessionName()
            );

            return Result.success("会话创建成功", response);
        } catch (IllegalArgumentException e) {
            log.error("创建会话失败：{}", e.getMessage());
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            log.error("创建会话异常", e);
            return Result.serverError("会话创建失败，请稍后重试");
        }
    }

    /**
     * 查询用户会话列表
     * 接口地址：/api/chat/session/list
     * 请求方式：GET
     * 接口描述：查询当前用户所有对话会话
     * 请求参数：pageNum(整型，选填，页码)、pageSize(整型，选填，每页条数)
     * 响应参数：会话列表集合，包含 sessionId、sessionName、updateTime、lastContent
     *
     * @param request 会话列表查询请求
     * @return 统一响应结果
     */
    @GetMapping("/list")
    public Result getChatSessionList(ChatSessionListRequest request) {
        try {
            // if (request.getUserId() == null) {
            //     return Result.paramError("用户 ID 不能为空");
            // }
            // 从 ThreadLocal 获取当前用户ID
            Long userId = UserContextHolder.getUserId();

            ChatSessionListResponse response = chatSessionService.getSessionList(
                    userId,
                    request.getPageNum(),
                    request.getPageSize()
            );

            return Result.success("查询成功", response);
        } catch (IllegalArgumentException e) {
            log.error("查询会话列表失败：{}", e.getMessage());
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            log.error("查询会话列表异常", e);
            return Result.serverError("查询会话列表失败，请稍后重试");
        }
    }

    /**
     * 统一智能会话接口（SSE 流式）
     * 接口地址：/api/chat/unified/stream
     * 请求方式：POST
     * 接口描述：统一的 AI对话接口，采用优先级降级策略：
     * - Agent 模式：传入 agentId，基于自定义Agent对话（可调用工具）
     * - 普通模式：未传入 agentId，降级为基础AI对话
     *
     * 知识库检索增强（两种模式都支持）：
     * - 传入 kbId 时：启用知识库检索
     * - 未传入 kbId 时：禁用知识库检索
     *
     * 请求参数：
     * - sessionId(字符串，必填)、content(字符串，必填)
     * - agentId(字符串，选填，存在时使用 Agent 模式，否则降级为普通对话)
     * - kbId(字符串，选填，所有模式都支持知识库检索)
     * 响应形式：SSE 实时推送，包含状态反馈和生成内容
     *
     * @param request 聊天消息请求
     * @return SSE 流式响应
     */
    @PostMapping(value = "/unified/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter unifiedChatStream(@RequestBody ChatMessageRequest request) {
        try {
            // if (request.getUserId() == null) {
            //     throw new IllegalArgumentException("用户 ID 不能为空");
            // }
            // 从 ThreadLocal 获取当前用户ID
            Long userId = UserContextHolder.getUserId();

            if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                throw new IllegalArgumentException("会话ID 不能为空");
            }
            if (request.getContent() == null || request.getContent().isBlank()) {
                throw new IllegalArgumentException("消息内容不能为空");
            }

            String chatMode = (request.getAgentId() != null && !request.getAgentId().isBlank()) ? "AGENT" : "NORMAL";
            String ragStatus = (request.getKbId() != null && !request.getKbId().isBlank()) ? "ENABLED" : "DISABLED";

            log.info("统一聊天请求：userId={}, sessionId={}, chatMode={}, ragStatus={}, agentId={}, kbId={}",
                    userId, request.getSessionId(), chatMode, ragStatus,
                    request.getAgentId(), request.getKbId());

            return chatService.unifiedChatStream(
                    userId,
                    request.getSessionId(),
                    request.getContent(),
                    request.getAgentId(),
                    request.getKbId()
            );

        } catch (IllegalArgumentException e) {
            log.error("统一聊天请求参数错误：{}", e.getMessage());
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"msg\":\"参数错误：" + e.getMessage() + "\"}"));
                emitter.complete();
            } catch (Exception ex) {
                log.error("发送错误消息失败", ex);
            }
            return emitter;
        } catch (Exception e) {
            log.error("统一聊天请求异常", e);
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"msg\":\"服务器错误\"}"));
                emitter.complete();
            } catch (Exception ex) {
                log.error("发送错误消息失败", ex);
            }
            return emitter;
        }
    }

    /**
     * 查询会话历史记录
     * 接口地址：/api/chat/history/list
     * 请求方式：GET
     * 接口描述：查询指定会话的历史对话记录，支持上下文记忆回溯
     * 请求参数：sessionId(字符串，必填)
     * 响应参数：历史消息列表，包含消息 ID、发送方、内容、时间、类型（普通/RAG/工具调用）
     *
     * @param request 历史记录查询请求
     * @return 统一响应结果
     */
    @GetMapping("/history/list")
    public Result getChatHistory(ChatHistoryRequest request) {
        try {
            // 参数校验
            if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                return Result.paramError("会话ID 不能为空");
            }
            // 从 ThreadLocal 获取当前用户ID
            Long userId = UserContextHolder.getUserId();
            // if (request.getUserId() == null) {
            //     return Result.paramError("用户 ID 不能为空");
            // }

            log.info("查询聊天记录：userId={}, sessionId={}", userId, request.getSessionId());

            // 查询历史记录
            List<ChatHistoryItemResponse> historyList = chatHistoryService.getHistoryBySessionId(
                    request.getSessionId(),
                    userId
            );

            return Result.success("查询成功", historyList);
        } catch (IllegalArgumentException e) {
            log.error("查询历史记录失败：{}", e.getMessage());
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            log.error("查询历史记录异常", e);
            return Result.serverError("查询历史记录失败，请稍后重试");
        }
    }

    /**
     * 删除会话
     * 接口地址：/api/chat/session/delete/{sessionId}
     * 请求方式：DELETE
     * 接口描述：删除指定的对话会话，同时删除关联的历史记录和 Redis 缓存
     * 请求参数：sessionId(路径参数，字符串，必填)
     * 响应参数：无
     *
     * @param sessionId 会话ID
     * @return 统一响应结果
     */
    @DeleteMapping("/delete/{sessionId}")
    public Result deleteChatSession(@PathVariable String sessionId) {
        try {
            // 从 ThreadLocal 获取当前用户ID
            Long userId = UserContextHolder.getUserId();

            // 参数校验
            if (sessionId == null || sessionId.isBlank()) {
                return Result.paramError("会话ID 不能为空");
            }

            log.info("删除会话请求：userId={}, sessionId={}", userId, sessionId);

            // 调用服务层删除会话
            chatSessionService.deleteSession(sessionId, userId);

            return Result.success("会话删除成功", null);
        } catch (IllegalArgumentException e) {
            log.error("删除会话失败：{}", e.getMessage());
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            log.error("删除会话异常", e);
            return Result.serverError("删除会话失败，请稍后重试");
        }
    }

}
