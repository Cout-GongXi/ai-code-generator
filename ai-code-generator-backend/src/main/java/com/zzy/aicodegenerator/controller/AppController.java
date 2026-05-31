package com.zzy.aicodegenerator.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zzy.aicodegenerator.annotation.AuthCheck;
import com.zzy.aicodegenerator.common.BaseResponse;
import com.zzy.aicodegenerator.common.DeleteRequest;
import com.zzy.aicodegenerator.common.ResultUtils;
import com.zzy.aicodegenerator.constant.AppConstant;
import com.zzy.aicodegenerator.constant.UserConstant;
import com.zzy.aicodegenerator.exception.BusinessException;
import com.zzy.aicodegenerator.exception.ErrorCode;
import com.zzy.aicodegenerator.exception.ThrowUtils;
import com.zzy.aicodegenerator.model.dto.app.*;
import com.zzy.aicodegenerator.model.entity.App;
import com.zzy.aicodegenerator.model.entity.User;
import com.zzy.aicodegenerator.model.enums.CodeGenTypeEnum;
import com.zzy.aicodegenerator.model.vo.AppVO;
import com.zzy.aicodegenerator.service.AppService;
import com.zzy.aicodegenerator.service.ProjectDownloadService;
import com.zzy.aicodegenerator.service.UserService;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @Resource
    private ProjectDownloadService projectDownloadService;

    /**
     * 聊天生成代码。
     *
     * @param appId   应用 id
     * @param message 对话内容
     * @param request HTTP 请求
     * @return 生成的代码
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId, @RequestParam String message, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "对话内容不能为空");
        User loginUser = userService.getLoginUser(request);
        // 调用服务生成代码 流式返回
        Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser);
        return contentFlux.map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        //发送一个结束事件
                        ServerSentEvent.<String>builder().event("done").data("").build()
                ));
    }

    /**
     * 部署应用。
     *
     * @param appDeployRequest 应用部署请求体
     * @param request          HTTP 请求
     * @return 部署结果
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取应用id
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId 错误");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        // 返回URL
        return ResultUtils.success(deployUrl);

    }

    /**
     * 下载应用代码。
     *
     * @param appId       应用 id
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId, HttpServletRequest request, HttpServletResponse response) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId 错误");

        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 权限校验
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }
        // 构建应用代码目录
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 检查代码目录是否存在
        if (!new File(sourceDirPath).exists()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用代码不存在");
        }
        // 生成下载文件名（不建议添加中文内容）
        String downloadFileName = String.valueOf(appId);

        // 调用服务生成下载文件
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }


// ==================== 用户接口 ====================

    /**
     * 创建应用（须填写 initPrompt）。
     *
     * @param addAddRequest 应用创建请求体
     * @param request       HTTP 请求
     * @return 新应用 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest addAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(addAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 参数校验
        String initPrompt = addAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "initPrompt 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 构造入库对象
        App app = new App();
        BeanUtils.copyProperties(addAddRequest, app);
        // 设置用户id
        app.setUserId(loginUser.getId());
        // 应用名称默认设置为initPrompt的前12位
        app.setAppName(StrUtil.sub(initPrompt, 0, 12));
        // 暂时设置为Vue生成
        app.setCodeGenType(CodeGenTypeEnum.VUE_PROJECT.getValue());
        // 插入数据库
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
        ThrowUtils.throwIf(appUpdateRequest == null || appUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);

        App oldApp = appService.getById(appUpdateRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 校验权限
        ThrowUtils.throwIf(!oldApp.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权修改该应用");
        // 构造更新对象
        App app = new App();
        app.setId(appUpdateRequest.getId());
        app.setAppName(appUpdateRequest.getAppName());
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
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
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "应用 id 错误");
        // 获取应用
        App app = appService.getById(id);
        // 应用不存在
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 获取应用详情
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询自己的应用列表（支持根据名称查询，每页最多 20 个）。
     *
     * @param appQueryRequest 应用查询请求体
     * @param request         HTTP 请求
     * @return 应用分页列表（脱敏）
     */
    @PostMapping("/list/user/page/vo")
    public BaseResponse<Page<AppVO>> listUserAppsVOByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                          HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null,
                ErrorCode.PARAMS_ERROR, "请求参数错误");
        User loginUser = userService.getLoginUser(request);
        int pageSize = appQueryRequest.getPageSize();
        long pageNum = appQueryRequest.getPageNum();
        // 限制每页最多 20 条数据
        ThrowUtils.throwIf(pageSize > AppConstant.MAX_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        ThrowUtils.throwIf(pageNum <= 0, ErrorCode.PARAMS_ERROR, "页码错误");
        // 只查询当前用户的应用
        appQueryRequest.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
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
    @PostMapping("/list/featured/page/vo")
    public BaseResponse<Page<AppVO>> listFeaturedAppsVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数错误");
        int pageSize = appQueryRequest.getPageSize();
        long pageNum = appQueryRequest.getPageNum();
        // 限制每页最多 20 条数据
        ThrowUtils.throwIf(pageSize > AppConstant.MAX_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        ThrowUtils.throwIf(pageNum <= 0, ErrorCode.PARAMS_ERROR, "页码错误");
        appQueryRequest.setPriority(AppConstant.Featured_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
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
        Long id = deleteRequest.getId();
        App existApp = appService.getById(id);
        ThrowUtils.throwIf(existApp == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean result = appService.removeById(id);
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
        // 判断应用是否存在
        App existApp = appService.getById(appUpdateByAdminRequest.getId());
        ThrowUtils.throwIf(existApp == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        App app = new App();
        BeanUtils.copyProperties(appUpdateByAdminRequest, app);
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
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
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数错误");
        int pageNum = appQueryRequest.getPageNum();
        ThrowUtils.throwIf(pageNum <= 0, ErrorCode.PARAMS_ERROR, "页码错误");
        int pageSize = appQueryRequest.getPageSize();

        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 根据 id 查看应用详情。管理员可用
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(Long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "应用 id 错误");
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        return ResultUtils.success(appService.getAppVO(app));
    }
}
