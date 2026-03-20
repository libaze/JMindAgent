package com.itheima.jmindagent.service.impl;

import cn.hutool.json.JSONUtil;
import com.itheima.jmindagent.entity.TChatHistory;
import com.itheima.jmindagent.mapper.TChatHistoryMapper;
import com.itheima.jmindagent.service.IRedisChatCacheService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RedisChatCacheServiceImpl implements IRedisChatCacheService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private TChatHistoryMapper chatHistoryMapper;

    private static final String CACHE_KEY_PREFIX = "chat:session:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Override
    public void saveMessageToCache(String sessionId, Long userId, Message message, boolean isUserMessage) {
        try {
            String cacheKey = buildCacheKey(sessionId, userId);

            CachedMessageDTO cachedMessage = new CachedMessageDTO();
            cachedMessage.setSenderId(userId);
            cachedMessage.setSenderType(isUserMessage ? 1 : 2);
            cachedMessage.setContent(message.getText());
            cachedMessage.setMsgType(1);
            cachedMessage.setCreateTime(java.time.LocalDateTime.now());

            String json = JSONUtil.toJsonStr(cachedMessage);
            stringRedisTemplate.opsForList().rightPush(cacheKey, json);

            stringRedisTemplate.expire(cacheKey, Duration.ofHours(CACHE_EXPIRE_HOURS));

            log.debug("保存消息到 Redis 缓存：sessionId={}, msgType={}", sessionId, isUserMessage ? "user" : "ai");

        } catch (Exception e) {
            log.warn("缓存消息失败：sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    @Override
    public List<Message> loadMessagesFromCache(String sessionId, Long userId, int maxMessages) {
        String cacheKey = buildCacheKey(sessionId, userId);

        List<String> jsonList = stringRedisTemplate.opsForList().range(cacheKey, 0, -1);

        if (jsonList == null || jsonList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> messages = new ArrayList<>();
        int startIndex = Math.max(0, jsonList.size() - maxMessages);

        for (int i = startIndex; i < jsonList.size(); i++) {
            String json = jsonList.get(i);
            try {
                CachedMessageDTO cached = JSONUtil.toBean(json, CachedMessageDTO.class);

                if (cached.getSenderType() == 1) {
                    messages.add(new UserMessage(cached.getContent()));
                } else {
                    messages.add(new AssistantMessage(cached.getContent()));
                }
            } catch (Exception e) {
                log.warn("反序列化消息失败：error={}", e.getMessage());
            }
        }

        log.debug("从 Redis 缓存加载{}条消息：sessionId={}", messages.size(), sessionId);
        return messages;
    }

    @Override
    public boolean hasSessionCache(String sessionId, Long userId) {
        String cacheKey = buildCacheKey(sessionId, userId);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(cacheKey));
    }

    @Override
    public void clearSessionCache(String sessionId, Long userId) {
        String cacheKey = buildCacheKey(sessionId, userId);
        stringRedisTemplate.delete(cacheKey);
        log.info("清除会话缓存：sessionId={}", sessionId);
    }

    /**
     * 构建 Redis 缓存 Key
     */
    private String buildCacheKey(String sessionId, Long userId) {
        return CACHE_KEY_PREFIX + userId + ":" + sessionId;
    }

    /**
     * 缓存消息 DTO
     */
    @Data
    public static class CachedMessageDTO {
        private Long senderId;
        private Integer senderType;
        private String content;
        private Integer msgType;
        private java.time.LocalDateTime createTime;
    }
}
