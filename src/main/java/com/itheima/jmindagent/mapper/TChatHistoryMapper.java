package com.itheima.jmindagent.mapper;

import com.itheima.jmindagent.entity.TChatHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.jmindagent.entity.dto.response.ChatHistoryItemResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author
 * @since 2026-03-16
 */
public interface TChatHistoryMapper extends BaseMapper<TChatHistory> {

    /**
     * 查询指定会话的历史对话记录
     * @param sessionId 会话ID
     * @param userId 用户 ID
     * @return 历史消息列表（按时间正序排列）
     */
    List<ChatHistoryItemResponse> selectHistoryBySessionId(@Param("sessionId") String sessionId,
                                                            @Param("userId") Long userId);
}
