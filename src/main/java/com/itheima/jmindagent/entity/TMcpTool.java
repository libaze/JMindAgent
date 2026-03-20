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
@TableName("t_mcp_tool")
// @ApiModel(value="TMcpTool对象", description="")
public class TMcpTool implements Serializable {

    private static final long serialVersionUID = 1L;

    // @ApiModelProperty(value = "工具唯一 ID")
    @TableId(value = "tool_id", type = IdType.AUTO)
    private String toolId;

    // @ApiModelProperty(value = "工具名称")
    @TableField("tool_name")
    private String toolName;

    // @ApiModelProperty(value = "工具功能描述")
    @TableField("tool_desc")
    private String toolDesc;

    // @ApiModelProperty(value = "MCP 协议调用地址")
    @TableField("mcp_url")
    private String mcpUrl;

    // @ApiModelProperty(value = "工具鉴权信息")
    @TableField("auth_info")
    private String authInfo;

    // @ApiModelProperty(value = "工具状态：1-启用，0-禁用")
    @TableField("tool_status")
    private Integer toolStatus;

    // @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;


}
