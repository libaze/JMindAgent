package com.itheima.jmindagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.jmindagent.entity.TChatHistory;
import com.itheima.jmindagent.entity.TChatSession;
import com.itheima.jmindagent.entity.dto.response.ChatSessionItemResponse;
import com.itheima.jmindagent.entity.dto.response.ChatSessionListResponse;
import com.itheima.jmindagent.mapper.TChatHistoryMapper;
import com.itheima.jmindagent.mapper.TChatSessionMapper;
import com.itheima.jmindagent.service.IRedisChatCacheService;
import com.itheima.jmindagent.service.ITChatSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.jmindagent.entity.dto.response.ChatSessionCreateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
public class TChatSessionServiceImpl extends ServiceImpl<TChatSessionMapper, TChatSession> implements ITChatSessionService {

    @Autowired
    private IRedisChatCacheService redisChatCacheService;
    @Autowired
    private TChatHistoryMapper chatHistoryMapper;

    @Override
    public ChatSessionCreateResponse createSession(Long userId, String sessionName) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        TChatSession session = new TChatSession();
        session.setSessionId(UUID.randomUUID().toString().replace("-", "").toUpperCase());
        session.setUserId(userId);
        session.setSessionName(sessionName != null ? sessionName : "新建对话");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        log.info("创建会话：{}", session);

        this.save(session);

        return ChatSessionCreateResponse.builder()
                .sessionId(session.getSessionId())
                .sessionName(session.getSessionName())
                .createTime(session.getCreateTime())
                .build();
    }

    @Override
    public ChatSessionListResponse getSessionList(Long userId, Integer pageNum, Integer pageSize) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        // 默认值处理
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        // 计算偏移量
        int offset = (pageNum - 1) * pageSize;

        // 查询会话列表
        List<ChatSessionItemResponse> sessionList = baseMapper.selectSessionList(userId, offset, pageSize);

        // 查询总数
        long total = baseMapper.countSessionList(userId);

        // 计算总页数
        int totalPages = (int) Math.ceil((double) total / pageSize);

        return ChatSessionListResponse.builder()
                .list(sessionList)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId, Long userId) {
        // 1. 参数校验
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("会话ID 不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID 不能为空");
        }

        // 2. 查询会话信息，验证归属权
        TChatSession session = this.getById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除该会话");
        }

        log.info("开始删除会话：sessionId={}, userId={}", sessionId, userId);

        // 3. 删除 Redis 缓存
        try {
            redisChatCacheService.clearSessionCache(sessionId, userId);
            log.info("已删除 Redis 缓存：sessionId={}", sessionId);
        } catch (Exception e) {
            log.warn("删除 Redis 缓存失败：sessionId={}, error={}", sessionId, e.getMessage());
        }

        // 4. 删除会话关联的历史记录
        try {
            LambdaQueryWrapper<TChatHistory> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TChatHistory::getSessionId, sessionId);
            chatHistoryMapper.delete(queryWrapper);
            log.info("已删除会话历史记录：sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("删除历史记录失败：sessionId={}, error={}", sessionId, e.getMessage());
            throw new RuntimeException("删除历史记录失败", e);
        }

        // 5. 删除会话本身
        try {
            this.removeById(sessionId);
            log.info("已删除会话：sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("删除会话失败：sessionId={}, error={}", sessionId, e.getMessage());
            throw new RuntimeException("删除会话失败", e);
        }

        log.info("会话删除完成：sessionId={}, userId={}", sessionId, userId);
    }
}
