package com.zzy.aicodegenerator.utils;

import com.zzy.aicodegenerator.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
/**
 * MinIO 文件上传工具类
 */
@Component
public class MinioUtils {
    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioConfig minioConfig;
    /**
     * 创建存储桶
     *
     * @param bucketName 存储桶名称
     */
    public void makeBucket(String bucketName) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * 通用文件上传
     *
     * @param objectName  对象存储路径（由调用方决定命名规则）
     * @param inputStream 文件输入流
     * @param size        文件大小
     * @param contentType 文件类型
     */
    public String upload(String objectName, InputStream inputStream, long size, String contentType) throws Exception {
        String bucketName = minioConfig.getBucketName();
        makeBucket(bucketName);

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, size, -1)
                        .contentType(contentType)
                        .build()
        );

        return objectName;
    }

    /**
     * 根据对象路径获取完整访问 URL
     * <p>
     * 该方法将 MinIO 服务端点、桶名称和对象路径拼接成完整的访问地址。
     * 格式：endpoint/bucketName/objectName
     * 示例：http://localhost:9000/my-bucket/screenshots/2026/05/29/143025_abc123.jpg
     * </p>
     *
     * @param objectName 对象存储路径（已上传文件的对象名）
     * @return 完整的文件访问 URL
     */
    public String getFileUrl(String objectName) {
        return minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/" + objectName;
    }
}
