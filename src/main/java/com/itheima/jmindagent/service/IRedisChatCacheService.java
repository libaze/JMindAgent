package com.itheima.jmindagent.service;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Redis 聊天缓存服务接口
 */
public interface IRedisChatCacheService {

    /**
     * 保存消息到 Redis 缓存
     * @param sessionId 会话 ID
     * @param userId 用户 ID
     * @param message 消息对象
     * @param isUserMessage 是否为用户消息
     */
    void saveMessageToCache(String sessionId, Long userId, Message message, boolean isUserMessage);

    /**
     * 从 Redis 加载历史消息
     * @param sessionId 会话 ID
     * @param userId 用户 ID
     * @param maxMessages 最大加载消息数
     * @return 消息列表
     */
    List<Message> loadMessagesFromCache(String sessionId, Long userId, int maxMessages);

    /**
     * 删除 Redis 中的会话缓存
     * @param sessionId 会话 ID
     * @param userId 用户 ID
     */
    void clearSessionCache(String sessionId, Long userId);

    /**
     * 检查 Redis 中是否存在该会话的缓存
     * @param sessionId 会话 ID
     * @param userId 用户 ID
     * @return true-存在缓存，false-无缓存
     */
    boolean hasSessionCache(String sessionId, Long userId);

}
