package com.mdkj;

import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.mdkj.mapper")
@SpringBootApplication
@EnableDiscoveryClient
@ImportAutoConfiguration({RocketMQAutoConfiguration.class})
public class SaleApp {  
    public static void main(String[] args) {  
        SpringApplication.run(SaleApp.class, args);
        System.out.println("SaleApp启动成功");
    }  
}
