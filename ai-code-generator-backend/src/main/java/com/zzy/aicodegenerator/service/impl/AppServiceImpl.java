package com.zzy.aicodegenerator.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zzy.aicodegenerator.ai.AiCodeGenTypeRoutingService;
import com.zzy.aicodegenerator.ai.AiCodeGenTypeRoutingServiceFactory;
import com.zzy.aicodegenerator.constant.AppConstant;
import com.zzy.aicodegenerator.core.AICodeGeneratorFacade;
import com.zzy.aicodegenerator.core.builder.VueProjectBuilder;
import com.zzy.aicodegenerator.core.handler.StreamHandlerExecutor;
import com.zzy.aicodegenerator.exception.BusinessException;
import com.zzy.aicodegenerator.exception.ErrorCode;
import com.zzy.aicodegenerator.exception.ThrowUtils;
import com.zzy.aicodegenerator.mapper.AppMapper;
import com.zzy.aicodegenerator.model.dto.app.AppAddRequest;
import com.zzy.aicodegenerator.model.dto.app.AppQueryRequest;
import com.zzy.aicodegenerator.model.entity.App;
import com.zzy.aicodegenerator.model.entity.User;
import com.zzy.aicodegenerator.model.enums.ChatHistoryMessageTypeEnum;
import com.zzy.aicodegenerator.model.enums.CodeGenTypeEnum;
import com.zzy.aicodegenerator.model.vo.AppVO;
import com.zzy.aicodegenerator.model.vo.UserVO;
import com.zzy.aicodegenerator.service.AppService;
import com.zzy.aicodegenerator.service.ChatHistoryService;
import com.zzy.aicodegenerator.service.ScreenshotService;
import com.zzy.aicodegenerator.service.UserService;
import com.zzy.aicodegenerator.utils.MinioUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author zzy
 */
@Slf4j
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AICodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private MinioUtils minioUtils;

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id错误");
        ThrowUtils.throwIf(message == null || message.isEmpty(), ErrorCode.PARAMS_ERROR, "消息不能为空");
        // 2. 查询应用应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }
        // 4. 获取生成模式
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum enumGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (enumGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成模式错误");
        }
        // 5. 保存用户消息到数据库中
        chatHistoryService.addChatMessage(appId, loginUser.getId(), message, ChatHistoryMessageTypeEnum.USER.getValue());

        // 6. 调用 AI 进行生成
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, enumGenTypeEnum, appId);

        // 7. 收集 AI 生成的消息，保存到数据库中
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, enumGenTypeEnum);
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_IN, "未登录");
        // 2. 获取应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验，仅允许应用创建者或管理员可以部署应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 如果没有， 则生成 6 位 deployKey（字母+数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，获取代码生成路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查路径是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码生成目录不存在");
        }
        // 7. Vue 项目特殊处理，执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "构建 Vue 项目失败,请重试");
            // 减产 dist 目录是否存在
            File distDir = new File(sourceDirPath + File.separator + "dist");
            ThrowUtils.throwIf(!distDir.exists() || !distDir.isDirectory(), ErrorCode.SYSTEM_ERROR, "项目构建完成但 dist 目录未生成,请重试");
            // 构建完成后，需要将构建后的文件复制到部署目录
            sourceDir = distDir;
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "复制文件失败");
        }
        // 9. 更新数据库
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 得到可以访问的URL
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 11. 异步生成截图并更新应用封面到数据库中
        generateScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "initPrompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtils.copyProperties(appAddRequest, app);
        // 设置用户id
        app.setUserId(loginUser.getId());
        // 应用名称默认设置为initPrompt的前12位
        app.setAppName(StrUtil.sub(initPrompt, 0, 20));
        // 使用 AI 智能选择代码生成类型(多例模式)
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "应用创建失败");
        log.info("应用创建成功，id: {}, 类型：{}", app.getId(), selectedCodeGenType);
        return app.getId();
    }

    /**
     * 异步生成应用截图并更新数据库中
     *
     * @param appId        应用id
     * @param appDeployUrl 应用部署的URL
     */
    private void generateScreenshotAsync(Long appId, String appDeployUrl) {
        Thread.startVirtualThread(() -> {
            // 生成截图
            String screenshotURL = screenshotService.generateAndUploadScreenshot(appDeployUrl);
            // 更新应用封面
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotURL);
            boolean result = this.updateById(updateApp);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新应用封面失败");
        });
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtils.copyProperties(app, appVO);
        // 将相对路径拼接为完整的 MinIO 访问 URL
        if (StrUtil.isNotBlank(app.getCover())) {
            appVO.setCover(minioUtils.getFileUrl(app.getCover()));
        }
        // 获取用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            appVO.setUser(userService.getUserVO(user));
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return List.of();
        }
        // 批量获取用户信息，避免 N + 1 问题
        Set<Long> userIds = appList.stream().map(App::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));

    }

    /**
     * 删除应用时，关联删除对话历史
     *
     * @param id 数据主键
     * @return 删除结果
     */
    @Override
    public boolean removeById(Serializable id) {
        long appId = Long.parseLong(id.toString());

        if (appId <= 0) {
            return false;
        }
        // 删除应用历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            log.error("删除应用历史失败，appId: {}, error: {}", appId, e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }

}
