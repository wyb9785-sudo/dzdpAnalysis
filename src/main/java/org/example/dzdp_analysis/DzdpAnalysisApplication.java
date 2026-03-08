package org.example.dzdp_analysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EntityScan("org.example.dzdp_analysis.repository.entity")
@EnableJpaRepositories("org.example.dzdp_analysis.repository.dao")
@SpringBootApplication
@EnableAsync // 确保有这个注解
@EnableScheduling
public class DzdpAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(DzdpAnalysisApplication.class, args);
    }

}
