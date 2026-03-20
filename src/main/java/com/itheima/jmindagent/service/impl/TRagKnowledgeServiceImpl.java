package com.itheima.jmindagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.jmindagent.context.UserContextHolder;
import com.itheima.jmindagent.entity.TRagChunkVector;
import com.itheima.jmindagent.entity.TRagDocument;
import com.itheima.jmindagent.entity.TRagKnowledge;
import com.itheima.jmindagent.entity.dto.request.RagKbUpdateRequest;
import com.itheima.jmindagent.entity.dto.response.RagKbDetailResponse;
import com.itheima.jmindagent.entity.dto.response.RagKbListItemResponse;
import com.itheima.jmindagent.entity.dto.response.RagKbListResponse;
import com.itheima.jmindagent.mapper.TRagChunkVectorMapper;
import com.itheima.jmindagent.mapper.TRagKnowledgeMapper;
import com.itheima.jmindagent.service.ITRagDocumentService;
import com.itheima.jmindagent.service.ITRagKnowledgeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.jmindagent.entity.dto.request.RagKbCreateRequest;
import com.itheima.jmindagent.entity.dto.response.RagKbCreateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
public class TRagKnowledgeServiceImpl extends ServiceImpl<TRagKnowledgeMapper, TRagKnowledge> implements ITRagKnowledgeService {

    @Override
    public RagKbCreateResponse createKnowledge(RagKbCreateRequest request) {
        // 从 ThreadLocal 获取当前用户ID
        Long userId = UserContextHolder.getUserId();

        // 1. 构建知识库实体
        TRagKnowledge knowledge = new TRagKnowledge();
        knowledge.setKbId(UUID.randomUUID().toString().replace("-", "").toUpperCase());
        knowledge.setUserId(userId);
        knowledge.setKbName(request.getKbName());
        knowledge.setKbDesc(request.getKbDesc());
        knowledge.setDocCount(0); // 初始文档数量为 0
        knowledge.setKbStatus(1); // 默认启用状态
        knowledge.setCreateTime(LocalDateTime.now());
        knowledge.setUpdateTime(LocalDateTime.now());

        log.info("创建知识库：{}", knowledge);

        // 2. 保存到数据库
        boolean saved = this.save(knowledge);
        if (!saved) {
            throw new RuntimeException("知识库创建失败");
        }

        // 3. 构建响应结果
        return RagKbCreateResponse.builder()
                .kbId(knowledge.getKbId())
                .kbName(knowledge.getKbName())
                .createTime(knowledge.getCreateTime())
                .build();
    }

    @Override
    public RagKbDetailResponse getKbDetail(String kbId) {
        if (kbId == null || kbId.isBlank()) {
            throw new IllegalArgumentException("知识库 ID 不能为空");
        }

        // 1. 查询知识库信息
        TRagKnowledge knowledge = this.getById(kbId);
        if (knowledge == null) {
            throw new IllegalArgumentException("知识库不存在：" + kbId);
        }

        // 2. 构建响应对象
        return RagKbDetailResponse.builder()
                .kbId(knowledge.getKbId())
                .userId(knowledge.getUserId())
                .kbName(knowledge.getKbName())
                .kbDesc(knowledge.getKbDesc())
                .docCount(knowledge.getDocCount())
                .kbStatus(knowledge.getKbStatus())
                .statusDesc(getStatusDescription(knowledge.getKbStatus()))
                .createTime(knowledge.getCreateTime())
                .updateTime(knowledge.getUpdateTime())
                .build();
    }

    @Override
    public RagKbListResponse getUserKbList(Long userId, Integer pageNum, Integer pageSize) {
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

        // 1. 使用 MyBatis-Plus 分页查询
        Page<TRagKnowledge> page = new Page<>(pageNum, pageSize);

        // 2. 构建查询条件：按用户 ID 过滤，按创建时间倒序
        LambdaQueryWrapper<TRagKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TRagKnowledge::getUserId, userId)
                .orderByDesc(TRagKnowledge::getCreateTime);

        // 3. 执行分页查询
        Page<TRagKnowledge> resultPage = this.page(page, wrapper);

        // 4. 转换为响应对象
        List<RagKbListItemResponse> list = resultPage.getRecords().stream()
                .map(knowledge -> RagKbListItemResponse.builder()
                        .kbId(knowledge.getKbId())
                        .kbName(knowledge.getKbName())
                        .kbDesc(knowledge.getKbDesc() != null ? knowledge.getKbDesc() : "")
                        .docCount(knowledge.getDocCount() != null ? knowledge.getDocCount() : 0)
                        .kbStatus(knowledge.getKbStatus())
                        .createTime(knowledge.getCreateTime())
                        .updateTime(knowledge.getUpdateTime())
                        .build())
                .collect(Collectors.toList());

        // 5. 计算总页数
        int totalPages = (int) Math.ceil((double) resultPage.getTotal() / pageSize);

        // 6. 构建响应结果
        return RagKbListResponse.builder()
                .list(list)
                .total(resultPage.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RagKbDetailResponse updateKnowledge(String kbId, Long userId, RagKbUpdateRequest request) {
        // 1. 参数校验
        if (kbId == null || kbId.isBlank()) {
            throw new IllegalArgumentException("知识库 ID 不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID 不能为空");
        }
        if (request == null) {
            throw new IllegalArgumentException("更新请求参数不能为空");
        }

        // 2. 查询知识库信息，验证归属权
        TRagKnowledge knowledge = this.getById(kbId);
        if (knowledge == null) {
            throw new IllegalArgumentException("知识库不存在");
        }
        if (!knowledge.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改该知识库");
        }

        log.info("开始更新知识库：kbId={}, userId={}", kbId, userId);

        // 3. 选择性更新字段（只更新非空字段）
        boolean hasUpdate = false;

        if (request.getKbName() != null && !request.getKbName().trim().isEmpty()) {
            knowledge.setKbName(request.getKbName());
            hasUpdate = true;
        }

        if (request.getKbDesc() != null) {
            knowledge.setKbDesc(request.getKbDesc());
            hasUpdate = true;
        }

        if (request.getKbStatus() != null) {
            knowledge.setKbStatus(request.getKbStatus());
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new IllegalArgumentException("至少需要更新一个字段");
        }

        // 4. 更新时间
        knowledge.setUpdateTime(LocalDateTime.now());

        // 5. 保存到数据库
        boolean updated = this.updateById(knowledge);
        if (!updated) {
            throw new RuntimeException("知识库更新失败");
        }

        log.info("知识库更新成功：kbId={}", kbId);

        // 6. 构建响应对象
        return RagKbDetailResponse.builder()
                .kbId(knowledge.getKbId())
                .userId(knowledge.getUserId())
                .kbName(knowledge.getKbName())
                .kbDesc(knowledge.getKbDesc())
                .docCount(knowledge.getDocCount())
                .kbStatus(knowledge.getKbStatus())
                .statusDesc(getStatusDescription(knowledge.getKbStatus()))
                .createTime(knowledge.getCreateTime())
                .updateTime(knowledge.getUpdateTime())
                .build();
    }

    /**
     * 获取状态描述
     * @param status 状态码
     * @return 状态描述
     */
    private String getStatusDescription(Integer status) {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "启用" : "禁用";
    }
}
