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
@TableName("t_rag_document")
// @ApiModel(value="TRagDocument对象", description="")
public class TRagDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    // @ApiModelProperty(value = "文档唯一 ID，UUID 生成")
    @TableId(value = "doc_id", type = IdType.AUTO)
    private String docId;

    // @ApiModelProperty(value = "关联知识库 ID")
    @TableField("kb_id")
    private String kbId;

    // @ApiModelProperty(value = "原始文档名称")
    @TableField("doc_name")
    private String docName;

    // @ApiModelProperty(value = "文档类型")
    @TableField("doc_type")
    private String docType;

    // @ApiModelProperty(value = "文档大小")
    @TableField("doc_size")
    private Long docSize;

    // @ApiModelProperty(value = "文档处理状态")
    @TableField("doc_status")
    private Integer docStatus;

    // @ApiModelProperty(value = "分块总数")
    @TableField("chunk_count")
    private Integer chunkCount;

    // @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    // @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;


}
