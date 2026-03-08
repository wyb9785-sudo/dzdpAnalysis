package org.example.dzdp_analysis.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "DZDP Analysis Platform");
        response.put("timestamp", System.currentTimeMillis());
        response.put("version", "1.0.0");
        return response;
    }

    @GetMapping("/test")
    public Map<String, String> testEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "后端API工作正常！");
        response.put("endpoint", "/api/test");
        return response;
    }

    // 添加根路径映射
    @GetMapping("")
    public Map<String, Object> apiRoot() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "大众点评数据分析平台API");
        response.put("endpoints", new String[]{
                "GET /api/health - 健康检查",
                "GET /api/test - 测试接口",
                "POST /api/data/upload - 数据上传",
                "POST /api/etl/run - 执行ETL",
                "GET /api/etl/tasks - 获取ETL任务列表",
                "GET /api/cluster/health - 集群健康检查",
                "GET /api/data/upload-logs - 获取上传日志"
        });
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}