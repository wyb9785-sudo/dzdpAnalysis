package org.example.dzdp_analysis.service.data;

import org.example.dzdp_analysis.repository.dao.data.DataQualityReportRepository;
import org.slf4j.Logger;
import java.util.Date;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

@Service
public class DataQualityService {

    private static final Logger logger = LoggerFactory.getLogger(DataQualityService.class);
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DataQualityReportRepository reportRepository;

    @Transactional
    public void saveQualityReport(Long taskId, String taskName, String uploadDate,
                                  Map<String, Object> qualityReport) {
        String sql = "INSERT INTO data_quality_report (" +
                "task_id, task_name, upload_date, total_records, valid_records, " +
                "null_comment_count, price_error_count, core_field_missing_count, " +
                "rating_anomaly_count, quality_score, core_field_score, content_score, " +
                "price_score, rating_score, merchant_count, avg_rating, positive_rate, create_time" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            // 使用安全的类型转换方法
            jdbcTemplate.update(sql,
                    taskId,                    // task_id
                    taskName,                  // task_name
                    parseUploadDate(uploadDate), // upload_date
                    getIntValue(qualityReport, "totalRecords", 0),
                    getIntValue(qualityReport, "validRecords", 0),
                    getIntValue(qualityReport, "nullCommentCount", 0),
                    getIntValue(qualityReport, "priceErrorCount", 0),
                    getIntValue(qualityReport, "coreFieldMissingCount", 0),
                    getIntValue(qualityReport, "ratingAnomalyCount", 0),
                    getDoubleValue(qualityReport, "qualityScore", 0.0),
                    getDoubleValue(qualityReport, "coreFieldScore", 0.0),
                    getDoubleValue(qualityReport, "contentScore", 0.0),
                    getDoubleValue(qualityReport, "priceScore", 0.0),
                    getDoubleValue(qualityReport, "ratingScore", 0.0),
                    getIntValue(qualityReport, "merchantCount", 0),
                    getDoubleValue(qualityReport, "avgRating", 0.0),
                    getDoubleValue(qualityReport, "positiveRate", 0.0),
                    new Date()  // create_time
            );

            logger.info("数据质量报告已保存到MySQL，任务ID: {}", taskId);
        } catch (Exception e) {
            logger.error("保存数据质量报告到MySQL失败: {}", e.getMessage(), e);
            logger.error("质量报告内容: {}", qualityReport);
            throw new RuntimeException("保存数据质量报告失败: " + e.getMessage(), e);
        }
    }

    // 辅助方法：安全获取整数值
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            logger.warn("数据质量报告字段 {} 为null，使用默认值{}", key, defaultValue);
            return defaultValue;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            } else {
                return defaultValue;
            }
        } catch (NumberFormatException e) {
            logger.warn("数据质量报告字段 {} 的值 {} 无法转换为整数，使用默认值{}", key, value, defaultValue);
            return defaultValue;
        }
    }

    // 辅助方法：安全获取双精度值
    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            logger.warn("数据质量报告字段 {} 为null，使用默认值{}", key, defaultValue);
            return defaultValue;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            } else {
                return defaultValue;
            }
        } catch (NumberFormatException e) {
            logger.warn("数据质量报告字段 {} 的值 {} 无法转换为浮点数，使用默认值{}", key, value, defaultValue);
            return defaultValue;
        }
    }

    // 辅助方法：解析上传日期
    private java.sql.Date parseUploadDate(String uploadDate) {
        try {
            // 假设uploadDate格式为yyyyMMdd
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            java.util.Date date = sdf.parse(uploadDate);
            return new java.sql.Date(date.getTime());
        } catch (ParseException e) {
            logger.warn("上传日期格式错误: {}，使用当前日期", uploadDate);
            return new java.sql.Date(System.currentTimeMillis());
        }
    }
}