package com.zzy.aicodegenerator.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.zzy.aicodegenerator.exception.ErrorCode;
import com.zzy.aicodegenerator.exception.ThrowUtils;
import com.zzy.aicodegenerator.service.ScreenshotService;
import com.zzy.aicodegenerator.utils.MinioUtils;
import com.zzy.aicodegenerator.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class ScreenshotServiceImpl implements ScreenshotService {
    @Resource
    private MinioUtils minioUtils;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        // 参数校验
        ThrowUtils.throwIf(webUrl == null, ErrorCode.PARAMS_ERROR, "网址不能为空");
        // 本地截图
        log.info("开始生成网页截图 URL:{}", webUrl);
        String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        ThrowUtils.throwIf(StrUtil.isBlank(localScreenshotPath) , ErrorCode.OPERATION_ERROR, "截图失败");
        // 上传图片到MinIO
        try {
            String cosUrl = uploadScreenshot(localScreenshotPath);
            ThrowUtils.throwIf(StrUtil.isBlank(cosUrl), ErrorCode.OPERATION_ERROR, "上传图片失败");
            log.info("上传图片成功 URL:{}", cosUrl);
            return cosUrl;
            // 清理本地文件
        } finally {
            cleanupLocalFile(localScreenshotPath);
        }
    }

    /**
     * 上传截图文件到 MinIO
     *
     * @param screenshotPath 本地截图文件路径（由 WebScreenshotUtils 生成，文件名已包含随机数）
     * @return MinIO 对象存储路径
     */
    private String uploadScreenshot(String screenshotPath) {
        ThrowUtils.throwIf(screenshotPath == null, ErrorCode.PARAMS_ERROR, "截图文件路径不能为空");
        File file = new File(screenshotPath);
        ThrowUtils.throwIf(!file.exists(), ErrorCode.PARAMS_ERROR, "截图文件不存在");
        // 生成文件保存路径
        String objectName = generateObjectName(file.getName());
        try (InputStream inputStream = new FileInputStream(file)) {
            String contentType = Files.probeContentType(file.toPath());
            return minioUtils.upload(objectName, inputStream, file.length(), contentType);
        } catch (Exception e) {
            log.error("上传图片失败:{}", screenshotPath, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成 MinIO 对象存储路径
     * 格式：screenshots/yyyy/MM/dd/文件名
     * 文件名由 WebScreenshotUtils 截图时已通过随机数保证唯一性
     *
     * @param fileName 截图文件名（如 12345_compressed.jpg）
     * @return 对象存储路径（如 screenshots/2026/05/29/12345678_compressed.jpg）
     */
    private String generateObjectName(String fileName) {
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return "screenshots/" + datePath + "/" + fileName;
    }

    /**
     * 清理本地文件
     *
     * @param localFilePath 本地文件路径
     */
    private void cleanupLocalFile(String localFilePath) {
        File localFile = new File(localFilePath);
        if (localFile.exists()){
            FileUtil.del(localFile);
            log.info("清理本地文件成功:{}", localFilePath);
        }
    }
}
