package com.zzy.aicodegenerator.constant;

/**
 * 应用常量
 *
 * @author zzy
 */
public interface AppConstant {
    /**
     * 精选应用的优先级
     */
    Integer Featured_APP_PRIORITY = 100;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 用户查询每页最大数量
     */
    int MAX_PAGE_SIZE = 20;

    /**
     * 代码生成结果保存根目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 代码部署根目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 应用部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost:8010";

}
