package com.zzy.aicodegenerator.service;

/**
 * 截图服务
 */
public interface ScreenshotService {

    /**
     * 生成网页截图并上传
     *
     * @param webUrl 网页链接
     * @return 截图文件路径
     */
    String generateAndUploadScreenshot(String webUrl);
}
