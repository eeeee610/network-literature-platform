package com.novel.platform.modules.novel.config;

import com.novel.platform.modules.novel.auth.NovelAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 临时鉴权注册：仅覆盖 /api/novel 下需登录的接口，联调时替换为 A 模块鉴权配置。
 */
@Configuration
public class NovelWebMvcConfig implements WebMvcConfigurer {

    private final NovelAuthInterceptor novelAuthInterceptor;

    public NovelWebMvcConfig(NovelAuthInterceptor novelAuthInterceptor) {
        this.novelAuthInterceptor = novelAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(novelAuthInterceptor)
                .addPathPatterns("/api/novel", "/api/novel/**");
    }
}
