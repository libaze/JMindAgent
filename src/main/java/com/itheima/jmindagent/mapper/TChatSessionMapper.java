package com.itheima.jmindagent.mapper;

import com.itheima.jmindagent.entity.TChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.jmindagent.entity.dto.response.ChatSessionItemResponse;
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
public interface TChatSessionMapper extends BaseMapper<TChatSession> {

    /**
     * 查询用户会话列表（包含最后一条消息内容）
     * @param userId 用户 ID
     * @param offset 偏移量
     * @param limit 每页条数
     * @return 会话列表
     */
    List<ChatSessionItemResponse> selectSessionList(@Param("userId") Long userId,
                                                     @Param("offset") Integer offset,
                                                     @Param("limit") Integer limit);

    /**
     * 查询用户会话总数
     * @param userId 用户 ID
     * @return 会话总数
     */
    long countSessionList(@Param("userId") Long userId);
}
