package com.itheima.jmindagent.controller;

import com.itheima.jmindagent.entity.Result;
import com.itheima.jmindagent.entity.dto.response.ToolInfoResponse;
import com.itheima.jmindagent.service.ILocalToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工具管理控制器
 * 提供工具查询、管理等功能
 */
@Slf4j
@Tag(name = "工具管理", description = "提供可用工具查询等相关接口")
@RestController
@RequestMapping("/api/tool")
public class ToolController {

    @Autowired
    private ILocalToolService localToolService;

    /**
     * 查询可用工具包列表（按工具包分组）
     * @return 可用工具包信息列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询可用工具包列表", description = "查询系统中所有可用的工具包信息，每个工具包包含多个具体工具方法")
    public Result listTools() {
        log.info("查询可用工具包列表");

        try {
            List<ToolInfoResponse> toolInfos = localToolService.getAllToolPackages();
            return Result.success("查询成功", toolInfos);

        } catch (Exception e) {
            log.error("查询可用工具包列表失败", e);
            return Result.serverError("查询失败：" + e.getMessage());
        }
    }
}
