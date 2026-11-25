package com.novel.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.novel.platform.modules.user.mapper")
public class NovelPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovelPlatformApplication.class, args);
    }
}
