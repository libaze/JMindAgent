package com.itheima.jmindagent.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 *
 * </p>
 *
 * @author
 * @since 2026-03-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_rag_chunk_vector")
public class TRagChunkVector implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "chunk_id", type = IdType.AUTO)
    private String chunkId;

    @TableField("kb_id")
    private String kbId;

    @TableField("doc_id")
    private String docId;

    @TableField("chunk_content")
    private String chunkContent;

    @TableField("chunk_index")
    private Integer chunkIndex;

    @TableField("embedding_vector")
    private String embeddingVector;

    @TableField("vector_status")
    private Integer vectorStatus;

    @TableField("create_time")
    private LocalDateTime createTime;
}