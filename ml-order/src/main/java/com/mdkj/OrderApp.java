package com.mdkj;

import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.mdkj.mapper")
@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients(basePackages = "com.mdkj.feign")
public class OrderApp {  
    public static void main(String[] args) {  
        SpringApplication.run(OrderApp.class, args);
        System.out.println("OrderApp启动成功");
    }  
}
