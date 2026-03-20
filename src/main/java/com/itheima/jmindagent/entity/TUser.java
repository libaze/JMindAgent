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
@TableName("t_user")
// @ApiModel(value="TUser对象", description="")
public class TUser implements Serializable {

    private static final long serialVersionUID = 1L;

    // @ApiModelProperty(value = "用户唯一 ID，自增")
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    // @ApiModelProperty(value = "用户名")
    @TableField("username")
    private String username;

    // @ApiModelProperty(value = "加密后密码")
    @TableField("password")
    private String password;

    // @ApiModelProperty(value = "手机号")
    @TableField("phone")
    private String phone;

    // @ApiModelProperty(value = "邮箱")
    @TableField("email")
    private String email;

    // @ApiModelProperty(value = "头像")
    @TableField("avatar")
    private String avatar;

    // @ApiModelProperty(value = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    // @ApiModelProperty(value = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;


}
