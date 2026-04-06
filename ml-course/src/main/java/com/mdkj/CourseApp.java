package com.mdkj;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;


@MapperScan("com.mdkj.mapper")
@SpringBootApplication
@EnableDiscoveryClient
public class CourseApp {
    public static void main(String[] args) {  
        SpringApplication.run(CourseApp.class, args);
        System.out.println("CourseApp启动成功");
    }  
}
