package com.zzy.aicodegenerator.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ProjectDownloadService {
    /**
     * 下载项目为 ZIP 文件
     *
     * @param projectName       项目名称
     * @param downloadFileName  下载文件名
     * @param response          HttpServletResponse 对象
     */
    void downloadProjectAsZip(String projectName, String downloadFileName, HttpServletResponse response);
}
