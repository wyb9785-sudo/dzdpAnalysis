package org.example.dzdp_analysis.config;

import org.example.dzdp_analysis.util.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:8081")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // 预检请求缓存时间
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/health/**",
                        "/api/test/**",
                        "/api/cluster/**",
                        "/api/status/**",
                        "/api/data/upload-logs",  // 添加数据上传相关API
                        "/api/data/upload",
                        "/api/data/hdfs-test",
                        "/api/etl/**",           // 放行所有ETL API
                        "/api/admin/users/**" ,   // 放行用户管理API
                        "/api/merchant-analysis/**" , // 添加商户分析API放行
                        "/api/decision/**"  // 添加决策辅助API
                );
    }
    // 配置 OPTIONS 请求处理
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:8081")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600); // 预检请求缓存时间
            }
        };
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 前端静态资源（Vue打包后放在resources/static下）
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);
    }

    // 添加路径匹配配置
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        //configurer.setUseTrailingSlashMatch(false);
    }
}