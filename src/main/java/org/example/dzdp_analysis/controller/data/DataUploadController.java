package org.example.dzdp_analysis.controller.data;

import org.example.dzdp_analysis.repository.entity.data.UploadLog;
import org.example.dzdp_analysis.service.data.DataProcessService;
import org.example.dzdp_analysis.service.data.HdfsService;
import org.example.dzdp_analysis.util.FileValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataUploadController {

    @Autowired
    private HdfsService hdfsService;

    @Autowired
    private DataProcessService dataProcessService;


    // 添加测试端点
    @GetMapping("/test_upload")
    public ResponseEntity<?> testUpload() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "数据上传测试接口正常工作");
            response.put("timestamp", LocalDateTime.now());
            response.put("endpoints", new String[]{
                    "POST /api/data/upload - 上传文件到HDFS",
                    "GET /api/data/upload-logs - 获取上传日志",
                    "DELETE /api/data/upload-log/{id} - 删除上传记录",
                    "DELETE /api/data/upload-logs/batch - 批量删除"
            });
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "测试接口异常",
                    "message", e.getMessage()
            ));
        }
    }
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "operator", defaultValue = "system") String operator) {

        try {
            // 1. 文件校验
            FileValidator.validateUploadFile(file);

            // 2. 生成HDFS路径
            String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String safeFilename = file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
            String hdfsFileName = timestamp + "_" + safeFilename;

            // 3. 上传到HDFS
            String hdfsPath = hdfsService.uploadFile(file, hdfsFileName);

            // 4. 记录上传日志
            Map<String, Object> uploadResult = dataProcessService.recordUploadLog(
                    file.getOriginalFilename(),
                    file.getSize(),
                    hdfsPath,
                    operator
            );

            return ResponseEntity.ok(uploadResult);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "文件上传失败",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/upload-logs")
    public ResponseEntity<Page<UploadLog>> getUploadLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "uploadTime,desc") String sort) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("uploadTime")));
            Page<UploadLog> logs = dataProcessService.getUploadLogs(pageable);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 添加HDFS连接测试端点
    @GetMapping("/hdfs-test")
    public ResponseEntity<?> testHdfsConnection() {
        try {
            boolean connected = hdfsService.testConnection();
            Map<String, Object> response = new HashMap<>();
            response.put("status", connected ? "SUCCESS" : "FAILED");
            response.put("service", "HDFS Connection");
            response.put("connected", connected);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "HDFS连接测试失败",
                    "message", e.getMessage()
            ));
        }
    }
    @DeleteMapping("/upload-log/{logId}")
    public ResponseEntity<?> deleteUploadLog(@PathVariable Long logId) {
        try {
            UploadLog log = dataProcessService.getUploadLog(logId);

            if (log.getHdfsPath() != null && !log.getHdfsPath().isEmpty()) {
                hdfsService.deleteFile(log.getHdfsPath());
            }

            dataProcessService.deleteUploadLog(logId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "删除成功",
                    "deletedFile", log.getFileName()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "删除失败",
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/upload-logs/batch")
    public ResponseEntity<?> batchDeleteUploadLogs(@RequestBody List<Long> logIds) {
        try {

            dataProcessService.batchDeleteUploadLogs(logIds);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "批量删除成功",
                    "deletedCount", logIds.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "批量删除失败",
                    "message", e.getMessage()
            ));
        }
    }
}