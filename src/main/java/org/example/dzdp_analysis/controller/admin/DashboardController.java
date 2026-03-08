package org.example.dzdp_analysis.controller.admin;

import org.example.dzdp_analysis.repository.entity.data.UploadLog;
import org.example.dzdp_analysis.service.data.DataProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DataProcessService dataProcessService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = dataProcessService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取统计信息失败: " + e.getMessage()));
        }
    }


    @GetMapping("/recent-uploads")
    public ResponseEntity<List<UploadLog>> getRecentUploads() {
        try {
            List<UploadLog> uploads = dataProcessService.getRecentUploads();
            return ResponseEntity.ok(uploads);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}