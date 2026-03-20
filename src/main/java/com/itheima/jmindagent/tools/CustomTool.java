package com.itheima.jmindagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 自定义工具接口
 * 所有自定义工具都需要实现此接口
 */
public interface CustomTool {

    /**
     * 获取工具名称（工具包 ID，用于 Agent 绑定）
     * @return 工具包 ID（英文，如 calculator）
     */
    String getToolName();

    /**
     * 获取工具描述（工具包的总体描述）
     * @return 工具包描述
     */
    String getToolDescription();

    /**
     * 获取工具中文别名（用于前端展示）
     * 默认返回工具名称，子类可重写以提供友好的中文名
     * @return 工具中文别名
     */
    default String getToolAlias() {
        return getToolName();
    }
}
