package com.mdkj.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.minio.MinioClient;

@Configuration
public class MinioConfig {

    // 全部用短横线格式，和配置文件完全匹配
    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}") // 匹配配置文件的 access-key
    private String accessKey;

    @Value("${minio.secret-key}") // 匹配配置文件的 secret-key
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}