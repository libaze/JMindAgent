package com.itheima.jmindagent.service.impl;

import com.itheima.jmindagent.entity.TMcpTool;
import com.itheima.jmindagent.mapper.TMcpToolMapper;
import com.itheima.jmindagent.service.ILocalToolService;
import com.itheima.jmindagent.service.ITMcpToolService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
public class TMcpToolServiceImpl extends ServiceImpl<TMcpToolMapper, TMcpTool> implements ITMcpToolService {

    @Autowired
    private ILocalToolService localToolService;

    @Override
    public List<ToolCallback> getAvailableTools(String toolPackageIds) {
        log.info("获取可用工具，toolPackageIds={}", toolPackageIds);

        List<ToolCallback> allTools = new ArrayList<>();

        // 1. 获取本地工具
        if (toolPackageIds != null && !toolPackageIds.isBlank()) {
            List<ToolCallback> localTools = localToolService.getToolsByPackageIds(toolPackageIds);
            allTools.addAll(localTools);
            log.info("加载了{}个本地工具", localTools.size());
        }

        // 2. TODO: 未来可以添加 MCP 远程工具
        // List<ToolCallback> mcpTools = loadMcpTools(toolPackageIds);
        // allTools.addAll(mcpTools);

        log.info("总共加载了{}个工具", allTools.size());
        return allTools;
    }
}
