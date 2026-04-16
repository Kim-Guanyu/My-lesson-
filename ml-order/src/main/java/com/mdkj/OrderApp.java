package com.mdkj;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

@EnableScheduling
@MapperScan("com.mdkj.mapper")
@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients(basePackages = "com.mdkj.feign")
public class OrderApp {  
    public static void main(String[] args) {  
        // 启动前兜底，防止配置中心加载异常时 rocketmq.name-server 为空导致 connect to null failed
        String nameServer = System.getProperty("rocketmq.name-server");
        if (!StringUtils.hasText(nameServer)) {
            System.setProperty("rocketmq.name-server", "192.168.211.132:9876");
        }
        String nameServerCamel = System.getProperty("rocketmq.nameServer");
        if (!StringUtils.hasText(nameServerCamel)) {
            System.setProperty("rocketmq.nameServer", System.getProperty("rocketmq.name-server"));
        }
        SpringApplication.run(OrderApp.class, args);
        System.out.println("OrderApp启动成功");
    }  
}
