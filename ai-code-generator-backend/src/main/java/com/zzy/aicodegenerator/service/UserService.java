package com.zzy.aicodegenerator.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.zzy.aicodegenerator.model.dto.user.UserQueryRequest;
import com.zzy.aicodegenerator.model.entity.User;
import com.zzy.aicodegenerator.model.vo.LoginUserVO;
import com.zzy.aicodegenerator.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户表 服务层。
 *
 * @author zzy
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册。
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);


    /**
     * 用户登录。
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      HTTP 请求
     * @return 登录用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取登录用户信息(脱敏)
     *
     * @param user 用户
     * @return 登录用户信息
     */
    LoginUserVO getLoginUserVo(User user);

    /**
     * 获取登录用户信息。
     *
     * @param request HTTP 请求
     * @return 登录用户信息
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取用户信息(脱敏)。
     *
     * @param user 用户
     * @return 登录用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取用户信息列表(脱敏)。
     *
     * @param userList 用户列表
     * @return 登录用户信息列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销。
     *
     * @param request HTTP 请求
     * @return 注销结果
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 根据查询条件构造查询包装器。
     *
     * @param userQueryRequest 用户查询请求体
     * @return 查询包装器
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 获取加密后的密码。
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);
}
