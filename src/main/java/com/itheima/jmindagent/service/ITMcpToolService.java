package com.itheima.jmindagent.service;

import com.itheima.jmindagent.entity.TMcpTool;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author
 * @since 2026-03-16
 */
public interface ITMcpToolService extends IService<TMcpTool> {

    /**
     * 获取可用的工具（包括本地工具和 MCP 工具）
     * @param toolIds 工具 ID 列表（逗号分隔）
     * @return 工具回调列表
     */
    List<ToolCallback> getAvailableTools(String toolIds);
}
