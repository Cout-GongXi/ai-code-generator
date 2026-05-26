package com.zzy.aicodegenerator.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新请求管理员更新，可以更新用户角色
 */
@Data
public class UserUpdateByAdminRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 确认密码
     */
    private String confirmPassword;

    /**
     * 用户角色: user-普通用户 admin-管理员
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}