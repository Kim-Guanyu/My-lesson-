package com.mdkj.config;
import feign.Logger;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenFeignConfig {

	/**
	 * OpenFeign提供了日志打印功能，用于对OpenFeign接口的调用情况进行监控和输出
	 *
	 * <p>Logger.Level.NONE: 不显示任何日志，默认值
	 * <p>Logger.Level.BASIC: 仅记录请求方法、URL、响应状态码及执行时间
	 * <p>Logger.Level.HEADERS: 除了BASIC中定义的信息之外，还有请求和响应的头信息
	 * <p>Logger.Level.FULL: 除了HEADERS中定义的信息之外，还有请求和响应的正文及元数据
	 */
	@Bean
	Logger.Level feignLoggerLevel() {
		return Logger.Level.FULL;
	}
}
