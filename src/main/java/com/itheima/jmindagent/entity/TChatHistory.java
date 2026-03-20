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
@TableName("t_chat_history")
// @ApiModel(value="TChatHistory对象", description="")
public class TChatHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    // @ApiModelProperty(value = "消息唯一 ID，自增")
    @TableId(value = "msg_id", type = IdType.AUTO)
    private Long msgId;

    // @ApiModelProperty(value = "关联会话 ID")
    @TableField("session_id")
    private String sessionId;

    // @ApiModelProperty(value = "关联用户 ID")
    @TableField("user_id")
    private Long userId;

    // @ApiModelProperty(value = "发送方类型：1-用户，2-AI")
    @TableField("sender_type")
    private Integer senderType;

    // @ApiModelProperty(value = "消息内容")
    @TableField("content")
    private String content;

    // @ApiModelProperty(value = "消息类型：1-普通，2-RAG，3-工具调用")
    @TableField("msg_type")
    private Integer msgType;

    // @ApiModelProperty(value = "发送时间")
    @TableField("create_time")
    private LocalDateTime createTime;


}
