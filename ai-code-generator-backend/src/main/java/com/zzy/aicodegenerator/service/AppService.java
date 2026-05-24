package com.zzy.aicodegenerator.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.zzy.aicodegenerator.model.dto.app.AppQueryRequest;
import com.zzy.aicodegenerator.model.entity.App;
import com.zzy.aicodegenerator.model.vo.AppVO;

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
}
