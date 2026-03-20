package com.itheima.jmindagent.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.itheima.jmindagent.entity.dto.response.ToolInfoResponse;
import com.itheima.jmindagent.service.ILocalToolService;
import com.itheima.jmindagent.tools.CustomTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LocalToolServiceImpl implements ILocalToolService {

    @Autowired
    private ApplicationContext applicationContext;

    // 缓存已注册的工具包（按工具类分组）
    // key: 工具包 ID (如 calculator), value: 该工具包下的所有工具方法
    private final Map<String, List<ToolCallback>> toolPackages = new ConcurrentHashMap<>();

    // 缓存工具实例
    private final Map<String, CustomTool> toolInstances = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("初始化本地工具管理器...");
        refreshTools();
    }

    @Override
    public void refreshTools() {
        log.info("开始刷新工具注册表...");

        try {
            // 清空现有工具
            toolPackages.clear();
            toolInstances.clear();

            // 获取所有实现了 CustomTool 接口的 Bean
            Map<String, CustomTool> customToolBeans = applicationContext.getBeansOfType(CustomTool.class);

            log.info("发现{}个自定义工具 Bean", customToolBeans.size());

            // 遍历所有工具 Bean
            for (Map.Entry<String, CustomTool> entry : customToolBeans.entrySet()) {
                String beanName = entry.getKey();
                CustomTool toolBean = entry.getValue();

                log.info("处理工具 Bean: {}, 实例：{}", beanName, toolBean.getClass().getName());

                // 获取工具包 ID
                String toolPackageId = toolBean.getToolName();

                // 保存工具实例
                toolInstances.put(toolPackageId, toolBean);

                // 为每个工具 Bean 创建 ToolCallback
                List<ToolCallback> callbacks = createToolCallbacks(toolBean);

                // 按工具包 ID 分组存储
                toolPackages.put(toolPackageId, callbacks);

                log.info("注册工具包：{}, 包含{}个方法", toolPackageId, callbacks.size());
                for (ToolCallback callback : callbacks) {
                    log.info("  - 方法：{} -> {}", callback.getToolDefinition().name(),
                             callback.getToolDefinition().description());
                }
            }

            log.info("工具注册表刷新完成，共注册{}个工具包", toolPackages.size());

        } catch (Exception e) {
            log.error("刷新工具注册表失败", e);
        }
    }

    @Override
    public List<ToolInfoResponse> getAllToolPackages() {
        return getAllToolInfos();
    }

    @Override
    public List<ToolCallback> getToolsByPackageIds(String toolPackageIds) {
        if (toolPackageIds == null || toolPackageIds.isBlank()) {
            return List.of();
        }

        List<ToolCallback> result = new ArrayList<>();
        String[] ids = toolPackageIds.split(",");

        for (String packageId : ids) {
            String trimmedId = packageId.trim();
            List<ToolCallback> callbacks = toolPackages.get(trimmedId);
            if (callbacks != null) {
                result.addAll(callbacks);
                log.info("找到工具包：{}, 包含{}个方法", trimmedId, callbacks.size());
            } else {
                log.warn("未找到工具包：{}", trimmedId);
            }
        }

        log.info("根据工具包 IDs 获取到{}个工具方法", result.size());
        return result;
    }

    @Override
    public List<ToolInfoResponse> getAllToolInfos() {
        List<ToolInfoResponse> toolInfos = new ArrayList<>();

        for (Map.Entry<String, List<ToolCallback>> entry : toolPackages.entrySet()) {
            String toolPackageId = entry.getKey();
            List<ToolCallback> callbacks = entry.getValue();
            CustomTool toolInstance = toolInstances.get(toolPackageId);

            if (callbacks.isEmpty()) {
                continue;
            }

            // 构建工具方法列表
            List<ToolInfoResponse.ToolMethodItem> methodItems = new ArrayList<>();
            for (ToolCallback callback : callbacks) {
                ToolInfoResponse.ToolMethodItem methodItem =
                        ToolInfoResponse.ToolMethodItem.builder()
                                .methodId(callback.getToolDefinition().name())
                                .methodName(callback.getToolDefinition().name())
                                .methodDescription(callback.getToolDefinition().description())
                                .build();
                methodItems.add(methodItem);
            }

            // 构建工具包信息
            ToolInfoResponse toolInfo = ToolInfoResponse.builder()
                    .toolPackageId(toolPackageId)
                    .toolAlias(toolInstance != null ? toolInstance.getToolAlias() : toolPackageId)
                    .toolDescription(toolInstance != null ? toolInstance.getToolDescription() : callbacks.get(0).getToolDefinition().description())
                    .toolType("local")
                    .methods(methodItems)
                    .build();

            toolInfos.add(toolInfo);
        }

        log.info("获取到{}个工具包信息", toolInfos.size());
        return toolInfos;
    }


    @Override
    public ToolInfoResponse getToolPackageById(String toolPackageId) {
        List<ToolCallback> callbacks = toolPackages.get(toolPackageId);
        if (callbacks == null || callbacks.isEmpty()) {
            return null;
        }

        CustomTool toolInstance = toolInstances.get(toolPackageId);

        // 构建工具方法列表
        List<ToolInfoResponse.ToolMethodItem> methodItems = new ArrayList<>();
        for (ToolCallback callback : callbacks) {
            ToolInfoResponse.ToolMethodItem methodItem =
                    ToolInfoResponse.ToolMethodItem.builder()
                            .methodId(callback.getToolDefinition().name())
                            .methodName(callback.getToolDefinition().name())
                            .methodDescription(callback.getToolDefinition().description())
                            .build();
            methodItems.add(methodItem);
        }

        return ToolInfoResponse.builder()
                .toolPackageId(toolPackageId)
                .toolAlias(toolInstance != null ? toolInstance.getToolAlias() : toolPackageId)
                .toolDescription(toolInstance != null ? toolInstance.getToolDescription() : callbacks.get(0).getToolDefinition().description())
                .toolType("local")
                .methods(methodItems)
                .build();
    }

    /**
     * 为工具 Bean 创建 ToolCallback
     */
    private List<ToolCallback> createToolCallbacks(CustomTool toolBean) {
        List<ToolCallback> callbacks = new ArrayList<>();

        try {
            // 使用方法级别的工具回调提供者
            ToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                    .toolObjects(toolBean)
                    .build();

            ToolCallback[] toolCallbacks = provider.getToolCallbacks();

            if (toolCallbacks != null && toolCallbacks.length > 0) {
                for (ToolCallback callback : toolCallbacks) {
                    callbacks.add(callback);
                }
            }

            log.debug("工具 {} 创建了{}个回调方法", toolBean.getToolName(), callbacks.size());

        } catch (Exception e) {
            log.error("创建工具回调失败：{}", toolBean.getToolName(), e);
        }

        return callbacks;
    }
}
