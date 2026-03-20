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
@TableName("t_rag_knowledge")
// @ApiModel(value="TRagKnowledge对象", description="")
public class TRagKnowledge implements Serializable {

    private static final long serialVersionUID = 1L;

    // @ApiModelProperty(value = "知识库 ID")
    @TableId(value = "kb_id", type = IdType.AUTO)
    private String kbId;

    // @ApiModelProperty(value = "所属用户 ID")
    @TableField("user_id")
    private Long userId;

    // @ApiModelProperty(value = "知识库名称")
    @TableField("kb_name")
    private String kbName;

    // @ApiModelProperty(value = "知识库描述")
    @TableField("kb_desc")
    private String kbDesc;

    // @ApiModelProperty(value = "文档数量")
    @TableField("doc_count")
    private Integer docCount;

    // @ApiModelProperty(value = "知识库状态：1-启用，0-禁用")
    @TableField("kb_status")
    private Integer kbStatus;

    // @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    // @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;


}
