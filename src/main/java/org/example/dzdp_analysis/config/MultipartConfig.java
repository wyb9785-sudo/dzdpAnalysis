package org.example.dzdp_analysis.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;

/**
 * @author 芍药
 */
@Configuration
public class MultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // 设置单个文件最大大小
        factory.setMaxFileSize(DataSize.ofGigabytes(10));
        // 设置总上传数据最大大小
        factory.setMaxRequestSize(DataSize.ofGigabytes(10));
        return factory.createMultipartConfig();
    }
}