package org.example.dzdp_analysis.controller.data;  // 移动到data包

import org.example.dzdp_analysis.repository.dao.data.DataQualityReportRepository;
import org.example.dzdp_analysis.repository.entity.data.DataQualityReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/quality-reports")
public class QualityReportController {

    @Autowired
    private DataQualityReportRepository qualityReportRepository;

    // 新增：获取所有质量报告（分页）
    @GetMapping("/list")
    public ResponseEntity<Page<DataQualityReport>> getAllQualityReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createTime") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        try {
            Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            Page<DataQualityReport> reports = qualityReportRepository.findAll(pageable);

            // 添加调试日志
            System.out.println("查询到质量报告数量: " + reports.getTotalElements());
            if (reports.hasContent()) {
                System.out.println("第一条记录ID: " + reports.getContent().get(0).getId());
                System.out.println("任务名称: " + reports.getContent().get(0).getTaskName());
            }

            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            System.err.println("获取质量报告失败: " + e.getMessage());
            e.printStackTrace();
            // 返回空分页而不是错误
            Page<DataQualityReport> emptyPage = Page.empty();
            return ResponseEntity.ok(emptyPage);
        }
    }

    // 新增：根据任务ID获取质量报告详情
    @GetMapping("/task/{taskId}/detail")
    public ResponseEntity<DataQualityReport> getQualityReportDetail(@PathVariable Long taskId) {
        Optional<DataQualityReport> report = qualityReportRepository.findByTaskId(taskId);
        return report.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 新增：获取最近的质量报告
    @GetMapping("/recent")
    public ResponseEntity<List<DataQualityReport>> getRecentQualityReports(
            @RequestParam(defaultValue = "5") int limit) {

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<DataQualityReport> reports = qualityReportRepository.findAll(pageable);
        return ResponseEntity.ok(reports.getContent());
    }
    @GetMapping
    public ResponseEntity<?> getQualityReports(
            @RequestParam(required = false) String uploadDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createTime").descending());
        Page<DataQualityReport> reports;

        if (uploadDate != null && !uploadDate.isEmpty()) {
            reports = qualityReportRepository.findByUploadDate(uploadDate, pageable);
        } else {
            reports = qualityReportRepository.findAll(pageable);
        }

        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getQualityReport(@PathVariable Long id) {
        Optional<DataQualityReport> report = qualityReportRepository.findById(id);
        if (report.isPresent()) {
            return ResponseEntity.ok(report.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getQualityReportByTaskId(@PathVariable Long taskId) {
        Optional<DataQualityReport> report = qualityReportRepository.findByTaskId(taskId);
        if (report.isPresent()) {
            return ResponseEntity.ok(report.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestQualityReport() {
        Pageable pageable = PageRequest.of(0, 1, Sort.by("createTime").descending());
        Page<DataQualityReport> reports = qualityReportRepository.findAll(pageable);
        if (reports.hasContent()) {
            return ResponseEntity.ok(reports.getContent().get(0));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 添加统计接口
    @GetMapping("/stats/summary")
    public ResponseEntity<?> getQualityStats() {
        Long highQualityCount = qualityReportRepository.countHighQualityReports();
        Long lowQualityCount = qualityReportRepository.countLowQualityReports();
        Optional<java.time.LocalDateTime> latestTime = qualityReportRepository.findLatestReportTime();

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("highQualityReports", highQualityCount);
        stats.put("lowQualityReports", lowQualityCount);
        stats.put("latestReportTime", latestTime.orElse(null));
        stats.put("totalReports", qualityReportRepository.count());

        return ResponseEntity.ok(stats);
    }
}