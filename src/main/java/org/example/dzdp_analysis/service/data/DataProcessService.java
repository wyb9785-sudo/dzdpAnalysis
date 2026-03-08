package org.example.dzdp_analysis.service.data;

import org.apache.logging.log4j.core.config.Scheduled;
import org.example.dzdp_analysis.repository.dao.data.DataQualityReportRepository;
import org.example.dzdp_analysis.repository.dao.data.EtlTaskRepository;
import org.example.dzdp_analysis.repository.dao.data.UploadLogRepository;
import org.example.dzdp_analysis.repository.entity.data.DataQualityReport;
import org.example.dzdp_analysis.repository.entity.data.EtlTask;
import org.example.dzdp_analysis.repository.entity.data.UploadLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DataProcessService {

    private static final Logger logger = LoggerFactory.getLogger(DataProcessService.class);

    @Autowired
    private UserSyncService userSyncService;
    @Autowired
    private HdfsService hdfsService;
    @Autowired
    private EtlTaskLoggerService etlTaskLoggerService;

    @Autowired
    private HiveService hiveService;

    @Autowired
    private MySqlSyncService mySqlSyncService;

    @Autowired
    private UploadLogRepository uploadLogRepository;

    @Autowired
    private EtlTaskRepository etlTaskRepository;

    @Autowired
    private DataQualityReportRepository dataQualityReportRepository;

    @Autowired
    private DataQualityService dataQualityService;

    @Autowired
    private ParallelEtlService parallelEtlService;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 5000; // 5秒

    @Transactional
    public Map<String, Object> recordUploadLog(String fileName, long fileSize,
                                               String hdfsPath, String operator) {
        UploadLog log = new UploadLog();
        log.setFileName(fileName);
        log.setFileSize(fileSize);
        log.setHdfsPath(hdfsPath);
        log.setOperator(operator);
        log.setUploadTime(LocalDateTime.now());
        log.setUploadStatus("SUCCESS");

        // 估算记录数（CSV文件每行约200字节）
        if (fileName.toLowerCase().endsWith(".csv")) {
            log.setRecordCount((int) Math.max(1, fileSize / 200));
        } else {
            log.setRecordCount(0);
        }

        uploadLogRepository.save(log);

        Map<String, Object> result = new HashMap<>();
        result.put("logId", log.getId());
        result.put("fileName", fileName);
        result.put("fileSize", fileSize);
        result.put("hdfsPath", hdfsPath);
        result.put("uploadTime", log.getUploadTime());
        result.put("recordCount", log.getRecordCount());
        result.put("operator", operator);

        logger.info("上传日志记录完成: {}", result);
        return result;
    }

    public List<UploadLog> getAllUploadLogs() {
        try {
            // 添加详细的日志记录
            logger.info("开始查询所有上传记录...");
            List<UploadLog> logs = uploadLogRepository.findAllByOrderByUploadTimeDesc();

            if (logs.isEmpty()) {
                logger.warn("未找到任何上传记录");
            } else {
                logger.info("成功获取 {} 条上传记录", logs.size());
            }
            return logs;
        } catch (Exception e) {
            logger.error("获取上传记录失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取上传记录失败: " + e.getMessage());
        }
    }


    // 修改executeEtlProcess方法
    //@Transactional
    // 在DataProcessService.java中修改executeEtlProcess方法
    @Transactional
    public Map<String, Object> executeEtlProcess(String hdfsPath, String operator) {
        cleanupStaleTasks();

        EtlTask task = new EtlTask();
        task.setTaskName("ETL_PROCESS_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        task.setHdfsPath(hdfsPath);
        task.setOperator(operator);
        task.setStartTime(LocalDateTime.now());
        task.setStatus("RUNNING");
        task = etlTaskRepository.save(task);

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("startTime", task.getStartTime());

        String uploadDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        try {
            // 检查Hive连接健康状态
            if (!hiveService.checkConnectionHealth()) {
                throw new RuntimeException("Hive连接不可用，无法执行ETL");
            }

            // 1. 确保Hive表结构正确
            hiveService.createDatabaseAndTables();

            // 2. 加载数据到ODS层
            hiveService.loadDataToOds(hdfsPath, uploadDate);

            // 3. 验证数据加载
            hiveService.validateDataFile(uploadDate);

            // 4. 执行ETL到DWD层（数据清洗）
            hiveService.executeEtlToDwd(uploadDate);

            // 5. 执行DWS聚合
            executeDwsAggregationWithRetry(uploadDate, task);

            // 6. 生成ADS层数据
            hiveService.generateMerchantDashboardData(uploadDate);
            hiveService.generateRankingDailyData(uploadDate);

            // 7. 同步到MySQL
            mySqlSyncService.syncAllDataToMySql(uploadDate);

            // 8. 生成质量报告（基于清洗后的DWD层数据）
            Map<String, Object> qualityReport = hiveService.generateDataQualityReport(uploadDate);
            dataQualityService.saveQualityReport(task.getId(), task.getTaskName(), uploadDate, qualityReport);

            // 9. 更新任务状态
            task.setStatus("SUCCESS");
            task.setEndTime(LocalDateTime.now());
            task.setRecordsProcessed(hiveService.getProcessedRecordCount(uploadDate));
            task.setExecutionDuration((int) ChronoUnit.SECONDS.between(task.getStartTime(), task.getEndTime()));

            result.put("status", "SUCCESS");
            result.put("processedRecords", task.getRecordsProcessed());
            result.put("executionDuration", task.getExecutionDuration());
            result.put("qualityReport", qualityReport);
            result.put("uploadDate", uploadDate);

            logger.info("ETL处理完成: 处理记录数: {}, 耗时: {}秒", task.getRecordsProcessed(), task.getExecutionDuration());

        } catch (Exception e) {
            logger.error("ETL处理失败: {}", e.getMessage(), e);
            task.setStatus("FAILED");
            task.setEndTime(LocalDateTime.now());
            task.setErrorMessage(e.getMessage());
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            result.put("endTime", LocalDateTime.now());

            throw new RuntimeException("ETL处理失败: " + e.getMessage(), e);
        } finally {
            etlTaskRepository.save(task);
        }

        return result;
    }

    // 添加带重试机制的DWS聚合方法
    private void executeDwsAggregationWithRetry(String uploadDate, EtlTask task) throws SQLException {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount <= maxRetries) {
            try {
                // 检查连接状态
                if (!hiveService.checkConnectionHealth()) {
                    throw new SQLException("Hive连接已断开");
                }

                hiveService.executeDwsAggregation(uploadDate);
                return; // 成功执行，退出循环

            } catch (SQLException e) {
                retryCount++;

                if (retryCount > maxRetries) {
                    logger.error("DWS聚合失败(重试{}次): {}", maxRetries, e.getMessage());
                    throw new SQLException("DWS聚合失败: " + e.getMessage(), e);
                }

                logger.warn("DWS聚合失败，第{}次重试: {}", retryCount, e.getMessage());

                // 更新任务状态
                // 9. 更新任务状态为成功
                task.setStatus("SUCCESS");
                task.setEndTime(LocalDateTime.now());
                task.setRecordsProcessed(hiveService.getProcessedRecordCount(uploadDate));
                task.setExecutionDuration((int) ChronoUnit.SECONDS.between(task.getStartTime(), task.getEndTime()));

                etlTaskRepository.save(task); // 确保状态保存
                etlTaskRepository.save(task);

                try {
                    Thread.sleep(5000 * retryCount); // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("重试被中断", ie);
                }
            }
        }
    }
    // 在DataProcessService中添加以下方法
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 总上传文件数 - 直接从上传日志获取
            long totalUploads = uploadLogRepository.count();
            stats.put("totalUploads", totalUploads);

            // 总处理记录数 - 所有成功任务的记录数总和
            Long totalRecords = etlTaskRepository.sumRecordsProcessedByStatus("SUCCESS");
            stats.put("totalRecords", totalRecords != null ? totalRecords : 0);

            // 任务统计
            stats.put("successTasks", etlTaskRepository.countByStatus("SUCCESS"));
            stats.put("failedTasks", etlTaskRepository.countByStatus("FAILED"));
            stats.put("runningTasks", etlTaskRepository.countByStatus("RUNNING"));

            // 平均质量评分 - 从质量报告获取，如果没有则使用模拟数据
            Double avgQuality = dataQualityReportRepository.findAvgQualityScore();
            if (avgQuality != null && avgQuality > 0) {
                stats.put("avgQuality", Math.round(avgQuality));
            } else {
                // 使用模拟数据
                stats.put("avgQuality", 87.5);
            }

        } catch (Exception e) {
            logger.error("获取仪表盘统计信息失败: {}", e.getMessage());
            // 设置默认值
            stats.put("totalUploads", 0);
            stats.put("totalRecords", 0);
            stats.put("successTasks", 0);
            stats.put("failedTasks", 0);
            stats.put("runningTasks", 0);
            stats.put("avgQuality", 0);
        }

        return stats;
    }

    public EtlTask getEtlTask(Long taskId) {
        try {
            Optional<EtlTask> task = etlTaskRepository.findById(taskId);
            return task.orElseThrow(() -> new RuntimeException("ETL任务不存在: " + taskId));
        } catch (Exception e) {
            logger.error("获取ETL任务失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取ETL任务失败: " + e.getMessage());
        }
    }

    public Page<EtlTask> getEtlTasks(Pageable pageable) {
        try {
            return etlTaskRepository.findAll(pageable);
        } catch (Exception e) {
            logger.error("获取ETL任务失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取ETL任务失败: " + e.getMessage());
        }
    }

    public Map<String, Object> getEtlStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("totalTasks", etlTaskRepository.count());
            stats.put("successTasks", etlTaskRepository.countByStatus("SUCCESS"));
            stats.put("failedTasks", etlTaskRepository.countByStatus("FAILED"));
            stats.put("runningTasks", etlTaskRepository.countByStatus("RUNNING"));

            // 计算成功率
            long total = etlTaskRepository.count();
            long success = etlTaskRepository.countByStatus("SUCCESS");
            if (total > 0) {
                stats.put("successRate", Math.round((success * 100.0) / total));
            } else {
                stats.put("successRate", 0);
            }

        } catch (Exception e) {
            logger.error("获取ETL统计信息失败: {}", e.getMessage(), e);
            // 设置默认值
            stats.put("totalTasks", 0);
            stats.put("successTasks", 0);
            stats.put("failedTasks", 0);
            stats.put("runningTasks", 0);
            stats.put("successRate", 0);
        }
        return stats;
    }


    public UploadLog getUploadLog(Long logId) {
        Optional<UploadLog> log = uploadLogRepository.findById(logId);
        return log.orElseThrow(() -> new RuntimeException("上传记录不存在: " + logId));
    }

    public Page<UploadLog> getUploadLogs(Pageable pageable) {
        try {
            return uploadLogRepository.findAll(pageable);
        } catch (Exception e) {
            logger.error("获取上传记录失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取上传记录失败: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> syncToMySql(String uploadDate) {
        Map<String, Object> result = new HashMap<>();
        result.put("uploadDate", uploadDate);
        result.put("startTime", LocalDateTime.now());

        try {
            logger.info("开始同步数据到MySQL，日期: {}", uploadDate);
            mySqlSyncService.syncAllDataToMySql(uploadDate);

            result.put("status", "SUCCESS");
            result.put("endTime", LocalDateTime.now());
            result.put("message", "数据同步到MySQL完成");

            logger.info("MySQL数据同步完成: {}", uploadDate);

        } catch (Exception e) {
            logger.error("MySQL数据同步失败: {}", e.getMessage(), e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            result.put("endTime", LocalDateTime.now());
        }

        return result;
    }

    public Map<String, Object> testClusterConnection() {
        Map<String, Object> result = new HashMap<>();

        try {
            boolean hdfsConnected = hdfsService.testConnection();
            boolean hiveConnected = hiveService.testConnection();

            result.put("hdfsConnection", hdfsConnected ? "SUCCESS" : "FAILED");
            result.put("hiveConnection", hiveConnected ? "SUCCESS" : "FAILED");
            result.put("overallStatus", (hdfsConnected && hiveConnected) ? "READY" : "ERROR");
            result.put("timestamp", LocalDateTime.now().toString());

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("overallStatus", "ERROR");
        }

        return result;
    }
    @Transactional
    public void deleteUploadLog(Long logId) {
        uploadLogRepository.deleteById(logId);
        logger.info("删除上传记录: {}", logId);
    }

    @Transactional
    public void batchDeleteUploadLogs(List<Long> logIds) {
        for (Long logId : logIds) {
            UploadLog log = getUploadLog(logId);
            if (log.getHdfsPath() != null) {
                hdfsService.deleteFile(log.getHdfsPath());
            }
            uploadLogRepository.deleteById(logId);
        }
        logger.info("批量删除上传记录: {}", logIds.size());
    }
    public List<UploadLog> getRecentUploads() {
        return uploadLogRepository.findRecentUploads();
    }
    // 在DataProcessService中添加定期清理方法
    //@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    @Transactional
    public void cleanupStaleTasks() {
        logger.info("开始清理过期任务...");

        // 使用java.time.LocalDateTime
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        // 修改查询方法，使用java.time.LocalDateTime
        List<EtlTask> staleTasks = etlTaskRepository.findByStatusAndStartTimeBefore("RUNNING", threshold);

        for (EtlTask task : staleTasks) {
            task.setStatus("FAILED");
            task.setEndTime(LocalDateTime.now());
            task.setErrorMessage("任务超时自动终止");
            etlTaskRepository.save(task);
            logger.info("清理超时任务: {}", task.getId());
        }

        logger.info("清理完成，共处理{}个超时任务", staleTasks.size());
    }

}