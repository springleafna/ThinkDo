package com.springleaf.thinkdo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.springleaf.thinkdo.mapper")
public class ThinkDoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ThinkDoApplication.class, args);
    }
}
