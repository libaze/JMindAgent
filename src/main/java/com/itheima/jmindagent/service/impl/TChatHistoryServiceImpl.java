package com.itheima.jmindagent.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.itheima.jmindagent.entity.TChatHistory;
import com.itheima.jmindagent.entity.dto.response.ChatHistoryItemResponse;
import com.itheima.jmindagent.mapper.TChatHistoryMapper;
import com.itheima.jmindagent.service.ITChatHistoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
public class TChatHistoryServiceImpl extends ServiceImpl<TChatHistoryMapper, TChatHistory> implements ITChatHistoryService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private TChatHistoryMapper chatHistoryMapper;
    @Autowired
    private RedisChatCacheServiceImpl redisChatCacheService;

    private static final String CACHE_KEY_PREFIX = "chat:session:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Override
    public void saveUserMessage(String sessionId, Long userId, String content) {
        try {
            TChatHistory history = new TChatHistory();
            history.setSessionId(sessionId);
            history.setUserId(userId);
            history.setSenderType(1);
            history.setContent(content);
            history.setMsgType(1);
            history.setCreateTime(LocalDateTime.now());
            this.save(history);

            UserMessage userMessage = new UserMessage(content);
            redisChatCacheService.saveMessageToCache(sessionId, userId, userMessage, true);

            log.debug("用户消息已双写：PGSQL 和 Redis，sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("保存用户消息失败：sessionId={}, error={}", sessionId, e.getMessage());
            throw new RuntimeException("保存用户消息失败", e);
        }
    }

    @Override
    public void saveAiMessage(String sessionId, Long userId, String content) {
        try {
            TChatHistory history = new TChatHistory();
            history.setSessionId(sessionId);
            history.setUserId(userId);
            history.setSenderType(2);
            history.setContent(content);
            history.setMsgType(1);
            history.setCreateTime(LocalDateTime.now());
            this.save(history);

            AssistantMessage aiMessage = new AssistantMessage(content);
            redisChatCacheService.saveMessageToCache(sessionId, userId, aiMessage, false);

            log.debug("AI 消息已双写：PGSQL 和 Redis，sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("保存 AI 消息失败：sessionId={}, error={}", sessionId, e.getMessage());
            throw new RuntimeException("保存 AI 消息失败", e);
        }
    }

    @Override
    public List<ChatHistoryItemResponse> getHistoryBySessionId(String sessionId, Long userId) {
        try {
            if (redisChatCacheService.hasSessionCache(sessionId, userId)) {
                log.debug("Redis 缓存命中，从缓存加载历史消息：sessionId={}", sessionId);
                return loadFromRedis(sessionId, userId);
            }

            log.info("Redis 缓存未命中，从 PGSQL 加载：sessionId={}", sessionId);
            return loadAndCacheFromDatabase(sessionId, userId);

        } catch (Exception e) {
            log.error("从 Redis 加载失败，降级到 PGSQL：sessionId={}, error={}", sessionId, e.getMessage());
            return loadFromDatabaseOnly(sessionId, userId);
        }
    }

    @Override
    public List<Message> loadChatMemory(String sessionId, Long userId, int maxMessages) {
        try {
            if (redisChatCacheService.hasSessionCache(sessionId, userId)) {
                List<Message> cachedMessages = redisChatCacheService.loadMessagesFromCache(sessionId, userId, maxMessages);
                if (!cachedMessages.isEmpty()) {
                    log.info("Redis 缓存命中：sessionId={}, 消息数={}", sessionId, cachedMessages.size());
                    return cachedMessages;
                }
            }

            log.info("Redis 缓存未命中，从 PGSQL 加载：sessionId={}", sessionId);
            return loadMessagesFromDatabase(sessionId, userId, maxMessages);

        } catch (Exception e) {
            log.error("加载历史消息失败：sessionId={}, error={}", sessionId, e.getMessage());
            return loadMessagesFromDatabase(sessionId, userId, maxMessages);
        }
    }

    /**
     * 从 Redis 加载历史消息（返回 ChatHistoryItemResponse）
     */
    private List<ChatHistoryItemResponse> loadFromRedis(String sessionId, Long userId) {
        String cacheKey = buildCacheKey(sessionId, userId);

        List<String> jsonList = stringRedisTemplate.opsForList().range(cacheKey, 0, -1);

        if (jsonList == null || jsonList.isEmpty()) {
            log.warn("Redis 缓存为空，切换到数据库：sessionId={}", sessionId);
            return loadAndCacheFromDatabase(sessionId, userId);
        }

        List<ChatHistoryItemResponse> responses = new ArrayList<>();
        for (String json : jsonList) {
            try {
                RedisChatCacheServiceImpl.CachedMessageDTO cached =
                        JSONUtil.toBean(json, RedisChatCacheServiceImpl.CachedMessageDTO.class);

                ChatHistoryItemResponse response = ChatHistoryItemResponse.builder()
                        .msgId(null)
                        .sessionId(sessionId)
                        .userId(userId)
                        .senderType(cached.getSenderType())
                        .senderDesc(cached.getSenderType() == 1 ? "user" : "ai")
                        .content(cached.getContent())
                        .msgType(cached.getMsgType())
                        .msgTypeDesc(getMsgTypeDesc(cached.getMsgType()))
                        .createTime(cached.getCreateTime())
                        .build();

                responses.add(response);
            } catch (Exception e) {
                log.warn("JSON 反序列化失败，跳过此消息：error={}", e.getMessage());
            }
        }

        log.info("从 Redis 加载历史消息成功（JSON 格式）：sessionId={}, 消息数={}", sessionId, responses.size());
        return responses;
    }

    /**
     * 从数据库加载并缓存到 Redis（返回 ChatHistoryItemResponse）
     */
    private List<ChatHistoryItemResponse> loadAndCacheFromDatabase(String sessionId, Long userId) {
        List<ChatHistoryItemResponse> responses = loadFromDatabaseOnly(sessionId, userId);
        cacheToRedis(sessionId, userId, responses);
        return responses;
    }

    /**
     * 仅从数据库加载（返回 ChatHistoryItemResponse）
     */
    private List<ChatHistoryItemResponse> loadFromDatabaseOnly(String sessionId, Long userId) {
        LambdaQueryWrapper<TChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TChatHistory::getSessionId, sessionId)
               .eq(TChatHistory::getUserId, userId)
               .orderByAsc(TChatHistory::getCreateTime);

        List<TChatHistory> historyList = this.list(wrapper);

        return historyList.stream()
                .map(history -> ChatHistoryItemResponse.builder()
                        .msgId(history.getMsgId())
                        .sessionId(history.getSessionId())
                        .userId(history.getUserId())
                        .senderType(history.getSenderType())
                        .senderDesc(history.getSenderType() == 1 ? "user" : "ai")
                        .content(history.getContent())
                        .msgType(history.getMsgType())
                        .msgTypeDesc(getMsgTypeDesc(history.getMsgType()))
                        .createTime(history.getCreateTime())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 从数据库加载消息并转换为 Message 对象（带数量限制）
     */
    private List<Message> loadMessagesFromDatabase(String sessionId, Long userId, int maxMessages) {
        LambdaQueryWrapper<TChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TChatHistory::getSessionId, sessionId)
                .eq(TChatHistory::getUserId, userId)
                .orderByDesc(TChatHistory::getCreateTime)
                .last("LIMIT " + maxMessages);

        List<TChatHistory> historyList = this.list(wrapper);

        List<TChatHistory> reversedList = historyList.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            java.util.Collections.reverse(list);
                            return list;
                        }
                ));

        List<Message> messages = reversedList.stream()
                .map(history -> {
                    if (history.getSenderType() == 1) {
                        return new UserMessage(history.getContent());
                    } else {
                        return new AssistantMessage(history.getContent());
                    }
                })
                .collect(Collectors.toList());

        log.info("从 PGSQL 加载历史消息：sessionId={}, 加载{}条", sessionId, messages.size());
        return messages;
    }

    /**
     * 将数据缓存到 Redis（JSON 格式）
     */
    private void cacheToRedis(String sessionId, Long userId, List<ChatHistoryItemResponse> responses) {
        try {
            String cacheKey = buildCacheKey(sessionId, userId);

            stringRedisTemplate.delete(cacheKey);

            for (ChatHistoryItemResponse response : responses) {
                RedisChatCacheServiceImpl.CachedMessageDTO cachedMessage =
                        new RedisChatCacheServiceImpl.CachedMessageDTO();
                cachedMessage.setSenderId(userId);
                cachedMessage.setSenderType(response.getSenderType());
                cachedMessage.setContent(response.getContent());
                cachedMessage.setMsgType(response.getMsgType());
                cachedMessage.setCreateTime(response.getCreateTime());

                String json = JSONUtil.toJsonStr(cachedMessage);
                stringRedisTemplate.opsForList().rightPush(cacheKey, json);
            }

            stringRedisTemplate.expire(cacheKey, Duration.ofHours(CACHE_EXPIRE_HOURS));

            log.debug("缓存预热完成（JSON 格式）：sessionId={}, 缓存{}条消息", sessionId, responses.size());

        } catch (Exception e) {
            log.warn("缓存到 Redis 失败：sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    /**
     * 构建 Redis 缓存 Key
     */
    private String buildCacheKey(String sessionId, Long userId) {
        return CACHE_KEY_PREFIX + userId + ":" + sessionId;
    }

    /**
     * 获取消息类型描述
     */
    private String getMsgTypeDesc(Integer msgType) {
        if (msgType == null) {
            return "普通";
        }
        switch (msgType) {
            case 1: return "普通";
            case 2: return "RAG";
            case 3: return "工具调用";
            default: return "普通";
        }
    }
}
