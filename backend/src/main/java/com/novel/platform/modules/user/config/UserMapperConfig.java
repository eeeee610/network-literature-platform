package com.novel.platform.modules.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.novel.platform.modules.user.mapper")
public class UserMapperConfig {
}
