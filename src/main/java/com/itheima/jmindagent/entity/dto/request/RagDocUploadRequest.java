package com.itheima.jmindagent.entity.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * RAG 知识库文档上传请求 DTO
 */
@Data
public class RagDocUploadRequest {

    /**
     * 关联知识库 ID（必填）
     */
    private String kbId;

    /**
     * 上传的文件（必填，支持 txt/pdf/word/md）
     */
    private MultipartFile file;
}
