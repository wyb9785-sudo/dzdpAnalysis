package org.example.dzdp_analysis.service.data;

import org.example.dzdp_analysis.repository.dao.analysis.FoodKeywordsRepository;
import org.example.dzdp_analysis.repository.dao.analysis.MerchantAnalysisRepository;
import org.example.dzdp_analysis.repository.dao.analysis.MerchantDashboardRepository;
import org.example.dzdp_analysis.repository.dao.decision.RankingDailyRepository;
import org.example.dzdp_analysis.repository.dao.decision.TimeAnalysisRepository;
import org.example.dzdp_analysis.repository.entity.analysis.FoodKeywords;
import org.example.dzdp_analysis.repository.entity.analysis.MerchantAnalysis;
import org.example.dzdp_analysis.repository.entity.analysis.MerchantDashboard;
import org.example.dzdp_analysis.repository.entity.decision.RankingDaily;
import org.example.dzdp_analysis.repository.entity.decision.TimeAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class MySqlSyncService {

    private static final Logger logger = LoggerFactory.getLogger(MySqlSyncService.class);

    @Autowired
    private Connection hiveConnection;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MerchantAnalysisRepository merchantAnalysisRepository;

    @Autowired
    private TimeAnalysisRepository timeAnalysisRepository;

    @Autowired
    private FoodKeywordsRepository foodKeywordsRepository;

    @Autowired
    private MerchantDashboardRepository merchantDashboardRepository;

    @Autowired
    private RankingDailyRepository rankingDailyRepository;

    @Value("${hive.database:dianping}")
    private String hiveDatabase;
    // 商户名称验证正则表达式
    private static final Pattern VALID_MERCHANT_PATTERN =
            Pattern.compile("^[\\u4e00-\\u9fa5a-zA-Z0-9\\-\\s（）()&]+$");

    // 在MySqlSyncService中添加连接测试方法
    public boolean testMySqlConnection() {
        try {
            String sql = "SELECT 1";
            jdbcTemplate.queryForObject(sql, Integer.class);
            logger.info("MySQL连接测试成功");
            return true;
        } catch (Exception e) {
            logger.error("MySQL连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    // 在MySqlSyncService中添加表创建方法
    private void ensureTempTablesExist() {
        createMerchantAnalysisTempTable();
        createFoodKeywordsTempTable();
        logger.info("MySQL临时表检查完成");
    }

    private void createMerchantAnalysisTempTable() {
        String sql = "CREATE TABLE IF NOT EXISTS merchant_analysis_temp (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "  merchant_name VARCHAR(255) NOT NULL, " +
                "  avg_rating DOUBLE, " +
                "  avg_taste DOUBLE, " +
                "  avg_environment DOUBLE, " +
                "  avg_service DOUBLE, " +
                "  avg_price DOUBLE, " +
                "  total_reviews INT, " +
                "  positive_reviews INT, " +
                "  negative_reviews INT, " +
                "  neutral_reviews INT, " +
                "  positive_rate DOUBLE, " +
                "  update_date VARCHAR(8), " +
                "  stat_date VARCHAR(8), " +
                "  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "  INDEX idx_merchant (merchant_name), " +
                "  INDEX idx_update_date (update_date) " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try {
            jdbcTemplate.execute(sql);
            logger.info("merchant_analysis_temp表创建/检查完成");
        } catch (Exception e) {
            logger.error("创建merchant_analysis_temp表失败: {}", e.getMessage());
            throw new RuntimeException("创建临时表失败: " + e.getMessage(), e);
        }
    }

    private void createFoodKeywordsTempTable() {
        String sql = "CREATE TABLE IF NOT EXISTS food_keywords_temp (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "  merchant_name VARCHAR(255) NOT NULL, " +
                "  keyword VARCHAR(100) NOT NULL, " +
                "  mention_count INT, " +
                "  update_date VARCHAR(8), " +
                "  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "  INDEX idx_merchant (merchant_name), " +
                "  INDEX idx_keyword (keyword), " +
                "  INDEX idx_update_date (update_date) " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try {
            jdbcTemplate.execute(sql);
            logger.info("food_keywords_temp表创建/检查完成");
        } catch (Exception e) {
            logger.error("创建food_keywords_temp表失败: {}", e.getMessage());
            throw new RuntimeException("创建临时表失败: " + e.getMessage(), e);
        }
    }
    // 添加数据清理方法（在同步前先删除）
    // 修改cleanupExistingData方法，确保清理所有相关表
    private void cleanupExistingData(String uploadDate) {
        logger.info("彻底清理MySQL中{}的旧数据", uploadDate);

        try {
            // 清理所有相关表
            jdbcTemplate.update("DELETE FROM merchant_analysis WHERE update_date = ?", uploadDate);
            jdbcTemplate.update("DELETE FROM merchant_analysis_temp WHERE update_date = ?", uploadDate);
            jdbcTemplate.update("DELETE FROM time_analysis WHERE update_date = ?", uploadDate);
            jdbcTemplate.update("DELETE FROM food_keywords WHERE update_date = ?", uploadDate);
            jdbcTemplate.update("DELETE FROM merchant_dashboard WHERE stat_date = ?", uploadDate);
            jdbcTemplate.update("DELETE FROM ranking_daily WHERE rank_date = ?", uploadDate);

            // 清理临时表
            jdbcTemplate.update("DELETE FROM food_keywords_temp WHERE update_date = ?", uploadDate);

            logger.info("MySQL数据彻底清理完成: {}", uploadDate);
        } catch (Exception e) {
            logger.error("数据清理失败: {}", e.getMessage());
            throw new RuntimeException("数据清理失败: " + e.getMessage());
        }
    }

    // 修改syncAllDataToMySql方法
    @Transactional
    public void syncAllDataToMySql(String uploadDate) {
        logger.info("开始同步所有数据到MySQL，日期: {}", uploadDate);

        if (!testMySqlConnection()) {
            throw new RuntimeException("MySQL连接失败，无法同步数据");
        }

        try {
            // 先清理现有数据（覆盖模式）
            cleanupExistingData(uploadDate);

            // 确保临时表存在
            ensureTempTablesExist();

            // 从Hive同步数据到MySQL
            syncFromHiveToMySql(uploadDate);

            // MySQL内部数据同步
            syncMerchantAnalysisFromTemp(uploadDate);
            syncTimeAnalysis(uploadDate);
            syncFoodKeywords(uploadDate);
            syncMerchantDashboard(uploadDate);
            syncRankingDaily(uploadDate);
            // 5. 清理临时表数据
            jdbcTemplate.update("DELETE FROM merchant_analysis_temp WHERE update_date = ?", uploadDate);
            jdbcTemplate.update("DELETE FROM food_keywords_temp WHERE update_date = ?", uploadDate);

            logger.info("所有数据同步到MySQL完成: {}", uploadDate);

        } catch (Exception e) {
            logger.error("数据同步到MySQL失败: {}", e.getMessage(), e);
            throw new RuntimeException("数据同步到MySQL失败: " + e.getMessage());
        }
    }
    // 添加从临时表同步到正式表的方法
    private void syncMerchantAnalysisFromTemp(String uploadDate) {
        logger.info("将商户分析数据从临时表同步到正式表: {}", uploadDate);

        String syncSql = "INSERT INTO merchant_analysis " +
                "(merchant_name, avg_rating, avg_taste, avg_environment, avg_service, " +
                "avg_price, total_reviews, positive_reviews, negative_reviews, " +
                "neutral_reviews, positive_rate, update_date, stat_date) " +
                "SELECT merchant_name, avg_rating, avg_taste, avg_environment, avg_service, " +
                "avg_price, total_reviews, positive_reviews, negative_reviews, " +
                "neutral_reviews, positive_rate, update_date, stat_date " +
                "FROM merchant_analysis_temp WHERE update_date = ?";

        int affectedRows = jdbcTemplate.update(syncSql, uploadDate);
        logger.info("商户分析数据同步完成: {} 条记录", affectedRows);
    }

    // 从Hive同步数据到MySQL
    private void syncFromHiveToMySql(String uploadDate) {
        logger.info("从Hive同步数据到MySQL临时表，日期: {}", uploadDate);

        try {
            // 同步商户分析数据
            syncMerchantAnalysisFromHive(uploadDate);
            // 同步时间分析数据
            syncTimeAnalysisFromHive(uploadDate);
            // 同步菜品关键词数据
            syncFoodKeywordsFromHive(uploadDate);
            logger.info("从Hive同步数据到MySQL临时表完成: {}", uploadDate);
        } catch (Exception e) {
            logger.error("从Hive同步数据到MySQL失败: {}", e.getMessage(), e);
            throw new RuntimeException("从Hive同步数据到MySQL失败: " + e.getMessage());
        }
    }
    // 添加商户名称验证方法
    // 修改商户名称验证逻辑，专注于真正的乱码
    private boolean isValidMerchantName(String merchantName) {
        if (merchantName == null || merchantName.trim().isEmpty()) {
            return false;
        }

        String trimmedName = merchantName.trim();

        // 检查长度
        if (trimmedName.length() < 2 || trimmedName.length() > 100) {
            return false;
        }

        // 真正的乱码检测：包含�字符
        if (trimmedName.contains("�")) {
            logger.warn("跳过包含乱码字符�的商户名: {}", trimmedName);
            return false;
        }

        // 排除明显的测试数据
        if (trimmedName.toLowerCase().contains("未知商户") ||
                trimmedName.toLowerCase().contains("test") ||
                trimmedName.toLowerCase().contains("null") ||
                trimmedName.equalsIgnoreCase("空")) {
            return false;
        }

        // 宽松的字符验证：允许中文、英文、数字、常见符号
        // 移除过于严格的正则表达式检查
        if (!trimmedName.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9\\-\\s（）()&···?！!@#￥$%^&*+=_\\[\\]{}\\\\|;:'\",.<>/?]+$")) {
            logger.warn("商户名称可能包含特殊字符，但仍允许: {}", trimmedName);
            // 不再直接返回false，而是记录警告
        }

        return true;
    }
    // 同步商户分析数据从Hive到MySQL
    private void syncMerchantAnalysisFromHive(String uploadDate) throws SQLException {
        String hiveSql = String.format(
                "SELECT merchant, avg_rating, avg_taste, avg_environment, avg_service, " +
                        "avg_price, total_reviews, positive_reviews, negative_reviews, " +
                        "neutral_reviews, positive_rate, update_date " +
                        "FROM %s.dws_merchant_analysis WHERE update_date='%s'",
                hiveDatabase, uploadDate
        );

        // 先清理临时表
        jdbcTemplate.update("DELETE FROM merchant_analysis_temp WHERE update_date = ?", uploadDate);

        String mysqlSql = "INSERT INTO merchant_analysis_temp " +
                "(merchant_name, avg_rating, avg_taste, avg_environment, avg_service, " +
                "avg_price, total_reviews, positive_reviews, negative_reviews, " +
                "neutral_reviews, positive_rate, update_date, stat_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(hiveSql)) {

            int count = 0;
            while (rs.next()) {
                String merchantName = rs.getString("merchant");
                // 过滤乱码和无效商户名
                if (isValidMerchantName(merchantName)) {
                    jdbcTemplate.update(mysqlSql,
                            merchantName,
                            rs.getDouble("avg_rating"),
                            rs.getDouble("avg_taste"),
                            rs.getDouble("avg_environment"),
                            rs.getDouble("avg_service"),
                            rs.getDouble("avg_price"),
                            rs.getInt("total_reviews"),
                            rs.getInt("positive_reviews"),
                            rs.getInt("negative_reviews"),
                            rs.getInt("neutral_reviews"),
                            rs.getDouble("positive_rate"),
                            rs.getString("update_date"),
                            uploadDate
                    );
                    count++;
                }
            }
            logger.info("同步了 {} 条商户分析数据到临时表", count);
        }
    }

    // 同步时间分析数据从Hive到MySQL
    private void syncTimeAnalysisFromHive(String uploadDate) throws SQLException {
        String hiveSql = String.format(
                "SELECT merchant, time_period, period_type, avg_rating, " +
                        "positive_count, negative_count, total_count, update_date " +
                        "FROM %s.dws_time_analysis WHERE update_date='%s'",
                hiveDatabase, uploadDate
        );

        String mysqlSql = "INSERT INTO time_analysis " +
                "(merchant_name, time_period, period_type, avg_rating, " +
                "positive_count, negative_count, total_count, update_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(hiveSql)) {

            while (rs.next()) {
                jdbcTemplate.update(mysqlSql,
                        rs.getString("merchant"),
                        rs.getString("time_period"),
                        rs.getString("period_type"),
                        rs.getDouble("avg_rating"),
                        rs.getInt("positive_count"),
                        rs.getInt("negative_count"),
                        rs.getInt("total_count"),
                        rs.getString("update_date")
                );
            }
        }
    }

    // 同步菜品关键词数据从Hive到MySQL
    private void syncFoodKeywordsFromHive(String uploadDate) throws SQLException {
        String hiveSql = String.format(
                "SELECT merchant, keyword, mention_count, update_date " +
                        "FROM %s.dws_food_keywords WHERE update_date='%s'",
                hiveDatabase, uploadDate
        );

        // 先清空临时数据
        jdbcTemplate.update("DELETE FROM food_keywords_temp WHERE update_date = ?", uploadDate);

        String mysqlSql = "INSERT INTO food_keywords_temp " +
                "(merchant_name, keyword, mention_count, update_date) " +
                "VALUES (?, ?, ?, ?)";

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(hiveSql)) {

            int count = 0;
            while (rs.next()) {
                jdbcTemplate.update(mysqlSql,
                        rs.getString("merchant"),
                        rs.getString("keyword"),
                        rs.getInt("mention_count"),
                        rs.getString("update_date")
                );
                count++;
            }
            logger.info("从Hive同步了 {} 条菜品关键词数据到MySQL临时表", count);
        }
    }



    @Transactional
    public void syncMerchantAnalysis(String uploadDate) {
        // 这里可以根据需要添加额外的数据处理逻辑
        logger.info("商户分析数据同步完成: {}", uploadDate);
    }

    @Transactional
    public void syncTimeAnalysis(String uploadDate) {
        // 这里可以根据需要添加额外的数据处理逻辑
        logger.info("时间分析数据同步完成: {}", uploadDate);
    }

    /**
     * 处理菜品关键词数据的最终同步
     */
    @Transactional
    public void syncFoodKeywords(String uploadDate) {
        // 将临时表数据同步到正式表
        String syncSql = "INSERT INTO food_keywords " +
                "(merchant_name, keyword, mention_count, update_date, create_time) " +
                "SELECT merchant_name, keyword, mention_count, update_date, NOW() " +
                "FROM food_keywords_temp " +
                "WHERE update_date = ? " +
                "ON DUPLICATE KEY UPDATE " +
                "mention_count = VALUES(mention_count), " +
                "create_time = NOW()";

        int affectedRows = jdbcTemplate.update(syncSql, uploadDate);
        logger.info("菜品关键词数据同步完成: {} 条记录受影响", affectedRows);

        // 清理临时表
        jdbcTemplate.update("DELETE FROM food_keywords_temp WHERE update_date = ?", uploadDate);
    }

    @Transactional
    public void syncMerchantDashboard(String uploadDate) {
        // 生成商户仪表板数据
        String sql = "INSERT INTO merchant_dashboard " +
                "(merchant_name, stat_date, avg_rating_7d, avg_taste_7d, avg_service_7d, " +
                "avg_environment_7d, positive_rate_7d, trend_direction, top_foods, update_time) " +
                "SELECT merchant_name, '" + uploadDate + "', " +
                "ROUND(AVG(avg_rating), 2), ROUND(AVG(avg_taste), 2), " +
                "ROUND(AVG(avg_service), 2), ROUND(AVG(avg_environment), 2), " +
                "ROUND(AVG(positive_rate), 2), 'stable', '[]', NOW() " +
                "FROM merchant_analysis " +
                "WHERE update_date >= DATE_SUB('" + uploadDate + "', INTERVAL 7 DAY) " +
                "GROUP BY merchant_name";

        jdbcTemplate.update(sql);
        logger.info("商户仪表板数据同步完成: {}", uploadDate);
    }

    @Transactional
    public void syncRankingDaily(String uploadDate) {
        // 生成每日排行榜数据
        String sql = "INSERT INTO ranking_daily " +
                "(rank_date, rank_type, merchant_name, rank_position, " +
                "composite_score, avg_price, positive_rate, update_time) " +
                "SELECT '" + uploadDate + "', '综合排名', merchant_name, " +
                "ROW_NUMBER() OVER (ORDER BY avg_rating DESC, positive_rate DESC), " +
                "ROUND((avg_rating * 0.6 + positive_rate * 0.4), 2), " +
                "avg_price, positive_rate, NOW() " +
                "FROM merchant_analysis " +
                "WHERE update_date = '" + uploadDate + "'";

        jdbcTemplate.update(sql);
        logger.info("每日排行榜数据同步完成: {}", uploadDate);
    }

    // 辅助方法处理空值
    private Double getDoubleWithDefault(ResultSet rs, String columnName, Double defaultValue) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null || rs.wasNull()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("列 {} 的值 {} 无法转换为Double，使用默认值 {}", columnName, value, defaultValue);
            return defaultValue;
        }
    }

    private Integer getIntWithDefault(ResultSet rs, String columnName, Integer defaultValue) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null || rs.wasNull()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("列 {} 的值 {} 无法转换为Integer，使用默认值 {}", columnName, value, defaultValue);
            return defaultValue;
        }
    }
}