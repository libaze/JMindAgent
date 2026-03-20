package com.itheima.jmindagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.jmindagent.context.UserContextHolder;
import com.itheima.jmindagent.core.sse.SseEmitterManager;
import com.itheima.jmindagent.entity.TRagChunkVector;
import com.itheima.jmindagent.entity.TRagDocument;
import com.itheima.jmindagent.entity.TRagKnowledge;
import com.itheima.jmindagent.entity.dto.response.RagDocListResponse;
import com.itheima.jmindagent.mapper.TRagChunkVectorMapper;
import com.itheima.jmindagent.mapper.TRagDocumentMapper;
import com.itheima.jmindagent.service.ITRagDocumentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.jmindagent.entity.dto.request.RagDocUploadRequest;
import com.itheima.jmindagent.entity.dto.response.RagDocUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
public class TRagDocumentServiceImpl extends ServiceImpl<TRagDocumentMapper, TRagDocument> implements ITRagDocumentService {

    private final TRagChunkVectorMapper chunkVectorMapper;
    private final SseEmitterManager sseEmitterManager;
    private final EmbeddingModel embeddingModel;
    private final TRagKnowledgeServiceImpl ragKnowledgeService;

    // 异步处理线程池
    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    // 批量处理配置
    private static final int BATCH_SIZE = 10;

    public TRagDocumentServiceImpl(TRagChunkVectorMapper chunkVectorMapper,
                                   SseEmitterManager sseEmitterManager,
                                   EmbeddingModel embeddingModel, TRagKnowledgeServiceImpl ragKnowledgeService) {
        this.chunkVectorMapper = chunkVectorMapper;
        this.sseEmitterManager = sseEmitterManager;
        this.embeddingModel = embeddingModel;
        this.ragKnowledgeService = ragKnowledgeService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SseEmitter uploadDocumentWithProgress(RagDocUploadRequest request) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        SseEmitter emitter = sseEmitterManager.createEmitter(sessionId, 60L * 1000L);
        // 从 ThreadLocal 获取当前用户 ID
        Long userId = UserContextHolder.getUserId();
        try {
            sendProgress(emitter, sessionId, "初始化", "正在准备上传...", 0);

            MultipartFile file = request.getFile();
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("上传文件不能为空");
            }

            String fileName = file.getOriginalFilename();
            String fileType = getFileType(fileName);
            if (!isValidFileType(fileType)) {
                throw new IllegalArgumentException("不支持的文件类型，仅支持 txt/pdf/md 格式");
            }

            sendProgress(emitter, sessionId, "文件验证", "文件格式验证通过", 10);

            String kbId = request.getKbId();
            String savePath = saveFileToResources(file, kbId, fileName);
            sendProgress(emitter, sessionId, "文件保存", "原始文件已保存", 20);

            TRagDocument document = new TRagDocument();
            document.setDocId(UUID.randomUUID().toString().replace("-", "").toUpperCase());
            document.setKbId(kbId);
            document.setDocName(fileName);
            document.setDocType(fileType);
            document.setDocSize(file.getSize());
            document.setDocStatus(1);
            document.setCreateTime(LocalDateTime.now());
            document.setUpdateTime(LocalDateTime.now());

            boolean saved = this.save(document);
            if (!saved) {
                throw new RuntimeException("文档信息保存失败");
            }
            sendProgress(emitter, sessionId, "文档记录", "文档信息已保存：" + document.getDocId(), 30);

            List<String> chunks = splitDocument(savePath, fileType);
            log.info("文档分片完成：共{}个分片", chunks.size());
            sendProgress(emitter, sessionId, "文档分片", "已分成 " + chunks.size() + " 个片段", 40);

            sendProgress(emitter, sessionId, "向量化处理", "开始批量调用 Embedding 模型...", 50);

            final String docId = document.getDocId();
            CompletableFuture.runAsync(() -> {
                try {
                    processChunksBatchOptimized(emitter, sessionId, document, chunks);
                    sendProgress(emitter, sessionId, "完成", "文档处理完成！", 100);
                    sseEmitterManager.sendComplete(sessionId);
                } catch (Exception e) {
                    log.error("异步处理文档失败：sessionId={}", sessionId, e);
                    try {
                        // 更新文档状态为失败
                        updateDocumentStatus(docId, 3);
                        sendError(emitter, sessionId, "处理失败：" + e.getMessage());
                        sseEmitterManager.closeEmitter(sessionId);
                    } catch (Exception ex) {
                        log.error("发送错误消息失败", ex);
                    }
                }
            }, executor);

        } catch (Exception e) {
            log.error("文档上传失败：userId={}, kbId={}, error={}", userId, request.getKbId(), e.getMessage());
            try {
                sendError(emitter, sessionId, "上传失败：" + e.getMessage());
                sseEmitterManager.closeEmitter(sessionId);
            } catch (Exception ex) {
                log.error("发送错误消息失败", ex);
            }
        }

        return emitter;
    }

    @Override
    public RagDocListResponse getDocumentList(String kbId, Long userId) {
        // 1. 参数校验
        if (kbId == null || kbId.isBlank()) {
            throw new IllegalArgumentException("知识库 ID 不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID 不能为空");
        }

        // 2. 验证知识库归属权（使用 ragKnowledgeService 查询）
        TRagKnowledge knowledge = ragKnowledgeService.getById(kbId);

        if (knowledge == null) {
            throw new IllegalArgumentException("知识库不存在或无权访问");
        }

        // 验证是否属于当前用户
        if (!knowledge.getUserId().equals(userId)) {
            throw new IllegalArgumentException("知识库不存在或无权访问");
        }

        // 3. 查询文档列表
        LambdaQueryWrapper<TRagDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TRagDocument::getKbId, kbId)
                .orderByDesc(TRagDocument::getCreateTime);

        List<TRagDocument> documentList = this.list(queryWrapper);

        // 4. 转换为响应对象
        List<RagDocListResponse.RagDocListItemResponse> list = documentList.stream()
                .map(doc -> {
                    String statusDesc = getDocStatusDescription(doc.getDocStatus());
                    return RagDocListResponse.RagDocListItemResponse.builder()
                            .docId(doc.getDocId())
                            .docName(doc.getDocName())
                            .docType(doc.getDocType())
                            .docSize(doc.getDocSize())
                            .docStatus(doc.getDocStatus())
                            .statusDesc(statusDesc)
                            .chunkCount(doc.getChunkCount() != null ? doc.getChunkCount() : 0)
                            .createTime(doc.getCreateTime())
                            .updateTime(doc.getUpdateTime())
                            .build();
                })
                .collect(java.util.stream.Collectors.toList());

        // 5. 构建响应结果
        return RagDocListResponse.builder()
                .list(list)
                .total(list.size())
                .build();
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(String docId, String kbId, Long userId) {
        // 1. 参数校验
        if (docId == null || docId.isBlank()) {
            throw new IllegalArgumentException("文档 ID 不能为空");
        }
        if (kbId == null || kbId.isBlank()) {
            throw new IllegalArgumentException("知识库 ID 不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        log.info("开始删除文档：docId={}, kbId={}, userId={}", docId, kbId, userId);

        // 2. 查询文档信息
        TRagDocument document = this.getById(docId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在");
        }

        // 3. 验证文档归属权
        if (!document.getKbId().equals(kbId)) {
            throw new IllegalArgumentException("文档不属于该知识库");
        }

        TRagKnowledge knowledge = ragKnowledgeService.getById(kbId);
        if (knowledge == null || !knowledge.getUserId().equals(userId)) {
            throw new IllegalArgumentException("知识库不存在或无权访问");
        }

        // 4. 删除向量数据
        LambdaQueryWrapper<TRagChunkVector> vectorQueryWrapper = new LambdaQueryWrapper<>();
        vectorQueryWrapper.eq(TRagChunkVector::getDocId, docId);
        chunkVectorMapper.delete(vectorQueryWrapper);
        log.info("已删除向量数据：docId={}", docId);

        // 5. 删除本地文件
        deleteLocalFile(document.getDocName(), kbId);
        log.info("已删除本地文件：fileName={}, kbId={}", document.getDocName(), kbId);

        // 6. 删除文档记录
        this.removeById(docId);
        log.info("已删除文档记录：docId={}", docId);

        // 7. 更新知识库的文档数量
        updateKnowledgeDocCount(kbId, -1);

        log.info("文档删除完成：docId={}, kbId={}", docId, kbId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAllDocumentsByKbId(String kbId, Long userId) {
        // 1. 参数校验
        if (kbId == null || kbId.isBlank()) {
            throw new IllegalArgumentException("知识库 ID 不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        log.info("开始批量删除知识库文档：kbId={}, userId={}", kbId, userId);

        // 2. 验证知识库归属权
        TRagKnowledge knowledge = ragKnowledgeService.getById(kbId);
        if (knowledge == null || !knowledge.getUserId().equals(userId)) {
            throw new IllegalArgumentException("知识库不存在或无权访问");
        }

        // 3. 查询该知识库下的所有文档
        LambdaQueryWrapper<TRagDocument> documentQueryWrapper = new LambdaQueryWrapper<>();
        documentQueryWrapper.eq(TRagDocument::getKbId, kbId);
        List<TRagDocument> documents = this.list(documentQueryWrapper);

        if (documents.isEmpty()) {
            log.info("该知识库下没有文档：kbId={}", kbId);
            deleteKnowledgeDirectory(kbId);
            return;
        }

        log.info("找到{}个文档待删除", documents.size());

        // 4. 删除每个文档及其关联数据
        for (TRagDocument document : documents) {
            try {
                // 删除向量数据
                LambdaQueryWrapper<TRagChunkVector> vectorQueryWrapper = new LambdaQueryWrapper<>();
                vectorQueryWrapper.eq(TRagChunkVector::getDocId, document.getDocId());
                chunkVectorMapper.delete(vectorQueryWrapper);
                log.info("已删除向量数据：docId={}", document.getDocId());

                // 删除本地文件
                deleteLocalFile(document.getDocName(), kbId);

                // 删除文档记录
                this.removeById(document.getDocId());
                log.info("已删除文档记录：docId={}", document.getDocId());
            } catch (Exception e) {
                log.error("删除文档失败：docId={}, error={}", document.getDocId(), e.getMessage());
            }
        }

        // 5. 删除知识库目录
        deleteKnowledgeDirectory(kbId);
        log.info("已删除知识库目录：kbId={}", kbId);

        // 6. 更新知识库的文档数量为 0
        knowledge.setDocCount(0);
        ragKnowledgeService.updateById(knowledge);

        log.info("批量删除完成：kbId={}, 删除文档数={}", kbId, documents.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeWithDocuments(String kbId, Long userId) {
        // 1. 参数校验
        if (kbId == null || kbId.isBlank()) {
            throw new IllegalArgumentException("知识库 ID 不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        log.info("开始删除知识库（含所有文档）：kbId={}, userId={}", kbId, userId);

        // 2. 验证知识库归属权
        TRagKnowledge knowledge = ragKnowledgeService.getById(kbId);
        if (knowledge == null) {
            throw new IllegalArgumentException("知识库不存在");
        }
        if (!knowledge.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除该知识库");
        }

        log.info("知识库信息确认：kbId={}, kbName={}, userId={}", kbId, knowledge.getKbName(), userId);

        // 3. 删除该知识库下的所有文档、向量数据和本地文件
        deleteAllDocumentsByKbId(kbId, userId);
        log.info("已删除知识库下所有文档和向量数据：kbId={}", kbId);

        // 4. 删除知识库记录
        boolean removed = ragKnowledgeService.removeById(kbId);
        if (!removed) {
            throw new RuntimeException("知识库删除失败");
        }
        log.info("已删除知识库记录：kbId={}", kbId);

        log.info("知识库删除完成：kbId={}, kbName={}", kbId, knowledge.getKbName());
    }

    /**
     * 删除本地文件
     */
    private void deleteLocalFile(String fileName, String kbId) {
        try {
            String resourcesPath = System.getProperty("user.dir") +
                    File.separator + "src" +
                    File.separator + "main" +
                    File.separator + "resources";

            Path filePath = Paths.get(resourcesPath, "documents", kbId, fileName);
            File file = filePath.toFile();

            if (file.exists() && file.isFile()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    log.warn("删除文件失败：filePath={}", filePath);
                }
            } else {
                log.info("文件不存在，跳过删除：filePath={}", filePath);
            }
        } catch (Exception e) {
            log.error("删除本地文件异常：fileName={}, kbId={}, error={}", fileName, kbId, e.getMessage());
        }
    }

    /**
     * 删除知识库目录
     */
    private void deleteKnowledgeDirectory(String kbId) {
        try {
            String resourcesPath = System.getProperty("user.dir") +
                    File.separator + "src" +
                    File.separator + "main" +
                    File.separator + "resources";

            Path kbDir = Paths.get(resourcesPath, "documents", kbId);
            File kbDirectory = kbDir.toFile();

            if (kbDirectory.exists() && kbDirectory.isDirectory()) {
                // 递归删除目录及其内容
                deleteDirectoryRecursive(kbDirectory);
                log.info("知识库目录已删除：kbId={}", kbId);
            } else {
                log.info("知识库目录不存在，跳过删除：kbId={}", kbId);
            }
        } catch (Exception e) {
            log.error("删除知识库目录异常：kbId={}, error={}", kbId, e.getMessage());
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectoryRecursive(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectoryRecursive(file);
                }
            }
        }
        directory.delete();
    }

    /**
     * 更新知识库的文档数量
     */
    private void updateKnowledgeDocCount(String kbId, int delta) {
        try {
            TRagKnowledge knowledge = ragKnowledgeService.getById(kbId);
            if (knowledge != null && knowledge.getDocCount() != null) {
                int newCount = Math.max(0, knowledge.getDocCount() + delta);
                knowledge.setDocCount(newCount);
                ragKnowledgeService.updateById(knowledge);
                log.info("更新知识库文档数量：kbId={}, newCount={}", kbId, newCount);
            }
        } catch (Exception e) {
            log.error("更新知识库文档数量失败：kbId={}, error={}", kbId, e.getMessage());
        }
    }

    /**
     * 更新文档状态
     * @param docId 文档 ID
     * @param status 状态码：1-处理中，2-已完成，3-失败
     */
    private void updateDocumentStatus(String docId, int status) {
        try {
            TRagDocument document = this.getById(docId);
            if (document != null) {
                document.setDocStatus(status);
                document.setUpdateTime(LocalDateTime.now());
                this.updateById(document);
                log.info("更新文档状态：docId={}, status={}", docId, status);
            }
        } catch (Exception e) {
            log.error("更新文档状态失败：docId={}, error={}", docId, e.getMessage());
        }
    }

    /**
     * 获取文档状态描述
     * @param status 状态码
     * @return 状态描述
     */
    private String getDocStatusDescription(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 1 -> "处理中";
            case 2 -> "已完成";
            case 3 -> "失败";
            default -> "未知";
        };
    }

    /**
     * 优化的批量分片处理（带进度推送）
     */
    private void processChunksBatchOptimized(SseEmitter emitter, String sessionId,
                                             TRagDocument document, List<String> chunks) throws Exception {
        int totalChunks = chunks.size();
        AtomicInteger processedCount = new AtomicInteger(0);

        List<TRagChunkVector> allChunkVectors = new ArrayList<>();

        // 分批处理，每批 BATCH_SIZE 个
        for (int i = 0; i < chunks.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, chunks.size());
            List<String> batchChunks = chunks.subList(i, endIndex);

            log.info("处理批次：{}/{}", (i / BATCH_SIZE) + 1, (totalChunks + BATCH_SIZE - 1) / BATCH_SIZE);

            // 批量生成嵌入向量
            List<float[]> batchVectors = generateBatchEmbeddings(batchChunks);

            // 构建向量对象
            for (int j = 0; j < batchChunks.size(); j++) {
                int chunkIndex = i + j;
                String chunkContent = batchChunks.get(j);
                float[] vector = batchVectors.get(j);

                TRagChunkVector chunkVector = new TRagChunkVector();
                chunkVector.setChunkId(UUID.randomUUID().toString().replace("-", "").toUpperCase());
                chunkVector.setKbId(document.getKbId());
                chunkVector.setDocId(document.getDocId());
                chunkVector.setChunkContent(chunkContent);
                chunkVector.setChunkIndex(chunkIndex + 1);
                chunkVector.setEmbeddingVector(toPgVector(vector));
                chunkVector.setVectorStatus(1);
                chunkVector.setCreateTime(LocalDateTime.now());

                allChunkVectors.add(chunkVector);
            }

            // 更新进度
            int current = processedCount.addAndGet(batchChunks.size());
            int progress = 50 + (current * 40 / totalChunks);
            sendProgress(emitter, sessionId, "向量化中",
                    "正在处理第 " + current + "/" + totalChunks + " 个片段", progress);

            // 每批保存一次
            if (allChunkVectors.size() >= BATCH_SIZE) {
                batchSaveChunksOptimized(new ArrayList<>(allChunkVectors), document);
                allChunkVectors.clear();
            }
        }

        // 保存剩余的分片
        if (!allChunkVectors.isEmpty()) {
            batchSaveChunksOptimized(new ArrayList<>(allChunkVectors), document);
        }

        // 更新文档统计信息
        document.setChunkCount(totalChunks);
        document.setDocStatus(2); // 设置为已完成状态
        this.updateById(document);

        // 更新知识库的文档数量
        updateKnowledgeDocCount(document.getKbId(), 1);

        sendProgress(emitter, sessionId, "保存完成",
                "已向量化并保存 " + totalChunks + " 个片段", 95);
    }

    /**
     * 批量生成嵌入向量
     */
    private List<float[]> generateBatchEmbeddings(List<String> contents) {
        List<float[]> vectors = new ArrayList<>();

        try {
            // 批量调用 Embedding 模型
            EmbeddingResponse response = embeddingModel.embedForResponse(contents);

            if (response != null && response.getResults() != null) {
                for (var result : response.getResults()) {
                    vectors.add(result.getOutput());
                }
            } else {
                throw new RuntimeException("Embedding 响应为空");
            }

        } catch (Exception e) {
            log.error("批量向量化失败，尝试单个处理：error={}", e.getMessage());
            // 降级为单个处理
            for (String content : contents) {
                try {
                    float[] vector = generateEmbedding(content);
                    vectors.add(vector);
                } catch (Exception ex) {
                    log.error("单个向量化失败：content={}",
                            content.substring(0, Math.min(50, content.length())));
                    // 使用零向量
                    vectors.add(new float[1024]);
                }
            }
        }

        return vectors;
    }

    /**
     * 优化的批量保存（使用 MyBatis-Plus 批量插入）
     */
    @Transactional(rollbackFor = Exception.class)
    protected void batchSaveChunksOptimized(List<TRagChunkVector> chunkVectors, TRagDocument document) {
        if (chunkVectors.isEmpty()) {
            return;
        }

        try {
            // 使用 MyBatis-Plus 的 saveBatch 方法
            boolean success = chunkVectorMapper.insertBatch(chunkVectors);

            if (success) {
                log.info("批量保存成功：{} 个分片向量", chunkVectors.size());
            } else {
                log.warn("批量保存部分失败，尝试逐个保存");
                // 降级为逐个保存
                for (TRagChunkVector cv : chunkVectors) {
                    try {
                        chunkVectorMapper.insert(cv);
                    } catch (Exception e) {
                        log.error("单个保存失败：chunkIndex={}, error={}",
                                cv.getChunkIndex(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("批量保存异常：error={}", e.getMessage(), e);
            // 回滚处理
            throw new RuntimeException("批量保存失败：" + e.getMessage(), e);
        }
    }

    /**
     * 调用 Embedding 模型生成向量（保留作为降级方案）
     */
    private float[] generateEmbedding(String content) {
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(content));
            if (response != null && response.getResult() != null) {
                return response.getResult().getOutput();
            }
            throw new RuntimeException("Embedding 响应为空");
        } catch (Exception e) {
            log.error("调用 Embedding 模型失败：content={}, error={}",
                    content.substring(0, Math.min(50, content.length())), e.getMessage());
            throw new RuntimeException("向量化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将 float 数组转换为 PostgreSQL vector 格式字符串
     * 格式：[0.123,0.456,0.789]
     */
    private String toPgVector(float[] vector) {
        if (vector == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(String.format("%.6f", vector[i]));
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 发送进度消息
     */
    private void sendProgress(SseEmitter emitter, String sessionId,
                            String stage, String message, int progress) throws IOException {
        Map<String, Object> data = Map.of(
            "stage", stage,
            "message", message,
            "progress", progress
        );

        sseEmitterManager.send(sessionId, SseEmitter.event()
                .name("progress")
                .data(data)
                .build());

        log.info("发送进度：sessionId={}, stage={}, progress={}", sessionId, stage, progress);
    }

    /**
     * 发送错误消息
     */
    private void sendError(SseEmitter emitter, String sessionId, String errorMsg) throws IOException {
        Map<String, Object> data = Map.of(
            "error", errorMsg
        );

        sseEmitterManager.send(sessionId, SseEmitter.event()
                .name("error")
                .data(data)
                .build());
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null) {
            return "unknown";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "unknown";
    }

    /**
     * 校验文件类型
     */
    private boolean isValidFileType(String fileType) {
        return "txt".equals(fileType) ||
                "pdf".equals(fileType) ||
                "md".equals(fileType) ||
                "markdown".equals(fileType);
    }

    /**
     * 保存文件到 resources 目录下的知识库 ID 文件夹
     */
    private String saveFileToResources(MultipartFile file, String kbId, String fileName) {
        try {
            String resourcesPath = System.getProperty("user.dir") +
                                   File.separator + "src" +
                                   File.separator + "main" +
                                   File.separator + "resources";

            Path kbDir = Paths.get(resourcesPath, "documents", kbId);
            if (!Files.exists(kbDir)) {
                Files.createDirectories(kbDir);
            }

            Path filePath = kbDir.resolve(fileName);
            file.transferTo(filePath.toFile());

            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败：" + e.getMessage(), e);
        }
    }

    /**
     * 文档分片处理（根据文件类型选择不同策略）
     */
    private List<String> splitDocument(String filePath, String fileType) {
        try {
            log.info("开始文档分片：filePath={}, fileType={}", filePath, fileType);

            List<Document> documents = switch (fileType) {
                case "pdf" -> readPdfFile(filePath);
                case "md", "markdown" -> readMarkdownFile(filePath);
                default -> readTextFile(filePath);
            };

            log.info("文档读取完成：共{}个文档对象", documents.size());

            // 从 Document 对象中提取文本内容
            List<String> chunks = new ArrayList<>();
            for (Document doc : documents) {
                String content = doc.getFormattedContent();
                if (content == null || content.isBlank()) {
                    content = doc.toString();
                }
                chunks.add(content);
            }

            log.info("文档分片完成：生成{}个分片", chunks.size());
            return chunks;

        } catch (Exception e) {
            throw new RuntimeException("文档分片失败：" + e.getMessage(), e);
        }
    }

    /**
     * 读取 PDF 文件（使用 Spring AI PDF Reader）
     */
    private List<Document> readPdfFile(String filePath) {
        try {
            log.info("使用 PDF Reader 读取文件：{}", filePath);

            // 检查文件是否存在
            File file = new File(filePath);
            if (!file.exists()) {
                throw new RuntimeException("文件不存在：" + filePath);
            }

            // 使用 FileSystemResource 包装文件路径
            Resource resource = new FileSystemResource(file);
            // 创建 PDF Reader，传入 Resource 对象
            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
            List<Document> documents = reader.get();

            log.info("PDF 读取成功：共{}页", documents.size());
            return documents;

        } catch (Exception e) {
            log.error("PDF 读取失败：filePath={}", filePath, e);
            throw new RuntimeException("PDF 文件读取失败：" + e.getMessage(), e);
        }
    }

    /**
     * 读取 Markdown 文件（直接读取为单个 Document）
     */
    private List<Document> readMarkdownFile(String filePath) {
        try {
            log.info("读取 Markdown 文件：{}", filePath);

            Path path = Paths.get(filePath);
            String content = Files.readString(path, StandardCharsets.UTF_8);

            Document document = new Document(content);

            log.info("Markdown 读取成功：文件大小={} bytes", content.length());
            return List.of(document);

        } catch (Exception e) {
            log.error("Markdown 文件读取失败：filePath={}", filePath, e);
            throw new RuntimeException("Markdown 文件读取失败：" + e.getMessage(), e);
        }
    }

    /**
     * 读取普通文本文件（直接读取为单个 Document）
     */
    private List<Document> readTextFile(String filePath) {
        try {
            log.info("读取文本文件：{}", filePath);

            Path path = Paths.get(filePath);
            String content = Files.readString(path, StandardCharsets.UTF_8);

            Document document = new Document(content);

            log.info("文本文件读取成功：文件大小={} bytes", content.length());
            return List.of(document);

        } catch (Exception e) {
            log.error("文本文件读取失败：filePath={}", filePath, e);
            throw new RuntimeException("文本文件读取失败：" + e.getMessage(), e);
        }
    }

}
