package com.zzy.aicodegenerator.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.zzy.aicodegenerator.model.dto.app.AppAddRequest;
import com.zzy.aicodegenerator.model.dto.app.AppQueryRequest;
import com.zzy.aicodegenerator.model.entity.App;
import com.zzy.aicodegenerator.model.entity.User;
import com.zzy.aicodegenerator.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author zzy
 */
public interface AppService extends IService<App> {

    /**
     * 获取应用信息（脱敏）。
     *
     * @param app 应用
     * @return 脱敏后的应用信息
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用信息列表（脱敏）。
     *
     * @param appList 应用列表
     * @return 脱敏后的应用信息列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 根据查询条件构造查询包装器。
     *
     * @param appQueryRequest 应用查询请求体
     * @return 查询包装器
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 通过对话生成应用代码
     *
     * @param appId     应用ID
     * @param message   对话消息
     * @param loginUser 登录用户
     * @return 查询条件构造器
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
     *
     * @param appId     应用ID
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 创建应用
     *
     * @param addRequest 创建应用请求体
     * @param loginUser  登录用户
     * @return 新应用ID
     */
    Long createApp(AppAddRequest addRequest, User loginUser);
}
