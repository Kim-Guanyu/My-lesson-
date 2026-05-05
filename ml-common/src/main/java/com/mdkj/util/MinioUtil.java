package com.mdkj.util;

import cn.hutool.core.util.StrUtil;
import com.mdkj.exception.ServiceException;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
// 移除 @RequiredArgsConstructor，改用 @Resource 注入 MinioClient（解决 Spring 3.x 兼容问题）
public class MinioUtil {

    // 替换 final + 构造器注入 为 @Resource 注入（核心修复点）
    @Resource
    private MinioClient minioClient;

    private static MinioUtil INSTANCE;

    // 匹配配置文件的 minio.bucket-name（短横线格式）
    @Value("${minio.bucket-name}")
    private String defaultBucketName;

    private static final int DEFAULT_EXPIRE_SECONDS = 3600; // 1小时

    @PostConstruct
    public void init() {
        INSTANCE = this;
    }

    public static String randomFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (StrUtil.isNotBlank(originalFilename) && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return UUID.randomUUID().toString().replace("-", "") + suffix;
    }

    public static void upload(MultipartFile newFile, String fileName, String dirName, String bucketName) {
        try (InputStream inputStream = newFile.getInputStream()) {
            MinioUtil util = requireInstance();
            util.ensureBucket(bucketName);
            util.minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(buildObjectName(dirName, fileName))
                            .stream(inputStream, newFile.getSize(), -1)
                            .contentType(newFile.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new ServiceException(ResultCode.MINIO_ERROR, "MinIO上传失败: " + e.getMessage());
        }
    }

    public static void delete(String fileName, String dirName, String bucketName) {
        try {
            MinioUtil util = requireInstance();
            util.minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(buildObjectName(dirName, fileName))
                            .build()
            );
        } catch (Exception e) {
            throw new ServiceException(ResultCode.MINIO_ERROR, "MinIO删除失败: " + e.getMessage());
        }
    }

    private static MinioUtil requireInstance() {
        if (INSTANCE == null) {
            throw new ServiceException(ResultCode.SERVER_ERROR, "MinioUtil未完成初始化");
        }
        return INSTANCE;
    }

    private static String buildObjectName(String dirName, String fileName) {
        if (StrUtil.isBlank(dirName)) {
            return fileName;
        }
        return dirName + "/" + fileName;
    }

    private void ensureBucket(String bucketName) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * 上传本地图片到 MinIO（使用默认桶名）
     */
    public boolean uploadImage(String localFilePath, String objectName) {
        return uploadImage(localFilePath, defaultBucketName, objectName);
    }

    /**
     * 上传本地图片到 MinIO（指定桶名）
     * @param localFilePath 本地文件绝对路径
     * @param bucketName    桶名
     * @param objectName    桶内文件名（支持多级目录，如 images/avatar.png）
     */
    public boolean uploadImage(String localFilePath, String bucketName, String objectName) {
        File localFile = new File(localFilePath);
        if (!localFile.exists() || !localFile.isFile()) {
            throw new ServiceException(ResultCode.MINIO_ERROR, "本地文件不存在或不是文件: " + localFilePath);
        }
        try {
            ensureBucket(bucketName);
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .filename(localFilePath)
                            .contentType(getContentType(localFile.getName()))
                            .build()
            );
            return true;
        } catch (Exception e) {
            throw new ServiceException(ResultCode.MINIO_ERROR, "MinIO上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取图片临时预览 URL（使用默认桶名）
     */
    public String getImagePreviewUrl(String objectName) {
        return getImagePreviewUrl(defaultBucketName, objectName, DEFAULT_EXPIRE_SECONDS);
    }

    public byte[] getObjectBytes(String objectName) {
        return getObjectBytes(defaultBucketName, objectName);
    }

    public byte[] getObjectBytes(String bucketName, String objectName) {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        )) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            throw new ServiceException(ResultCode.MINIO_ERROR, "读取 MinIO 对象失败: " + e.getMessage());
        }
    }

    public String getObjectContentType(String objectName) {
        return getObjectContentType(defaultBucketName, objectName);
    }

    public String getObjectContentType(String bucketName, String objectName) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            ).contentType();
        } catch (Exception e) {
            throw new ServiceException(ResultCode.MINIO_ERROR, "获取 MinIO 对象类型失败: " + e.getMessage());
        }
    }

    /**
     * 获取图片临时预览 URL（指定桶名和过期时间）
     * @param bucketName  桶名
     * @param objectName  桶内文件名
     * @param expireSeconds 过期时间（秒）
     */
    public String getImagePreviewUrl(String bucketName, String objectName, int expireSeconds) {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                throw new ServiceException(ResultCode.MINIO_ERROR, "桶不存在: " + bucketName);
            }
            minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(expireSeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(ResultCode.MINIO_ERROR, "获取预览地址失败: " + e.getMessage());
        }
    }

    /**
     * 从 MinIO 下载图片到本地（使用默认桶名）
     */
    public boolean downloadImage(String objectName, String localSavePath) {
        return downloadImage(defaultBucketName, objectName, localSavePath);
    }

    /**
     * 从 MinIO 下载图片到本地（指定桶名）
     * @param bucketName    桶名
     * @param objectName    桶内文件名
     * @param localSavePath 本地保存路径
     */
    public boolean downloadImage(String bucketName, String objectName, String localSavePath) {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                throw new ServiceException(ResultCode.MINIO_ERROR, "桶不存在: " + bucketName);
            }
            minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());

            minioClient.downloadObject(
                    DownloadObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .filename(localSavePath)
                            .build()
            );
            return true;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(ResultCode.MINIO_ERROR, "MinIO下载失败: " + e.getMessage());
        }
    }

    /**
     * 根据文件名后缀获取 Content-Type
     */
    private String getContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
