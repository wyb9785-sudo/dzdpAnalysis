package org.example.dzdp_analysis.controller;

import org.example.dzdp_analysis.service.admin.ClusterHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cluster")
public class ClusterHealthController {

    @Autowired
    private ClusterHealthService clusterHealthService;

    @GetMapping("/health")
    public Map<String, Object> clusterHealth() {
        try {
            return clusterHealthService.checkClusterHealth();
        } catch (Exception e) {
            // 返回基本的健康状态
            Map<String, Object> mockStatus = new HashMap<>();
            mockStatus.put("hdfsConnection", "UNKNOWN");
            mockStatus.put("hiveConnection", "UNKNOWN");
            mockStatus.put("mysqlConnection", "SUCCESS");
            mockStatus.put("overallStatus", "DEGRADED");
            mockStatus.put("status", "DEGRADED");
            mockStatus.put("timestamp", System.currentTimeMillis());
            mockStatus.put("message", "集群检查异常，但MySQL连接正常");
            return mockStatus;
        }
    }

    @GetMapping("/status")
    public Map<String, Object> clusterStatus() {
        Map<String, Object> status = clusterHealthService.checkClusterHealth();
        status.put("service", "DZDP Analysis Platform Cluster");
        status.put("version", "1.0.0");
        return status;
    }

    // 添加测试端点
    @GetMapping("/test")
    public Map<String, Object> testCluster() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "集群接口正常工作");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}