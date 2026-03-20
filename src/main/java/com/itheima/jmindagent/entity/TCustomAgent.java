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
@TableName("t_custom_agent")
// @ApiModel(value="TCustomAgent对象", description="")
public class TCustomAgent implements Serializable {

    private static final long serialVersionUID = 1L;

    // @ApiModelProperty(value = "自定义 Agent 唯一 ID")
    @TableId(value = "agent_id", type = IdType.AUTO)
    private String agentId;

    // @ApiModelProperty(value = "所属用户 ID")
    @TableField("user_id")
    private Long userId;

    // @ApiModelProperty(value = "Agent 名称")
    @TableField("agent_name")
    private String agentName;

    // @ApiModelProperty(value = "Agent 描述")
    @TableField("agent_desc")
    private String agentDesc;

    // @ApiModelProperty(value = "Agent 专属提示词")
    @TableField("agent_prompt")
    private String agentPrompt;

    // @ApiModelProperty(value = "绑定工具 ID，逗号分隔")
    @TableField("tool_ids")
    private String toolIds;

    // @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;


}
