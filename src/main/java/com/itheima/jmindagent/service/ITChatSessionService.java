package com.itheima.jmindagent.service;

import com.itheima.jmindagent.entity.TChatSession;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.jmindagent.entity.dto.response.ChatSessionCreateResponse;
import com.itheima.jmindagent.entity.dto.response.ChatSessionListResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author
 * @since 2026-03-16
 */
public interface ITChatSessionService extends IService<TChatSession> {

    /**
     * 创建新的对话会话
     * @param userId 用户 ID
     * @param sessionName 会话名称（选填）
     * @return 会话创建响应信息
     */
    ChatSessionCreateResponse createSession(Long userId, String sessionName);

    /**
     * 查询用户会话列表
     * @param userId 用户 ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页会话列表
     */
    ChatSessionListResponse getSessionList(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 删除会话及关联的历史记录和 Redis 缓存
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    void deleteSession(String sessionId, Long userId);
}
