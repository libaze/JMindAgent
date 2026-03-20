package com.itheima.jmindagent.service;

import com.itheima.jmindagent.entity.dto.response.ToolInfoResponse;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Set;

/**
 * 本地工具管理服务接口
 */
public interface ILocalToolService {

    /**
     * 获取所有已注册的本地工具（按工具包分组）
     * @return 工具包信息列表
     */
    List<ToolInfoResponse> getAllToolPackages();

    /**
     * 根据工具包 ID 列表获取指定的工具
     * @param toolPackageIds 工具包 ID 列表（逗号分隔）
     * @return 工具回调列表（包含所有方法）
     */
    List<ToolCallback> getToolsByPackageIds(String toolPackageIds);

    /**
     * 刷新工具注册表
     */
    void refreshTools();

    /**
     * 获取所有可用工具的信息（用于前端展示，按工具包分组）
     * @return 工具包信息列表
     */
    List<ToolInfoResponse> getAllToolInfos();

    /**
     * 根据工具包 ID 获取工具包信息
     * @param toolPackageId 工具包 ID
     * @return 工具包信息
     */
    ToolInfoResponse getToolPackageById(String toolPackageId);
}
