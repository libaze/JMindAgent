package com.itheima.jmindagent.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天服务接口
 */
public interface IChatService {

    /**
     * 统一智能会话接口（SSE 流式）
     * 根据参数自动选择对话模式：
     * - Agent 模式：传入 agentId，基于自定义Agent对话（可调用工具）
     * - 普通模式：未传入 agentId，降级为基础AI对话
     *
     * 知识库检索增强（两种模式都支持）：
     * - 传入 kbId 时：启用知识库检索
     * - 未传入 kbId 时：禁用知识库检索
     *
     * @param userId 用户 ID
     * @param sessionId 会话ID
     * @param content 用户消息
     * @param agentId 自定义Agent ID（选填）
     * @param kbId 知识库 ID（选填）
     * @return SSE 发射器
     */
    SseEmitter unifiedChatStream(Long userId, String sessionId, String content, String agentId, String kbId);
}
