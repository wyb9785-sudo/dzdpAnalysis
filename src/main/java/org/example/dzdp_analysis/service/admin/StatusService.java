package org.example.dzdp_analysis.service.admin;

import org.example.dzdp_analysis.repository.dao.data.UploadLogRepository;
import org.example.dzdp_analysis.repository.dao.data.EtlTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StatusService {

    @Autowired
    private UploadLogRepository uploadLogRepository;

    @Autowired
    private EtlTaskRepository etlTaskRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 上传统计
        long totalUploads = uploadLogRepository.count();
        stats.put("totalUploads", totalUploads);

        // ETL任务统计
        long totalTasks = etlTaskRepository.count();
        long successTasks = etlTaskRepository.countByStatus("SUCCESS");
        long failedTasks = etlTaskRepository.countByStatus("FAILED");

        stats.put("totalTasks", totalTasks);
        stats.put("successTasks", successTasks);
        stats.put("failedTasks", failedTasks);

        // 数据质量评分（模拟）
        stats.put("avgQuality", 85.5);

        // 总记录数（模拟）
        stats.put("totalRecords", totalUploads * 1000);

        return stats;
    }
}
