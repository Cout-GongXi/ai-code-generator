package com.zzy.aicodegenerator.controller;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.zzy.aicodegenerator.annotation.AuthCheck;
import com.zzy.aicodegenerator.common.BaseResponse;
import com.zzy.aicodegenerator.common.DeleteRequest;
import com.zzy.aicodegenerator.common.ResultUtils;
import com.zzy.aicodegenerator.constant.AppConstant;
import com.zzy.aicodegenerator.constant.UserConstant;
import com.zzy.aicodegenerator.exception.ErrorCode;
import com.zzy.aicodegenerator.exception.ThrowUtils;
import com.zzy.aicodegenerator.model.dto.app.AppCreateRequest;
import com.zzy.aicodegenerator.model.dto.app.AppQueryRequest;
import com.zzy.aicodegenerator.model.dto.app.AppUpdateByAdminRequest;
import com.zzy.aicodegenerator.model.dto.app.AppUpdateRequest;
import com.zzy.aicodegenerator.model.entity.App;
import com.zzy.aicodegenerator.model.entity.User;
import com.zzy.aicodegenerator.model.vo.AppVO;
import com.zzy.aicodegenerator.service.AppService;
import com.zzy.aicodegenerator.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用 控制层。
 *
 * @author zzy
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    // ==================== 用户接口 ====================

    /**
     * 创建应用（须填写 initPrompt）。
     *
     * @param appCreateRequest 应用创建请求体
     * @param request          HTTP 请求
     * @return 新应用 id
     */
    @PostMapping("/create")
    public BaseResponse<Long> createApp(@RequestBody AppCreateRequest appCreateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appCreateRequest == null, ErrorCode.PARAMS_ERROR);
        String appName = appCreateRequest.getAppName();
        String initPrompt = appCreateRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "initPrompt 不能为空");
        User loginUser = userService.getLoginUser(request);
        App app = new App();
        BeanUtils.copyProperties(appCreateRequest, app);
        app.setUserId(loginUser.getId());
        if (StrUtil.isBlank(app.getCodeGenType())) {
            app.setCodeGenType(AppConstant.DEFAULT_CODE_GEN_TYPE);
        }
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "应用创建失败");
        return ResultUtils.success(app.getId());
    }

    /**
     * 根据 id 修改自己的应用（目前只支持修改应用名称）。
     *
     * @param appUpdateRequest 应用更新请求体
     * @param request          HTTP 请求
     * @return 更新结果
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appUpdateRequest == null || appUpdateRequest.getId() == null,
                ErrorCode.PARAMS_ERROR, "请求参数错误");
        User loginUser = userService.getLoginUser(request);
        App existApp = appService.getById(appUpdateRequest.getId());
        ThrowUtils.throwIf(existApp == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!existApp.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权修改该应用");
        App updateApp = new App();
        updateApp.setId(appUpdateRequest.getId());
        updateApp.setAppName(appUpdateRequest.getAppName());
        boolean result = appService.updateById(updateApp);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "应用更新失败");
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 删除自己的应用。
     *
     * @param deleteRequest 删除请求体
     * @param request       HTTP 请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "请求参数错误");
        User loginUser = userService.getLoginUser(request);
        App existApp = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(existApp == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!existApp.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权删除该应用");
        boolean result = appService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "应用删除失败");
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 查看应用详情（脱敏）。
     *
     * @param id      应用 id
     * @param request HTTP 请求
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "应用 id 错误");
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权查看该应用");
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询自己的应用列表（支持根据名称查询，每页最多 20 个）。
     *
     * @param appQueryRequest 应用查询请求体
     * @param request         HTTP 请求
     * @return 应用分页列表（脱敏）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<AppVO>> listUserAppsVOByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                          HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数错误");
        User loginUser = userService.getLoginUser(request);
        int pageNum = appQueryRequest.getPageNum();
        int pageSize = Math.min(appQueryRequest.getPageSize(), AppConstant.MAX_PAGE_SIZE);
        appQueryRequest.setUserId(loginUser.getId());
        appQueryRequest.setIsDelete(0);
        Page<App> appPage = appService.page(new Page<>(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest));
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页查询精选的应用列表（支持根据名称查询，每页最多 20 个）。
     * 精选应用：未被删除的应用，按优先级降序排列。
     *
     * @param appQueryRequest 应用查询请求体
     * @return 应用分页列表（脱敏）
     */
    @PostMapping("/list/featured/vo")
    public BaseResponse<Page<AppVO>> listFeaturedAppsVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数错误");
        int pageNum = appQueryRequest.getPageNum();
        int pageSize = Math.min(appQueryRequest.getPageSize(), AppConstant.MAX_PAGE_SIZE);
        appQueryRequest.setIsDelete(0);
        appQueryRequest.setSortField("priority");
        appQueryRequest.setSortOrder("descend");
        Page<App> appPage = appService.page(new Page<>(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest));
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    // ==================== 管理员接口 ====================

    /**
     * 根据 id 删除任意应用。管理员可用
     *
     * @param deleteRequest 删除请求体
     * @return 删除结果
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "请求参数错误");
        boolean result = appService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "应用删除失败");
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 更新任意应用（支持更新应用名称、应用封面、优先级）。管理员可用
     *
     * @param appUpdateByAdminRequest 管理员应用更新请求体
     * @return 更新结果
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppUpdateByAdminRequest appUpdateByAdminRequest) {
        ThrowUtils.throwIf(appUpdateByAdminRequest == null || appUpdateByAdminRequest.getId() == null,
                ErrorCode.PARAMS_ERROR, "请求参数错误");
        App app = new App();
        BeanUtils.copyProperties(appUpdateByAdminRequest, app);
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "应用更新失败");
        return ResultUtils.success(true);
    }

    /**
     * 分页查询应用列表（支持根据除时间外的任何字段查询，每页数量不限）。管理员可用
     *
     * @param appQueryRequest 应用查询请求体
     * @return 应用分页列表
     */
    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<App>> listAppsByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数错误");
        int pageNum = appQueryRequest.getPageNum();
        int pageSize = appQueryRequest.getPageSize();
        Page<App> appPage = appService.page(new Page<>(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest));
        return ResultUtils.success(appPage);
    }

    /**
     * 根据 id 查看应用详情。管理员可用
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<App> getAppById(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "应用 id 错误");
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        return ResultUtils.success(app);
    }
}
