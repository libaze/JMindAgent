package com.itheima.jmindagent.controller;

import com.itheima.jmindagent.context.UserContextHolder;
import com.itheima.jmindagent.entity.Result;
import com.itheima.jmindagent.entity.dto.request.RagDocUploadRequest;
import com.itheima.jmindagent.entity.dto.request.RagKbCreateRequest;
import com.itheima.jmindagent.entity.dto.request.RagKbUpdateRequest;
import com.itheima.jmindagent.entity.dto.response.*;
import com.itheima.jmindagent.service.ITRagDocumentService;
import com.itheima.jmindagent.service.ITRagKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * RAG 知识库管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final ITRagKnowledgeService ragKnowledgeService;
    private final ITRagDocumentService ragDocumentService;

    /**
     * 创建 RAG 知识库接口
     * 接口地址：/api/rag/kb/create
     * 请求方式：POST
     * 接口描述：创建 RAG 知识库，用于存储检索数据源
     *
     * @param request 知识库创建请求参数
     * @return 统一响应结果，包含 kbId、kbName、createTime
     */
    @PostMapping("/kb/create")
    public Result createKnowledge(@RequestBody RagKbCreateRequest request) {
        // 1. 参数校验
        if (request == null) {
            return Result.paramError("请求参数不能为空");
        }

        if (request.getKbName() == null || request.getKbName().trim().isEmpty()) {
            return Result.paramError("知识库名称不能为空");
        }

        try {
            // 2. 调用服务层创建知识库
            RagKbCreateResponse response = ragKnowledgeService.createKnowledge(request);

            // 3. 返回成功响应
            return Result.success(response);
        } catch (IllegalArgumentException e) {
            // 4. 处理业务异常
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            // 5. 处理其他异常
            return Result.serverError("创建知识库失败：" + e.getMessage());
        }
    }

    /**
     * 获取知识库详细信息接口
     * 接口地址：/api/rag/kb/detail
     * 请求方式：GET
     * 接口描述：根据知识库 ID 获取单个知识库的完整详细信息，包括名称、描述、文档数量、状态等。
     *
     * @param kbId 知识库 ID（必填）
     * @return 统一响应结果，包含知识库完整信息
     */
    @GetMapping("/kb/detail")
    public Result getKbDetail(@RequestParam("kbId") String kbId) {
        try {
            // 1. 参数校验
            if (kbId == null || kbId.trim().isEmpty()) {
                return Result.paramError("知识库 ID 不能为空");
            }

            // 2. 调用服务层获取详情
            RagKbDetailResponse response = ragKnowledgeService.getKbDetail(kbId);

            // 3. 返回成功响应
            return Result.success(response);

        } catch (IllegalArgumentException e) {
            // 4. 处理业务异常
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            // 5. 处理其他异常
            return Result.serverError("获取知识库详情失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户知识库列表接口
     * 接口地址：/api/rag/kb/list
     * 请求方式：GET
     * 接口描述：分页获取指定用户下的 RAG 知识库列表，支持按创建时间倒序排列。
     *
     * @param pageNum 页码（选填，默认 1）
     * @param pageSize 每页条数（选填，默认 10）
     * @return 统一响应结果，包含分页知识库列表
     */
    @GetMapping("/kb/list")
    public Result getKbList(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        try {
            // 1. 参数校验
            // if (userId == null) {
            //     return Result.paramError("用户 ID 不能为空");
            // }
            // 从 ThreadLocal 获取当前用户ID
            Long userId = UserContextHolder.getUserId();

            // 2. 调用服务层获取列表
            RagKbListResponse response = ragKnowledgeService.getUserKbList(userId, pageNum, pageSize);

            // 3. 返回成功响应
            return Result.success(response);

        } catch (IllegalArgumentException e) {
            // 4. 处理业务异常
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            // 5. 处理其他异常
            return Result.serverError("获取知识库列表失败：" + e.getMessage());
        }
    }

    /**
     * 上传文档至知识库接口（SSE 流式进度）
     * 接口地址：/api/rag/doc/upload-stream
     * 请求方式：POST
     * 接口描述：上传文档至指定知识库，通过 SSE 实时推送处理进度（包括分片、向量化等阶段）
     *
     * @param kbId 知识库 ID（必填）
     * @param file 上传的文件（必填，支持 txt/pdf/md）
     * @return SSE 流式响应，实时推送处理进度
     */
    @PostMapping(value = "/doc/upload-stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter uploadDocumentWithProgress(
            @RequestParam("kbId") String kbId,
            @RequestParam("file") MultipartFile file) {

        // 1. 参数校验
        if (kbId == null || kbId.trim().isEmpty()) {
            SseEmitter errorEmitter = new SseEmitter();
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"msg\":\"知识库 ID 不能为空\"}")
                        .build());
                errorEmitter.complete();
            } catch (Exception e) {
                log.error("发送错误消息失败", e);
            }
            return errorEmitter;
        }

        if (file == null || file.isEmpty()) {
            SseEmitter errorEmitter = new SseEmitter();
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"msg\":\"上传文件不能为空\"}")
                        .build());
                errorEmitter.complete();
            } catch (Exception e) {
                log.error("发送错误消息失败", e);
            }
            return errorEmitter;
        }

        // 从 ThreadLocal 获取当前用户ID
        Long userId = UserContextHolder.getUserId();

        // 校验文件类型
        String fileName = file.getOriginalFilename();
        if (fileName != null) {
            String fileType = getFileType(fileName);
            if (!isValidFileType(fileType)) {
                SseEmitter errorEmitter = new SseEmitter();
                try {
                    errorEmitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"msg\":\"不支持的文件类型，仅支持 txt/pdf/md 格式\"}")
                            .build());
                    errorEmitter.complete();
                } catch (Exception e) {
                    log.error("发送错误消息失败", e);
                }
                return errorEmitter;
            }
        }

        try {
            log.info("文档上传请求（SSE 流式）：userId={}, kbId={}, fileName={}", userId, kbId, fileName);

            // 2. 构建请求对象
            RagDocUploadRequest request = new RagDocUploadRequest();
            request.setKbId(kbId);
            request.setFile(file);

            // 3. 调用服务层上传文档（SSE 流式）
            return ragDocumentService.uploadDocumentWithProgress(request);

        } catch (Exception e) {
            log.error("文档上传异常", e);
            SseEmitter errorEmitter = new SseEmitter();
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"msg\":\"文档上传失败：" + e.getMessage() + "\"}")
                        .build());
                errorEmitter.complete();
            } catch (Exception ex) {
                log.error("发送错误消息失败", ex);
            }
            return errorEmitter;
        }
    }

    /**
     * 查询知识库中文档列表接口
     * 接口地址：/api/rag/doc/list
     * 请求方式：GET
     * 接口描述：查询指定知识库中的所有文档列表，包含文档名称、类型、大小、状态等信息
     *
     * @param kbId 知识库 ID（必填）
     * @return 统一响应结果，包含文档列表
     */
    @GetMapping("/doc/list")
    public Result getDocumentList(@RequestParam("kbId") String kbId) {
        try {
            // 1. 参数校验
            if (kbId == null || kbId.trim().isEmpty()) {
                return Result.paramError("知识库 ID 不能为空");
            }

            // 2. 从 ThreadLocal 获取当前用户ID
            Long userId = UserContextHolder.getUserId();

            log.info("查询文档列表：userId={}, kbId={}", userId, kbId);

            // 3. 调用服务层查询文档列表
            RagDocListResponse response = ragDocumentService.getDocumentList(kbId, userId);

            // 4. 返回成功响应
            return Result.success(response);

        } catch (IllegalArgumentException e) {
            // 5. 处理业务异常
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            // 6. 处理其他异常
            return Result.serverError("查询文档列表失败：" + e.getMessage());
        }
    }

    /**
     * 更新知识库信息接口
     * 接口地址：/api/rag/kb/update/{kbId}
     * 请求方式：PUT
     * 接口描述：更新知识库的基础信息（名称、描述、状态）
     *
     * @param kbId 知识库 ID（路径参数，必填）
     * @param request 知识库更新请求参数
     * @return 统一响应结果，包含更新后的知识库详情
     */
    @PutMapping("/kb/update/{kbId}")
    public Result updateKnowledge(@PathVariable String kbId, @RequestBody RagKbUpdateRequest request) {
        try {
            // 1. 参数校验
            if (kbId == null || kbId.isBlank()) {
                return Result.paramError("知识库 ID 不能为空");
            }
            if (request == null) {
                return Result.paramError("请求参数不能为空");
            }

            // 2. 从 ThreadLocal 获取当前用户ID
            Long userId = UserContextHolder.getUserId();

            log.info("更新知识库请求：userId={}, kbId={}", userId, kbId);

            // 3. 调用服务层更新知识库
            RagKbDetailResponse response = ragKnowledgeService.updateKnowledge(kbId, userId, request);

            // 4. 返回成功响应
            return Result.success("知识库更新成功", response);

        } catch (IllegalArgumentException e) {
            // 5. 处理业务异常
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            // 6. 处理其他异常
            return Result.serverError("更新知识库失败：" + e.getMessage());
        }
    }

    /**
     * 删除知识库中文档接口
     * 接口地址：/api/rag/doc/delete/{docId}
     * 请求方式：DELETE
     * 接口描述：删除指定知识库中的文档，同时删除向量库中的向量数据和本地资源文件
     *
     * @param docId 文档 ID（路径参数，必填）
     * @param kbId 知识库 ID（查询参数，必填）
     * @return 统一响应结果
     */
    @DeleteMapping("/doc/delete/{docId}")
    public Result deleteDocument(
            @PathVariable String docId,
            @RequestParam("kbId") String kbId) {
        try {
            // 1. 参数校验
            if (docId == null || docId.isBlank()) {
                return Result.paramError("文档 ID 不能为空");
            }
            if (kbId == null || kbId.isBlank()) {
                return Result.paramError("知识库 ID 不能为空");
            }

            // 2. 从 ThreadLocal 获取当前用户 ID
            Long userId = UserContextHolder.getUserId();

            log.info("删除文档请求：userId={}, kbId={}, docId={}", userId, kbId, docId);

            // 3. 调用服务层删除文档
            ragDocumentService.deleteDocument(docId, kbId, userId);

            // 4. 返回成功响应
            return Result.success("文档删除成功");

        } catch (IllegalArgumentException e) {
            // 5. 处理业务异常
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            // 6. 处理其他异常
            return Result.serverError("删除文档失败：" + e.getMessage());
        }
    }

    /**
     * 删除知识库接口
     * 接口地址：/api/rag/kb/delete/{kbId}
     * 请求方式：DELETE
     * 接口描述：删除指定知识库，包括该知识库下的所有文档、向量数据和本地资源文件
     *
     * @param kbId 知识库 ID（路径参数，必填）
     * @return 统一响应结果
     */
    @DeleteMapping("/kb/delete/{kbId}")
    public Result deleteKnowledge(@PathVariable String kbId) {
        try {
            // 1. 参数校验
            if (kbId == null || kbId.isBlank()) {
                return Result.paramError("知识库 ID 不能为空");
            }

            // 2. 从 ThreadLocal 获取当前用户 ID
            Long userId = UserContextHolder.getUserId();

            log.info("删除知识库请求：userId={}, kbId={}", userId, kbId);

            // 3. 调用文档服务层删除知识库（包括所有文档、向量、文件）
            ragDocumentService.deleteKnowledgeWithDocuments(kbId, userId);

            // 4. 返回成功响应
            return Result.success("知识库删除成功");

        } catch (IllegalArgumentException e) {
            // 5. 处理业务异常
            return Result.paramError(e.getMessage());
        } catch (Exception e) {
            // 6. 处理其他异常
            return Result.serverError("删除知识库失败：" + e.getMessage());
        }
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
}
