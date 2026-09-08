package com.mdkj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 智能客服 Spring AI 配置
 */
@Data
@ConfigurationProperties(prefix = "customer-service.ai")
public class CustomerServiceAiProperties {

    /** 是否启用大模型回复（需配置 spring.ai.openai.api-key） */
    private boolean enabled = false;

    /** 大模型调用失败时是否回退到规则引擎 */
    private boolean fallbackToRules = true;

    /** 模型温度，越低越稳定 */
    private double temperature = 0.3;

    /** 最大输出 token */
    private int maxTokens = 512;
}
