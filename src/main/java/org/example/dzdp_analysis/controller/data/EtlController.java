package org.example.dzdp_analysis.controller.data;

import org.example.dzdp_analysis.repository.entity.data.EtlTask;
import org.example.dzdp_analysis.service.data.DataProcessService;
import org.example.dzdp_analysis.repository.dao.data.EtlTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/etl")
public class EtlController {

    @Autowired
    private DataProcessService dataProcessService;

    @Autowired
    private EtlTaskRepository etlTaskRepository;

    @PostMapping("/run")
    public ResponseEntity<?> runETL(
            @RequestParam("hdfsPath") String hdfsPath,  // 添加参数名
            @RequestParam("operator") String operator)  {
        try {
            Map<String, Object> result = dataProcessService.executeEtlProcess(hdfsPath, operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "ETL任务执行失败",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/tasks")
    public ResponseEntity<?> getTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startTime,desc") String sort,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("startTime")));
            Page<EtlTask> tasks;

            // 添加搜索功能
            if (StringUtils.hasText(search)) {
                tasks = etlTaskRepository.findByTaskNameContainingOrOperatorContaining(
                        search, search, pageable);
            }
            // 状态筛选
            else if (StringUtils.hasText(status)) {
                tasks = etlTaskRepository.findByStatus(status, pageable);
            } else {
                tasks = etlTaskRepository.findAll(pageable);
            }

            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "获取ETL任务失败",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/stats/{taskId}")
    public ResponseEntity<?> getEtlStatus(@PathVariable Long taskId) {
        try {
            EtlTask task = dataProcessService.getEtlTask(taskId);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取ETL状态失败");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Map<String, Object> stats = dataProcessService.getEtlStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "获取统计信息失败",
                    "message", e.getMessage()
            ));
        }
    }

    // 添加测试端点
    @GetMapping("/test")
    public ResponseEntity<?> testEtl() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "ETL接口正常工作");
        response.put("timestamp", LocalDateTime.now());
        response.put("endpoints", new String[]{
                "POST /api/etl/run - 执行ETL流程",
                "GET /api/etl/stats/{taskId} - 获取ETL任务状态",
                "GET /api/etl/tasks - 获取所有ETL任务",
                "GET /api/etl/quality-report/{taskId} - 获取质量报告"
        });
        return ResponseEntity.ok(response);
    }
}