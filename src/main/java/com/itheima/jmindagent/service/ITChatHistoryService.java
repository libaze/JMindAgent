package com.itheima.jmindagent.service;

import com.itheima.jmindagent.entity.TChatHistory;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.jmindagent.entity.dto.response.ChatHistoryItemResponse;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author
 * @since 2026-03-16
 */
public interface ITChatHistoryService extends IService<TChatHistory> {

    /**
     * 保存用户消息
     * @param sessionId 会话ID
     * @param userId 用户 ID
     * @param content 消息内容
     */
    void saveUserMessage(String sessionId, Long userId, String content);

    /**
     * 保存 AI 消息
     * @param sessionId 会话ID
     * @param userId 用户 ID
     * @param content 消息内容
     */
    void saveAiMessage(String sessionId, Long userId, String content);

    /**
     * 查询指定会话的历史对话记录
     * @param sessionId 会话ID
     * @param userId 用户 ID
     * @return 历史消息列表
     */
    List<ChatHistoryItemResponse> getHistoryBySessionId(String sessionId, Long userId);

    /**
     * 加载会话的历史消息并转换为 ChatMemory 格式
     * @param sessionId 会话ID
     * @param userId 用户 ID
     * @param maxMessages 最大加载消息数（用于控制上下文长度）
     * @return Spring AI 的 Message 列表
     */
    List<Message> loadChatMemory(String sessionId, Long userId, int maxMessages);
}
