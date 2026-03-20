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
@TableName("t_chat_session")
// @ApiModel(value="TChatSession对象", description="")
public class TChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    // @ApiModelProperty(value = "会话唯一 ID")
    @TableId(value = "session_id", type = IdType.AUTO)
    private String sessionId;

    // @ApiModelProperty(value = "关联用户 ID")
    @TableField("user_id")
    private Long userId;

    // @ApiModelProperty(value = "会话名称，默认新建对话")
    @TableField("session_name")
    private String sessionName;

    // @ApiModelProperty(value = "关联自定义 AgentID，普通会话为空")
    @TableField("agent_id")
    private String agentId;

    // @ApiModelProperty(value = "关联知识库 ID，RAG 会话使用")
    @TableField("kb_id")
    private String kbId;

    @TableField("last_content")
    private String lastContent;

    // @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    // @ApiModelProperty(value = "最后更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;


}
