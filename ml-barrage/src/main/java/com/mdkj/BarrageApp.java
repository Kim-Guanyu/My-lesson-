package com.mdkj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class BarrageApp {
    public static void main(String[] args) {  
        SpringApplication.run(BarrageApp.class, args);
        System.out.println("BarrageApp启动成功");
    }  
}
