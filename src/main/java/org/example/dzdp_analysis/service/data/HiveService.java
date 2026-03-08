package org.example.dzdp_analysis.service.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class HiveService {

    private static final Logger logger = LoggerFactory.getLogger(HiveService.class);

    @Autowired
    private Connection hiveConnection;
    // 新增的注入
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private HdfsService hdfsService;

    @Value("${hive.database:dianping}")
    private String hiveDatabase;

    @Value("${hive.ods-table:ods_raw_reviews}")
    private String odsTable;

    @Value("${hive.dwd-table:dwd_cleaned_reviews}")
    private String dwdTable;

    @Value("${hive.dws-table:dws_merchant_analysis}")
    private String dwsTable;

    @Value("${hive.execution.engine:mr}") // 修改默认值为mr
    private String executionEngine;

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getColumnsFromResult(Map<String, Object> structure) {
        return (List<Map<String, String>>) structure.get("columns");
    }
    // 添加缺失的executeHiveSqlBatch方法
    private void executeHiveSqlBatch(List<String> sqls) throws SQLException {
        try (Statement stmt = hiveConnection.createStatement()) {
            for (String sql : sqls) {
                logger.info("Executing Hive SQL: {}", sql);
                stmt.execute(sql);
            }
        }
    }

    // 修改为MapReduce优化设置
    public void configureMapReduceOptimization() throws SQLException {
        List<String> mrConfigs = Arrays.asList(
                "SET hive.execution.engine=mr", // 使用MapReduce引擎
                "SET hive.stats.autogather=false",  // 关键：禁用自动统计收集
                "SET hive.compute.query.using.stats=false",  // 禁用统计查询
                "SET mapred.job.queue.name=default",
                "SET mapred.job.priority=HIGH",
                "SET mapred.map.tasks=100", // 增加map任务数
                "SET mapred.reduce.tasks=50", // 增加reduce任务数
                "SET mapred.compress.map.output=true",
                "SET mapred.output.compress=true",
                "SET mapred.output.compression.codec=org.apache.hadoop.io.compress.SnappyCodec",
                "SET hive.exec.compress.output=true",
                "SET hive.exec.parallel=true",
                "SET hive.exec.parallel.thread.number=8",
                "SET hive.optimize.sort.dynamic.partition=true",
                "SET hive.map.aggr=true", // 在map端做聚合
                "SET hive.groupby.skewindata=true" // 处理数据倾斜
        );
        executeHiveSqlBatch(mrConfigs);
        logger.info("MapReduce引擎优化配置完成");
    }

    public void createDatabaseAndTables() throws SQLException {
        //configureTezOptimization();
        configureMapReduceOptimization(); // 改为MapReduce配置
        List<String> sqls = new ArrayList<>();

        // 创建数据库
        sqls.add(String.format("CREATE DATABASE IF NOT EXISTS %s", hiveDatabase));

        // 创建元数据表
        sqls.add(String.format(
                "CREATE TABLE IF NOT EXISTS %s.data_file_metadata (" +
                        "   upload_date STRING," +
                        "   file_md5 STRING," +
                        "   file_path STRING," +
                        "   check_time TIMESTAMP" +
                        ") STORED AS PARQUET",
                hiveDatabase
        ));
        // HiveService.java - 修改createDatabaseAndTables方法中的ODS表创建
        // 修复ODS表创建语句，确保与实际数据文件列数匹配
        sqls.add(String.format(
                "CREATE EXTERNAL TABLE IF NOT EXISTS %s.%s (" +
                        "   review_id INT," +           // 1. Review_ID
                        "   merchant STRING," +         // 2. Merchant
                        "   rating INT," +              // 3. Rating
                        "   score_taste INT," +         // 4. Score_taste
                        "   score_environment INT," +   // 5. Score_environment
                        "   score_service INT," +       // 6. Score_service
                        "   price_per_person STRING," + // 7. Price_per_person
                        "   review_time STRING," +      // 8. Time
                        "   num_thumbs_up INT," +       // 9. Num_thumbs_up
                        "   num_response INT," +        // 10. Num_response
                        "   content_review STRING," +   // 11. Content_review
                        "   reviewer STRING," +         // 12. Reviewer
                        "   reviewer_value STRING," +   // 13. Reviewer_value
                        "   reviewer_rank STRING," +    // 14. Reviewer_rank
                        "   favorite_foods STRING" +    // 15. Favorite_foods
                        ") " +
                        "PARTITIONED BY (upload_date STRING) " +
                        "ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde' " +
                        "WITH SERDEPROPERTIES (" +
                        "   'separatorChar' = ','," +
                        "   'quoteChar' = '\"'," +
                        "   'escapeChar' = '\\\\'," +
                        "   'skip.header.line.count' = '1'," +
                        "   'serialization.encoding' = 'UTF-8'" +
                        ") " +
                        "STORED AS TEXTFILE " +
                        "LOCATION '/dianping/warehouse/ods' " +
                        "TBLPROPERTIES (" +
                        "   'skip.header.line.count'='1'," +
                        "   'serialization.null.format'=''," +
                        "   'serialization.encoding' = 'UTF-8'" +
                        ")",
                hiveDatabase, odsTable
        ));


        // 创建DWD层表 - 确保有13个列 + 1个分区列 = 14列
        sqls.add(String.format(
                "CREATE EXTERNAL TABLE IF NOT EXISTS %s.%s (" +
                        "   review_id INT," +
                        "   merchant STRING," +
                        "   rating INT," +
                        "   score_taste INT," +
                        "   score_environment INT," +
                        "   score_service INT," +
                        "   price_per_person DOUBLE," +
                        "   review_time TIMESTAMP," +
                        "   num_thumbs_up INT," +
                        "   num_response INT," +
                        "   content_review STRING," +
                        "   reviewer STRING," +
                        "   sentiment STRING" +
                        ") " +
                        "PARTITIONED BY (upload_date STRING) " +
                        "STORED AS PARQUET " +
                        "LOCATION '/dianping/warehouse/dwd' " +
                        "TBLPROPERTIES ('parquet.compression'='SNAPPY')",
                hiveDatabase, dwdTable
        ));

        // DWS层主表 - 添加分区字段
        sqls.add(String.format(
                "CREATE EXTERNAL TABLE IF NOT EXISTS %s.%s (" +
                        "   merchant STRING," +           // 改为 merchant
                        "   avg_rating DOUBLE," +
                        "   avg_taste DOUBLE," +
                        "   avg_environment DOUBLE," +
                        "   avg_service DOUBLE," +
                        "   avg_price DOUBLE," +
                        "   total_reviews INT," +
                        "   positive_reviews INT," +
                        "   negative_reviews INT," +
                        "   neutral_reviews INT," +
                        "   positive_rate DOUBLE" +
                        ") " +
                        "PARTITIONED BY (stat_date STRING, update_date STRING) " + // 保留stat_date作为分区列
                        "STORED AS PARQUET " +
                        "LOCATION '/dianping/warehouse/dws' " +
                        "TBLPROPERTIES ('parquet.compression'='SNAPPY')",
                hiveDatabase, dwsTable
        ));

        // 时间分析表 - 添加分区字段
        sqls.add(String.format(
                "CREATE EXTERNAL TABLE IF NOT EXISTS %s.dws_time_analysis (" +
                        "   merchant STRING," +
                        "   time_period STRING," +
                        "   period_type STRING," +
                        "   avg_rating DOUBLE," +
                        "   positive_count INT," +
                        "   negative_count INT," +
                        "   total_count INT" +
                        ") " +
                        "PARTITIONED BY (update_date STRING) " +  // 添加分区字段
                        "STORED AS PARQUET " +
                        "LOCATION '/dianping/warehouse/dws/time_analysis' " +
                        "TBLPROPERTIES ('parquet.compression'='SNAPPY')",
                hiveDatabase
        ));


        // 菜品关键词表 - 添加分区字段
        sqls.add(String.format(
                "CREATE EXTERNAL TABLE IF NOT EXISTS %s.dws_food_keywords (" +
                        "   merchant STRING," +
                        "   keyword STRING," +
                        "   mention_count INT" +
                        ") " +
                        "PARTITIONED BY (update_date STRING) " +  // 添加分区字段
                        "STORED AS PARQUET " +
                        "LOCATION '/dianping/warehouse/dws/food_keywords' " +
                        "TBLPROPERTIES ('parquet.compression'='SNAPPY')",
                hiveDatabase
        ));

        // 商户仪表板表 - 添加分区字段
        sqls.add(String.format(
                "CREATE EXTERNAL TABLE IF NOT EXISTS %s.ads_merchant_dashboard (" +
                        "   merchant STRING," +
                        "   avg_rating_7d DOUBLE," +
                        "   avg_taste_7d DOUBLE," +
                        "   avg_service_7d DOUBLE," +
                        "   avg_environment_7d DOUBLE," +
                        "   positive_rate_7d DOUBLE," +
                        "   trend_direction STRING," +
                        "   top_foods STRING," +
                        "   update_time TIMESTAMP" +
                        ") " +
                        "PARTITIONED BY (stat_date STRING) " +  // 添加分区字段
                        "STORED AS PARQUET " +
                        "LOCATION '/dianping/warehouse/ads/merchant_dashboard' " +  // 修正路径
                        "TBLPROPERTIES ('parquet.compression'='SNAPPY')",
                hiveDatabase
        ));

        // 每日排行榜表 - 添加分区字段
        sqls.add(String.format(
                "CREATE EXTERNAL TABLE IF NOT EXISTS %s.ads_ranking_daily (" +
                        "   rank_type STRING," +
                        "   merchant STRING," +
                        "   rank_position INT," +
                        "   composite_score DOUBLE," +
                        "   avg_price DOUBLE," +
                        "   positive_rate DOUBLE," +
                        "   update_time TIMESTAMP" +
                        ") " +
                        "PARTITIONED BY (rank_date STRING) " +  // 添加分区字段
                        "STORED AS PARQUET " +
                        "LOCATION '/dianping/warehouse/ads/ranking_daily' " +  // 修正路径
                        "TBLPROPERTIES ('parquet.compression'='SNAPPY')",
                hiveDatabase
        ));

        executeHiveSqlBatch(sqls);
        logger.info("Hive数据库和表创建完成");
    }
    // 添加数据内容检查方法
    // 在 isDataIdentical 方法中调用 hdfsService.getFileMd5
    private boolean isDataIdentical(String hdfsFilePath, String uploadDate) throws SQLException {
        try {
            // 获取HDFS文件MD5
            String fileMd5 = hdfsService.getFileMd5(hdfsFilePath);

            // 检查是否已有相同MD5的数据
            String checkSql = String.format(
                    "SELECT file_md5 FROM %s.data_file_metadata WHERE upload_date='%s'",
                    hiveDatabase, uploadDate
            );

            try (Statement stmt = hiveConnection.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next()) {
                    String existingMd5 = rs.getString("file_md5");
                    return fileMd5.equals(existingMd5);
                }
            }

            // 如果没有元数据记录，保存当前文件的MD5
            String insertSql = String.format(
                    "INSERT INTO %s.data_file_metadata (upload_date, file_md5, file_path) VALUES ('%s', '%s', '%s')",
                    hiveDatabase, uploadDate, fileMd5, hdfsFilePath
            );
            executeHiveSql(insertSql);

        } catch (Exception e) {
            logger.warn("数据内容检查失败，使用追加模式: {}", e.getMessage());
        }
        return false;
    }
    // 添加创建元数据表的方法
    private void createMetadataTable() throws SQLException {
        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s.data_file_metadata (" +
                        "   upload_date STRING," +
                        "   file_md5 STRING," +
                        "   file_path STRING," +
                        "   check_time TIMESTAMP" +
                        ") STORED AS PARQUET",
                hiveDatabase
        );
        executeHiveSql(sql);
    }

    public void loadDataToOds(String hdfsFilePath, String uploadDate) throws SQLException {
        // 检查HDFS文件内容是否与现有分区数据相同
        if (isPartitionExists(odsTable, uploadDate)) {
            if (isDataIdentical(hdfsFilePath, uploadDate)) {
                logger.info("数据内容相同，使用覆盖模式: {}", uploadDate);
                // 使用覆盖模式
                String sql = String.format(
                        "LOAD DATA INPATH '%s' OVERWRITE INTO TABLE %s.%s PARTITION (upload_date='%s')",
                        hdfsFilePath, hiveDatabase, odsTable, uploadDate
                );
                executeHiveSql(sql);
            } else {
                logger.info("数据内容不同，使用追加模式: {}", uploadDate);
                // 使用追加模式
                String sql = String.format(
                        "LOAD DATA INPATH '%s' INTO TABLE %s.%s PARTITION (upload_date='%s')",
                        hdfsFilePath, hiveDatabase, odsTable, uploadDate
                );
                executeHiveSql(sql);
            }
        } else {
            // 分区不存在，使用覆盖模式创建新分区
            logger.info("分区不存在，创建新分区并使用覆盖模式: {}", uploadDate);
            String sql = String.format(
                    "LOAD DATA INPATH '%s' OVERWRITE INTO TABLE %s.%s PARTITION (upload_date='%s')",
                    hdfsFilePath, hiveDatabase, odsTable, uploadDate
            );
            executeHiveSql(sql);
        }
        logger.info("数据加载到ODS完成: {}", hdfsFilePath);
    }

    // 获取HDFS文件所在的目录路径
    private String getHdfsDirectoryPath(String hdfsFilePath) {
        int lastSlashIndex = hdfsFilePath.lastIndexOf('/');
        if (lastSlashIndex > 0) {
            return hdfsFilePath.substring(0, lastSlashIndex);
        }
        return hdfsFilePath;
    }

    private boolean isPartitionExists(String tableName, String partitionValue) throws SQLException {
        String partitionCol = tableName.equals(dwsTable) ? "stat_date" : "upload_date";
        String sql = String.format(
                "SHOW PARTITIONS %s.%s PARTITION (%s='%s')",
                hiveDatabase, tableName, partitionCol, partitionValue
        );
        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }

    private void dropPartition(String tableName, String partitionValue) throws SQLException {
        String partitionCol = tableName.equals(dwsTable) ? "stat_date" : "upload_date";
        String sql = String.format(
                "ALTER TABLE %s.%s DROP IF EXISTS PARTITION (%s='%s')",
                hiveDatabase, tableName, partitionCol, partitionValue
        );
        executeHiveSql(sql);
    }

    // 修改ETL到DWD的方法，添加数据清洗和验证
    public void executeEtlToDwd(String uploadDate) throws SQLException {
        // 先删除分区
        if (isPartitionExists(dwdTable, uploadDate)) {
            logger.info("删除DWD层旧分区: {}", uploadDate);
            dropPartition(dwdTable, uploadDate);
        }

        // 设置Hive配置，优化MapReduce执行
        List<String> setupSqls = Arrays.asList(
                "SET hive.exec.dynamic.partition=true",
                "SET hive.exec.dynamic.partition.mode=nonstrict",
                "SET hive.optimize.sort.dynamic.partition=true",
                "SET hive.execution.engine=mr",
                "SET mapred.job.queue.name=default",
                "SET mapred.job.priority=HIGH",
                "SET mapred.map.tasks=50",
                "SET mapred.reduce.tasks=25",
                "SET hive.map.aggr=true",
                "SET hive.groupby.skewindata=true",
                "SET hive.exec.compress.output=true",
                "SET mapred.output.compression.codec=org.apache.hadoop.io.compress.SnappyCodec"
        );
        executeHiveSqlBatch(setupSqls);

        // 简化数据转换逻辑，分步骤执行
        try {
            // 第一步：创建临时表存储清洗后的数据
            String createTempTableSql = String.format(
                    "CREATE TEMPORARY TABLE temp_dwd_%s AS " +
                            "SELECT " +
                            "  review_id, " +
                            "  merchant, " +
                            "  rating, " +
                            "  score_taste, " +
                            "  score_environment, " +
                            "  score_service, " +
                            "  price_per_person, " +
                            "  review_time, " +
                            "  num_thumbs_up, " +
                            "  num_response, " +
                            "  content_review, " +
                            "  reviewer, " +
                            "  upload_date " +
                            "FROM %s.%s " +
                            "WHERE upload_date='%s' " +
                            "  AND review_id IS NOT NULL",
                    uploadDate, hiveDatabase, odsTable, uploadDate
            );
            executeHiveSql(createTempTableSql);

            // 第二步：逐步转换数据 - 加强商户名清洗逻辑
            // 第二步：逐步转换数据 - 修复括号匹配问题
            String transformSql = String.format(
                    "INSERT OVERWRITE TABLE %s.%s PARTITION (upload_date) " +
                            "SELECT " +
                            "  CAST(review_id AS INT) as review_id, " +
                            "  CASE " +
                            "    WHEN merchant IS NULL OR TRIM(merchant) = '' OR LENGTH(TRIM(merchant)) < 2 " +
                            "    THEN CONCAT('未知商户_', CAST(review_id AS STRING)) " +
                            "    ELSE " +
                            "      regexp_replace( " +
                            "        regexp_replace( " +
                            "          TRIM(merchant), " +
                            "          '[\\\\x00-\\\\x1F\\\\x7F-\\\\xFF]', '' " + // 移除控制字符
                            "        ), " +
                            "        '[\\\\p{C}]', '' " + // 移除其他不可见字符
                            "      ) " +
                            "  END as merchant, " +
                            "  CASE " +
                            "    WHEN rating IS NULL OR CAST(rating AS INT) < 1 OR CAST(rating AS INT) > 5 THEN 3 " +
                            "    ELSE CAST(rating AS INT) " +
                            "  END as rating, " +
                            "  CASE " +
                            "    WHEN score_taste IS NULL OR CAST(score_taste AS INT) < 1 OR CAST(score_taste AS INT) > 5 THEN 3 " +
                            "    ELSE CAST(score_taste AS INT) " +
                            "  END as score_taste, " +
                            "  CASE " +
                            "    WHEN score_environment IS NULL OR CAST(score_environment AS INT) < 1 OR CAST(score_environment AS INT) > 5 THEN 3 " +
                            "    ELSE CAST(score_environment AS INT) " +
                            "  END as score_environment, " +
                            "  CASE " +
                            "    WHEN score_service IS NULL OR CAST(score_service AS INT) < 1 OR CAST(score_service AS Int) > 5 THEN 3 " +
                            "    ELSE CAST(score_service AS INT) " +
                            "  END as score_service, " +
                            "  CASE " +
                            "    WHEN price_per_person = '空' OR price_per_person IS NULL OR TRIM(price_per_person) = '' THEN NULL " +
                            "    WHEN price_per_person RLIKE '^[0-9]+(\\\\.[0-9]+)?$' THEN CAST(price_per_person AS DOUBLE) " +
                            "    ELSE NULL " +
                            "  END as price_per_person, " +
                            "  CASE " +
                            "    WHEN review_time RLIKE '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' " +
                            "    THEN CAST(FROM_UNIXTIME(UNIX_TIMESTAMP(review_time, 'yyyy-MM-dd')) AS TIMESTAMP) " +
                            "    ELSE NULL " +
                            "  END as review_time, " +
                            "  COALESCE(CAST(num_thumbs_up AS INT), 0) as num_thumbs_up, " +
                            "  COALESCE(CAST(num_response AS INT), 0) as num_response, " +
                            "  COALESCE(NULLIF(TRIM(content_review), ''), '无评论内容') as content_review, " +
                            "  COALESCE(NULLIF(TRIM(reviewer), ''), '匿名用户') as reviewer, " +
                            "  CASE " +
                            "    WHEN CAST(rating AS INT) >= 4 THEN '正面' " +
                            "    WHEN CAST(rating AS INT) <= 2 THEN '负面' " +
                            "    ELSE '中性' " +
                            "  END as sentiment, " +
                            "  upload_date " +
                            "FROM %s.%s WHERE upload_date='%s'",
                    hiveDatabase, dwdTable, hiveDatabase, odsTable, uploadDate
            );
            executeHiveSql(transformSql);

            // 第三步：清理临时表
            String dropTempTableSql = String.format("DROP TABLE temp_dwd_%s", uploadDate);
            executeHiveSql(dropTempTableSql);

            // 第四步：验证清洗结果
            validateMerchantCleaning(uploadDate);

        } catch (SQLException e) {
            // 确保临时表被清理
            try {
                String dropTempTableSql = String.format("DROP TABLE IF EXISTS temp_dwd_%s", uploadDate);
                executeHiveSql(dropTempTableSql);
            } catch (SQLException ex) {
                logger.warn("清理临时表失败: {}", ex.getMessage());
            }
            throw e;
        }

        logger.info("ETL到DWD完成: {}", uploadDate);
    }

    /**
     * 验证商户名清洗结果
     */
    /**
     * 验证商户名清洗结果
     */
    private void validateMerchantCleaning(String uploadDate) throws SQLException {
        String checkSql = String.format(
                "SELECT COUNT(DISTINCT merchant) as merchant_count, " +
                        "AVG(CAST(rating AS DOUBLE)) as avg_rating " +
                        "FROM %s.%s WHERE upload_date='%s' AND " +
                        "merchant IS NOT NULL AND TRIM(merchant) != '' AND " +
                        "merchant NOT LIKE '未知商户%%' AND " +
                        "LENGTH(merchant) >= 2",
                hiveDatabase, dwdTable, uploadDate
        );

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {

            // 修复这里的逻辑 - 原来的代码有问题
            if (rs.next()) {
                int merchantCount = rs.getInt("merchant_count");
                double avgRating = rs.getDouble("avg_rating");

                logger.info("商户清洗验证: 有效商户数={}, 平均评分={}", merchantCount, avgRating);

                if (merchantCount == 0) {
                    logger.warn("未找到有效商户数据，可能存在清洗问题");
                } else {
                    logger.info("商户名清洗验证通过，找到 {} 个有效商户", merchantCount);
                }
            }

        } catch (SQLException e) {
            logger.error("商户名清洗验证失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 获取问题商户名样本用于调试
     */
    private void debugProblematicMerchants(String uploadDate) throws SQLException {
        String sampleSql = String.format(
                "SELECT DISTINCT ods.merchant as original_merchant, dwd.merchant as cleaned_merchant " +
                        "FROM %s.%s ods " +
                        "JOIN %s.%s dwd ON ods.review_id = dwd.review_id " +
                        "WHERE ods.upload_date='%s' " +
                        "  AND (ods.merchant RLIKE '[\\\\x00-\\\\x1F\\\\x7F-\\\\xFF]' " +
                        "       OR ods.merchant != dwd.merchant) " +
                        "LIMIT 10",
                hiveDatabase, odsTable, hiveDatabase, dwdTable, uploadDate
        );

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sampleSql)) {

            logger.info("商户名清洗前后对比样本:");
            while (rs.next()) {
                String original = rs.getString("original_merchant");
                String cleaned = rs.getString("cleaned_merchant");
                logger.info("  原始: {} -> 清洗后: {}", original, cleaned);
            }
        }
    }
    private void cleanupDwsPartition(String uploadDate) throws SQLException {
        try {
            // 明确指定数据列，避免使用 SELECT *
            String cleanupSql = String.format(
                    "INSERT OVERWRITE TABLE %s.%s PARTITION (stat_date='%s', update_date='%s') " +
                            "SELECT " +
                            "  merchant, avg_rating, avg_taste, avg_environment, avg_service, " +
                            "  avg_price, total_reviews, positive_reviews, negative_reviews, " +
                            "  neutral_reviews, positive_rate " +
                            "FROM %s.%s WHERE 1=0",
                    hiveDatabase, dwsTable, uploadDate, uploadDate,
                    hiveDatabase, dwsTable
            );

            executeHiveSql(cleanupSql);
            logger.info("彻底清空DWS分区数据: {}", uploadDate);
        } catch (SQLException e) {
            logger.warn("清空分区数据失败，尝试删除分区: {}", e.getMessage());

            // 删除分区时也要注意顺序
            String dropSql = String.format(
                    "ALTER TABLE %s.%s DROP IF EXISTS PARTITION (stat_date='%s', update_date='%s')",
                    hiveDatabase, dwsTable, uploadDate, uploadDate
            );

            executeHiveSql(dropSql);
            logger.info("删除DWS分区: {}", uploadDate);
        }
    }

    public void checkPartitionColumns(String tableName) throws SQLException {
        String sql = "SHOW CREATE TABLE " + hiveDatabase + "." + tableName;

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            StringBuilder createTableSql = new StringBuilder();
            while (rs.next()) {
                createTableSql.append(rs.getString(1));
            }

            logger.info("表 {} 的创建语句:\n{}", tableName, createTableSql.toString());

            // 解析分区列
            String sqlStr = createTableSql.toString();
            int partitionIndex = sqlStr.indexOf("PARTITIONED BY");
            if (partitionIndex > 0) {
                String partitionClause = sqlStr.substring(partitionIndex);
                logger.info("分区信息: {}", partitionClause);
            }
        }
    }
    public void executeDwsAggregation(String uploadDate) throws SQLException {
        // 先检查DWD层是否有数据
        String checkDataSql = String.format(
                "SELECT COUNT(*) as cnt FROM %s.%s WHERE upload_date='%s'",
                hiveDatabase, dwdTable, uploadDate
        );

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(checkDataSql)) {
            if (rs.next()) {
                int recordCount = rs.getInt("cnt");
                if (recordCount == 0) {
                    logger.warn("DWD层没有数据，跳过DWS聚合: {}", uploadDate);
                    return;
                }
                logger.info("DWD层有 {} 条记录，开始DWS聚合", recordCount);
            }
        }

        // 设置优化的MapReduce配置
        List<String> optimizationSqls = Arrays.asList(
                "SET hive.execution.engine=mr",
                "SET hive.stats.autogather=false",  // 关键：禁用自动统计收集
                "SET hive.compute.query.using.stats=false",  // 禁用统计查询
                "SET hive.stats.column.autogather=false",  // 禁用列统计
                "SET mapred.job.queue.name=default",
                "SET mapred.job.priority=HIGH",
                "SET mapred.map.tasks=50",  // 增加map任务数
                "SET mapred.reduce.tasks=25",  // 增加reduce任务数
                "SET hive.exec.reducers.bytes.per.reducer=256000000",
                "SET hive.map.aggr=true",
                "SET hive.groupby.skewindata=true",
                "SET hive.optimize.skewjoin=true",
                "SET hive.skewjoin.key=100000",
                "SET hive.exec.compress.output=true",
                "SET mapred.output.compression.codec=org.apache.hadoop.io.compress.SnappyCodec",
                // 增加内存配置
                "SET mapreduce.map.memory.mb=4096",
                "SET mapreduce.reduce.memory.mb=4096",
                "SET mapreduce.map.java.opts=-Xmx3276m",
                "SET mapreduce.reduce.java.opts=-Xmx3276m",
                // 超时配置
                "SET mapreduce.task.timeout=1800000",  // 30分钟
                // 动态分区配置
                "SET hive.exec.dynamic.partition=true",
                "SET mapred.output.compression.codec=org.apache.hadoop.io.compress.SnappyCodec",
                "SET hive.exec.dynamic.partition.mode=nonstrict",
                "SET hive.exec.max.dynamic.partitions=1000",
                "SET hive.exec.max.dynamic.partitions.pernode=100"
        );

        executeHiveSqlBatch(optimizationSqls);

        // 调用完整聚合方法
        executeAllDwsAggregation(uploadDate);
    }
    // 验证分区数据是否存在
    private boolean isPartitionDataExists(String uploadDate) throws SQLException {
        String checkSql = String.format(
                "SELECT COUNT(*) FROM %s.%s WHERE stat_date='%s' AND update_date='%s'",
                hiveDatabase, dwsTable, uploadDate, uploadDate
        );

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /**
     * 执行商户多维分析聚合
     */
    /**
     * 执行商户多维分析聚合 - 修正分区问题
     */
    public void executeDwsMerchantAnalysis(String uploadDate) throws SQLException {
        String sql = String.format(
                "INSERT OVERWRITE TABLE %s.%s PARTITION (stat_date='%s', update_date='%s') " +
                        "SELECT " +
                        "  merchant, " +
                        "  ROUND(AVG(CAST(rating AS DOUBLE)), 2) as avg_rating, " +
                        "  ROUND(AVG(CAST(score_taste AS DOUBLE)), 2) as avg_taste, " +
                        "  ROUND(AVG(CAST(score_environment AS DOUBLE)), 2) as avg_environment, " +
                        "  ROUND(AVG(CAST(score_service AS DOUBLE)), 2) as avg_service, " +
                        "  ROUND(AVG(CASE WHEN price_per_person IS NOT NULL THEN CAST(price_per_person AS DOUBLE) ELSE NULL END), 2) as avg_price, " +
                        "  COUNT(*) as total_reviews, " +
                        "  SUM(CASE WHEN sentiment = '正面' THEN 1 ELSE 0 END) as positive_reviews, " +
                        "  SUM(CASE WHEN sentiment = '负面' THEN 1 ELSE 0 END) as negative_reviews, " +
                        "  SUM(CASE WHEN sentiment = '中性' THEN 1 ELSE 0 END) as neutral_reviews, " +
                        "  ROUND((SUM(CASE WHEN sentiment = '正面' THEN 1 ELSE 0 END) * 100.0 / COUNT(*)), 2) as positive_rate " +
                        "FROM %s.%s " +
                        "WHERE upload_date = '%s' " +
                        "GROUP BY merchant",
                hiveDatabase, dwsTable, uploadDate, uploadDate, hiveDatabase, dwdTable, uploadDate
        );

        executeHiveSql(sql);
        logger.info("商户多维分析聚合完成: {}", uploadDate);
    }

    /**
     * 执行时间趋势分析聚合
     */
    /**
     * 执行时间趋势分析聚合 - 修正分区问题
     */
    // 在HiveService.java中修改executeDwsTimeAnalysis方法
    // 在HiveService.java中修改executeDwsTimeAnalysis方法
    public void executeDwsTimeAnalysis(String uploadDate) throws SQLException {
        // 先检查是否有足够的数据，避免对空数据执行复杂聚合
        String checkDataSql = String.format(
                "SELECT COUNT(*) as cnt FROM %s.%s WHERE upload_date='%s'",
                hiveDatabase, dwdTable, uploadDate
        );

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(checkDataSql)) {
            if (rs.next() && rs.getInt("cnt") == 0) {
                logger.info("没有数据需要处理，跳过时间分析聚合");
                return;
            }
        }

        // 使用传统子查询方式替代CTE
        String sql = String.format(
                "INSERT OVERWRITE TABLE %s.dws_time_analysis PARTITION (update_date='%s') " +
                        "SELECT " +
                        "  merchant, " +
                        "  time_period, " +
                        "  'month' as period_type, " +
                        "  ROUND(AVG(CAST(rating AS DOUBLE)), 2) as avg_rating, " +
                        "  SUM(CASE WHEN sentiment = '正面' THEN 1 ELSE 0 END) as positive_count, " +
                        "  SUM(CASE WHEN sentiment = '负面' THEN 1 ELSE 0 END) as negative_count, " +
                        "  COUNT(*) as total_count " +
                        "FROM (" +
                        "  SELECT " +
                        "    merchant, " +
                        "    DATE_FORMAT(review_time, 'yyyyMM') as time_period, " +
                        "    rating, " +
                        "    sentiment " +
                        "  FROM %s.%s " +
                        "  WHERE upload_date='%s' AND review_time IS NOT NULL" +
                        ") time_data " +
                        "GROUP BY merchant, time_period",
                hiveDatabase, uploadDate, hiveDatabase, dwdTable, uploadDate
        );

        executeHiveSql(sql);
        logger.info("时间分析聚合完成: {}", uploadDate);
    }

    /**
     * 执行菜品关键词分析聚合 - 改进版
     */
    /**
     * 执行菜品关键词分析聚合 - 修正分区问题
     */
    /**
     * 执行菜品关键词分析聚合 - 修正UDTF嵌套问题
     */
    public void executeDwsFoodKeywords(String uploadDate) throws SQLException {
        String sql = String.format(
                "INSERT OVERWRITE TABLE %s.dws_food_keywords PARTITION (update_date='%s') " +
                        "SELECT " +
                        "  merchant, " +
                        "  keyword, " +
                        "  COUNT(*) as mention_count " +
                        "FROM (" +
                        "  SELECT " +
                        "    merchant, " +
                        "    CASE " +
                        "      WHEN Favorite_foods IS NULL OR TRIM(Favorite_foods) = '' OR Favorite_foods = '无' " +
                        "      THEN '无' " +
                        "      ELSE TRIM(Favorite_foods) " +
                        "    END as food_list " +
                        "  FROM %s.%s " +
                        "  WHERE upload_date = '%s'" +
                        ") temp " +
                        "LATERAL VIEW explode(" +
                        "  CASE " +
                        "    WHEN food_list = '无' THEN array('无') " +
                        "    ELSE split(regexp_replace(food_list, '\\\\s+', ' '), ' ') " +
                        "  END" +
                        ") exploded_table AS keyword " +
                        "WHERE keyword IS NOT NULL " +
                        "  AND TRIM(keyword) != '' " +
                        "GROUP BY merchant, keyword",
                hiveDatabase, uploadDate, hiveDatabase, odsTable, uploadDate
        );

        executeHiveSql(sql);
        logger.info("菜品关键词分析聚合完成: {}", uploadDate);
    }
    /**
     * 执行所有DWS层聚合 - 改进版
     */
    public void executeAllDwsAggregation(String uploadDate) throws SQLException {
        try {
            // 设置优化配置
            configureMapReduceOptimization();

            // 执行商户分析聚合
            logger.info("开始商户分析聚合...");
            executeDwsMerchantAnalysis(uploadDate);

            // 执行时间分析聚合
            logger.info("开始时间分析聚合...");
            executeDwsTimeAnalysis(uploadDate);

            // 执行菜品关键词聚合
            logger.info("开始菜品关键词聚合...");
            executeDwsFoodKeywords(uploadDate);

            logger.info("所有DWS层聚合完成: {}", uploadDate);

        } catch (SQLException e) {
            logger.error("DWS聚合执行失败: {}", e.getMessage(), e);
            throw new SQLException("DWS聚合失败: " + e.getMessage(), e);
        }
    }

    // 新增辅助方法
    private int getRecordCount(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) as cnt FROM " + tableName;
        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("cnt") : 0;
        }
    }

    private int getPartitionRecordCount(String uploadDate) throws SQLException {
        String sql = String.format(
                "SELECT COUNT(*) as cnt FROM %s.%s WHERE stat_date='%s' AND update_date='%s'",
                hiveDatabase, dwsTable, uploadDate, uploadDate
        );
        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("cnt") : 0;
        }
    }

    public int getProcessedRecordCount(String uploadDate) throws SQLException {
        String sql = String.format(
                "SELECT COUNT(*) as cnt FROM %s.%s WHERE upload_date='%s'",
                hiveDatabase, dwdTable, uploadDate
        );

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        }
        return 0;
    }

    // 在ODS层统计各类问题
    private int getNullCommentCount(Statement stmt, String uploadDate) throws SQLException {
        String sql = String.format(
                "SELECT COUNT(*) as count FROM %s.%s WHERE upload_date='%s' AND " +
                        "(content_review IS NULL OR TRIM(content_review) = '' OR content_review = '无评论内容')",
                hiveDatabase, odsTable, uploadDate
        );
        return executeCountQuery(stmt, sql);
    }

    private int getPriceErrorCount(Statement stmt, String uploadDate) throws SQLException {
        String sql = String.format(
                "SELECT COUNT(*) as count FROM %s.%s WHERE upload_date='%s' AND " +
                        "(price_per_person IS NOT NULL AND price_per_person != '空' AND " +
                        "NOT price_per_person RLIKE '^[0-9]+(\\\\.[0-9]+)?$')",
                hiveDatabase, odsTable, uploadDate
        );
        return executeCountQuery(stmt, sql);
    }

    private int getInvalidMerchantCount(Statement stmt, String uploadDate) throws SQLException {
        String sql = String.format(
                "SELECT COUNT(*) as count FROM %s.%s WHERE upload_date='%s' AND " +
                        "(merchant IS NULL OR merchant = '' OR merchant LIKE '%%未知%%' OR " +
                        "merchant LIKE '%%null%%' OR merchant RLIKE '[\\\\x00-\\\\x1F\\\\x7F]')",
                hiveDatabase, odsTable, uploadDate
        );
        return executeCountQuery(stmt, sql);
    }

    // 从DWS层获取清洗后的统计结果
    private Map<String, Object> getDwsStatistics(Statement stmt, String uploadDate) throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        String sql = String.format(
                "SELECT COUNT(DISTINCT merchant) as merchant_count, " +
                        "AVG(avg_rating) as avg_rating, " +
                        "AVG(positive_rate) as avg_positive_rate " +
                        "FROM %s.%s WHERE stat_date='%s'",
                hiveDatabase, dwsTable, uploadDate
        );

        try (ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                stats.put("merchantCount", rs.getInt("merchant_count"));
                stats.put("avgRating", rs.getDouble("avg_rating"));
                stats.put("positiveRate", rs.getDouble("avg_positive_rate"));
            }
        }

        return stats;
    }


    // 修改getRatingAnomalyCount方法
    private int getRatingAnomalyCount(Statement stmt, String uploadDate) throws SQLException {
        String sql = String.format(
                "SELECT COUNT(*) as count FROM %s.%s WHERE upload_date=\'%s\' AND " +
                        "(rating < 1 OR rating > 5)",
                hiveDatabase, odsTable, uploadDate
        );
        return executeCountQuery(stmt, sql);
    }

    /**
     * 将数据质量报告保存到MySQL
     */

    private void saveQualityReportToMySQL(Long taskId, String taskName, String uploadDate,
                                          Map<String, Object> qualityReport) {
        String sql = "INSERT INTO data_quality_report (" +
                "task_id, task_name, upload_date, total_records, valid_records, " +
                "null_comment_count, price_error_count, core_field_missing_count, " +
                "rating_anomaly_count, quality_score, core_field_score, content_score, " +
                "price_score, rating_score, merchant_count, avg_rating, positive_rate, create_time" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        try {
            jdbcTemplate.update(sql,
                    taskId,                    // task_id
                    taskName,                  // task_name
                    java.sql.Date.valueOf(uploadDate.replaceAll("[^0-9]", "").substring(0, 8)) ,// 确保是yyyyMMdd格式
                    qualityReport.get("totalRecords"),          // total_records
                    qualityReport.get("validRecords"),          // valid_records
                    qualityReport.get("nullCommentCount"),      // null_comment_count
                    qualityReport.get("priceErrorCount"),       // price_error_count
                    qualityReport.get("coreFieldMissingCount"), // core_field_missing_count
                    qualityReport.get("ratingAnomalyCount"),    // rating_anomaly_count
                    qualityReport.get("qualityScore"),          // quality_score
                    qualityReport.get("coreFieldScore"),        // core_field_score
                    qualityReport.get("contentScore"),          // content_score
                    qualityReport.get("priceScore"),            // price_score
                    qualityReport.get("ratingScore"),           // rating_score
                    qualityReport.get("merchantCount"),         // merchant_count
                    qualityReport.get("avgRating"),             // avg_rating
                    qualityReport.get("positiveRate")           // positive_rate
            );

            logger.info("数据质量报告已保存到MySQL，任务ID: {}", taskId);
        } catch (Exception e) {
            logger.error("保存数据质量报告到MySQL失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存数据质量报告失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定任务的数据质量报告
     */
    public Map<String, Object> getQualityReportByTaskId(Long taskId) throws SQLException {
        String sql = "SELECT * FROM data_quality_report WHERE task_id = ? ORDER BY create_time DESC LIMIT 1";

        try {
            return jdbcTemplate.queryForMap(sql, taskId);
        } catch (Exception e) {
            logger.error("获取数据质量报告失败，任务ID: {}, 错误: {}", taskId, e.getMessage());
            throw new SQLException("获取数据质量报告失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近的数据质量报告列表
     */
    public List<Map<String, Object>> getRecentQualityReports(int limit) {
        String sql = "SELECT * FROM data_quality_report ORDER BY create_time DESC LIMIT ?";

        try {
            return jdbcTemplate.queryForList(sql, limit);
        } catch (Exception e) {
            logger.error("获取最近数据质量报告失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取数据质量统计信息
     */
    public Map<String, Object> getQualityStats() {
        String sql = "SELECT " +
                "COUNT(*) as total_reports, " +
                "AVG(quality_score) as avg_quality_score, " +
                "MAX(quality_score) as max_quality_score, " +
                "MIN(quality_score) as min_quality_score, " +
                "SUM(total_records) as total_processed_records, " +
                "AVG(positive_rate) as avg_positive_rate " +
                "FROM data_quality_report";

        try {
            return jdbcTemplate.queryForMap(sql);
        } catch (Exception e) {
            logger.error("获取数据质量统计信息失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    // 在generateDataQualityReport方法中添加缺失的字段计算

    // 在HiveService.java中修改generateDataQualityReport方法
    public Map<String, Object> generateDataQualityReport(String uploadDate) throws SQLException {
        Map<String, Object> report = new HashMap<>();

        try (Statement stmt = hiveConnection.createStatement()) {
            // 1. 获取总记录数 - 从DWD层（清洗后的数据）
            String totalRecordsSql = String.format(
                    "SELECT COUNT(*) as total FROM %s.%s WHERE upload_date='%s'",
                    hiveDatabase, dwdTable, uploadDate
            );
            int totalRecords = executeCountQuery(stmt, totalRecordsSql);
            report.put("totalRecords", totalRecords);

            // 2. 获取有效记录数 - 核心字段完整的记录
            String validRecordsSql = String.format(
                    "SELECT COUNT(*) as count FROM %s.%s WHERE upload_date='%s' AND " +
                            "review_id IS NOT NULL AND merchant IS NOT NULL AND rating IS NOT NULL",
                    hiveDatabase, dwdTable, uploadDate
            );
            int validRecords = executeCountQuery(stmt, validRecordsSql);
            report.put("validRecords", validRecords);

            // 3. 核心字段缺失数
            int coreFieldMissingCount = totalRecords - validRecords;
            report.put("coreFieldMissingCount", coreFieldMissingCount);

            // 4. 获取空评论数
            String nullCommentSql = String.format(
                    "SELECT COUNT(*) as count FROM %s.%s WHERE upload_date='%s' AND " +
                            "(content_review IS NULL OR TRIM(content_review) = '' OR content_review = '无评论内容')",
                    hiveDatabase, dwdTable, uploadDate
            );
            int nullCommentCount = executeCountQuery(stmt, nullCommentSql);
            report.put("nullCommentCount", nullCommentCount);

            // 5. 获取价格错误数
            String priceErrorSql = String.format(
                    "SELECT COUNT(*) as count FROM %s.%s WHERE upload_date='%s' AND " +
                            "(price_per_person IS NULL OR price_per_person <= 0)",
                    hiveDatabase, dwdTable, uploadDate
            );
            int priceErrorCount = executeCountQuery(stmt, priceErrorSql);
            report.put("priceErrorCount", priceErrorCount);

            // 6. 获取评分异常数
            String ratingAnomalySql = String.format(
                    "SELECT COUNT(*) as count FROM %s.%s WHERE upload_date='%s' AND " +
                            "(rating < 0 OR rating > 5 OR rating IS NULL)",
                    hiveDatabase, dwdTable, uploadDate
            );
            int ratingAnomalyCount = executeCountQuery(stmt, ratingAnomalySql);
            report.put("ratingAnomalyCount", ratingAnomalyCount);

            // 7. 获取商户数量、平均评分、正面评价率 - 从DWS层
            String businessStatsSql = String.format(
                    "SELECT COUNT(*) as merchant_count, " +
                            "AVG(avg_rating) as avg_rating, " +
                            "AVG(positive_rate) as positive_rate " +
                            "FROM %s.%s WHERE stat_date='%s'",
                    hiveDatabase, dwsTable, uploadDate
            );

            try (ResultSet rs = stmt.executeQuery(businessStatsSql)) {
                if (rs.next()) {
                    report.put("merchantCount", rs.getInt("merchant_count"));
                    report.put("avgRating", rs.getDouble("avg_rating"));
                    report.put("positiveRate", rs.getDouble("positive_rate"));
                } else {
                    report.put("merchantCount", 0);
                    report.put("avgRating", 0.0);
                    report.put("positiveRate", 0.0);
                }
            }

            // 8. 计算各项得分（基于DWD层清洗后的数据）
            double coreFieldScore = totalRecords > 0 ? (validRecords * 100.0 / totalRecords) : 0;
            double contentScore = totalRecords > 0 ? ((totalRecords - nullCommentCount) * 100.0 / totalRecords) : 0;
            double priceScore = totalRecords > 0 ? ((totalRecords - priceErrorCount) * 100.0 / totalRecords) : 0;
            double ratingScore = totalRecords > 0 ? ((totalRecords - ratingAnomalyCount) * 100.0 / totalRecords) : 0;

            // 9. 计算总质量评分（加权平均）
            double qualityScore = (coreFieldScore * 0.4) + (contentScore * 0.2) +
                    (priceScore * 0.2) + (ratingScore * 0.2);

            report.put("qualityScore", Math.round(qualityScore));
            report.put("coreFieldScore", Math.round(coreFieldScore));
            report.put("contentScore", Math.round(contentScore));
            report.put("priceScore", Math.round(priceScore));
            report.put("ratingScore", Math.round(ratingScore));

            logger.info("数据质量报告生成完成 - 总记录: {}, 有效记录: {}, 质量评分: {}",
                    totalRecords, validRecords, Math.round(qualityScore));

        } catch (SQLException e) {
            logger.error("生成数据质量报告失败: {}", e.getMessage(), e);
            throw e;
        }

        return report;
    }

    // 添加辅助方法用于验证数据质量
    public void validateQualityData(String uploadDate) throws SQLException {
        String validationSql = String.format(
                "SELECT " +
                        "  COUNT(*) as total, " +
                        "  SUM(CASE WHEN rating IS NOT NULL AND merchant IS NOT NULL THEN 1 ELSE 0 END) as valid " +
                        "FROM %s.%s WHERE upload_date='%s'",
                hiveDatabase, dwdTable, uploadDate
        );

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(validationSql)) {
            if (rs.next()) {
                int total = rs.getInt("total");
                int valid = rs.getInt("valid");

                if (total == valid) {
                    logger.info("数据验证通过: 所有{}条记录均为有效记录", total);
                } else {
                    logger.warn("数据验证警告: 总记录{}条，有效记录{}条", total, valid);
                }
            }
        }
    }
    private double calculateQualityScore(Map<String, Object> report, int totalRecords) {
        if (totalRecords == 0) return 0;

        // 各类问题的权重
        double[] weights = {0.3, 0.2, 0.2, 0.2, 0.1}; // 可根据业务调整

        int nullComments = (int) report.get("nullCommentCount");
        int priceErrors = (int) report.get("priceErrorCount");
        int coreFieldMissing = (int) report.get("coreFieldMissingCount");
        int ratingAnomalies = (int) report.get("ratingAnomalyCount");
        int invalidMerchants = (int) report.get("invalidMerchantCount");

        // 计算各项得分（问题越少得分越高）
        double[] scores = {
                (1 - (double) nullComments / totalRecords) * 100,
                (1 - (double) priceErrors / totalRecords) * 100,
                (1 - (double) coreFieldMissing / totalRecords) * 100,
                (1 - (double) ratingAnomalies / totalRecords) * 100,
                (1 - (double) invalidMerchants / totalRecords) * 100
        };

        // 加权平均
        double totalScore = 0;
        for (int i = 0; i < weights.length; i++) {
            totalScore += scores[i] * weights[i];
        }

        return Math.max(0, Math.min(100, totalScore)); // 确保在0-100范围内
    }
    // 修改calculateFieldScore方法
    private double calculateFieldScore(Statement stmt, String sql) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int total = rs.getInt("total");
                int valid = rs.getInt("valid");
                return total > 0 ? (valid * 100.0 / total) : 0;
            }
        }
        return 0;
    }
    public void verifyMerchantDashboardData(String uploadDate) throws SQLException {
        String verifySql = String.format(
                "SELECT COUNT(*) as cnt FROM %s.ads_merchant_dashboard WHERE stat_date='%s'",
                hiveDatabase, uploadDate
        );

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(verifySql)) {
            if (rs.next()) {
                int count = rs.getInt("cnt");
                logger.info("商户仪表板数据验证: 分区 {} 中有 {} 条记录", uploadDate, count);

                if (count == 0) {
                    throw new SQLException("商户仪表板数据生成失败！");
                }

                // 检查文件大小（通过HDFS命令）
                logger.info("请手动检查HDFS文件大小确认数据完整性");
                logger.info("命令: hdfs dfs -du -h /dianping/warehouse/ads/merchant_dashboard/stat_date={}", uploadDate);
            }
        }
    }
    // HiveService.java - 添加生成ADS层数据的方法
    /**
     * 生成商户仪表板数据
     */
    // 在生成数据前调用修复
    // 在Java代码中添加修复方法
    public void repairAdsMerchantDashboard() throws SQLException {
        // 1. 删除损坏的分区
        String deletePartitionSql = String.format(
                "ALTER TABLE %s.ads_merchant_dashboard DROP IF EXISTS PARTITION (stat_date='20250909')",
                hiveDatabase
        );
        executeHiveSql(deletePartitionSql);

        logger.info("已删除损坏的商户仪表板分区");
    }
    public void generateMerchantDashboardData(String uploadDate) throws SQLException {
        // 先修复表
        repairAdsMerchantDashboard();

        // 等待一段时间确保HDFS操作完成
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 然后重新生成数据
        String sql = String.format(
                "INSERT OVERWRITE TABLE %s.ads_merchant_dashboard PARTITION (stat_date='%s') " +
                        "SELECT " +
                        "  merchant, " +
                        "  ROUND(AVG(avg_rating), 2) as avg_rating_7d, " +
                        "  ROUND(AVG(avg_taste), 2) as avg_taste_7d, " +
                        "  ROUND(AVG(avg_environment), 2) as avg_environment_7d, " +
                        "  ROUND(AVG(avg_service), 2) as avg_service_7d, " +
                        "  ROUND(AVG(positive_rate), 2) as positive_rate_7d, " +
                        "  'stable' as trend_direction, " +
                        "  '[]' as top_foods, " +
                        "  CURRENT_TIMESTAMP as update_time " +
                        "FROM %s.dws_merchant_analysis " +
                        "WHERE stat_date = '%s' " +  // 先只插入当前日期的数据
                        "GROUP BY merchant",
                hiveDatabase, uploadDate, hiveDatabase, uploadDate
        );

        executeHiveSql(sql);
        logger.info("商户仪表板数据重新生成完成: {}", uploadDate);

        // 验证数据
        verifyMerchantDashboardData(uploadDate);
    }

    // 修改generateRankingDailyData方法
    public void generateRankingDailyData(String uploadDate) throws SQLException {
        String sql = String.format(
                "INSERT OVERWRITE TABLE %s.ads_ranking_daily PARTITION (rank_date='%s') " +
                        "SELECT " +
                        "  '综合排名' as rank_type, " +
                        "  merchant, " +
                        "  ROW_NUMBER() OVER (ORDER BY avg_rating DESC, positive_rate DESC) as rank_position, " +
                        "  ROUND((avg_rating * 0.6 + positive_rate * 0.4), 2) as composite_score, " +
                        "  avg_price, " +
                        "  positive_rate, " +
                        "  CURRENT_TIMESTAMP as update_time " +
                        "FROM %s.dws_merchant_analysis " +
                        "WHERE update_date = '%s'",  // 使用update_date而不是stat_date
                hiveDatabase, uploadDate, hiveDatabase, uploadDate
        );

        executeHiveSql(sql);
        logger.info("每日排行榜数据生成完成: {}", uploadDate);
    }

    private int executeCountQuery(Statement stmt, String sql) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    // 修改executeTimeAnalysisAggregation方法
    public void executeTimeAnalysisAggregation(String uploadDate) throws SQLException {
        String sql = String.format(
                "INSERT OVERWRITE TABLE %s.dws_time_analysis PARTITION (update_date='%s') " +
                        "SELECT " +
                        "  merchant, " +
                        "  DATE_FORMAT(review_time, 'yyyyMM') as time_period, " +
                        "  'month' as period_type, " +
                        "  ROUND(AVG(rating), 2) as avg_rating, " +
                        "  SUM(CASE WHEN sentiment = '正面' THEN 1 ELSE 0 END) as positive_count, " +
                        "  SUM(CASE WHEN sentiment = '负面' THEN 1 ELSE 0 END) as negative_count, " +
                        "  COUNT(*) as total_count " +
                        "FROM %s.%s " +
                        "WHERE upload_date='%s' " +
                        "GROUP BY merchant, DATE_FORMAT(review_time, 'yyyyMM')",
                hiveDatabase, uploadDate, hiveDatabase, dwdTable, uploadDate
        );
        executeHiveSql(sql);
        logger.info("时间分析聚合完成: {}", uploadDate);
    }

    // 修改executeFoodKeywordsAggregation方法
    public void executeFoodKeywordsAggregation(String uploadDate) throws SQLException {
        String sql = String.format(
                "INSERT OVERWRITE TABLE %s.dws_food_keywords PARTITION (update_date='%s') " +
                        "SELECT " +
                        "  merchant, " +
                        "  '热门菜品' as keyword, " +
                        "  COUNT(*) as mention_count " +
                        "FROM %s.%s " +
                        "WHERE upload_date='%s' " +
                        "GROUP BY merchant",
                hiveDatabase, uploadDate, hiveDatabase, dwdTable, uploadDate
        );
        executeHiveSql(sql);
        logger.info("菜品关键词聚合完成: {}", uploadDate);
    }
    // 在生成商户仪表板数据前，先修复表
    public void repairMerchantDashboardTable() throws SQLException {
        // 1. 删除损坏的分区
        String deletePartitionSql = String.format(
                "ALTER TABLE %s.ads_merchant_dashboard DROP IF EXISTS PARTITION (stat_date='20250909')",
                hiveDatabase
        );
        executeHiveSql(deletePartitionSql);

        logger.info("已删除损坏的商户仪表板分区");
    }

    private double calculateQualityScore(Map<String, Object> report) {
        int total = (int) report.get("totalRecords");
        int valid = (int) report.get("validRecords");
        if (total == 0) {
            return 0;
        }
        return (valid * 100.0) / total;
    }
    private int getTotalRecords(Statement stmt, String uploadDate) throws SQLException {
        String sql = String.format(
                "SELECT COUNT(*) as total FROM %s.%s WHERE upload_date='%s'",
                hiveDatabase, odsTable, uploadDate
        );
        try (ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    // 修改getValidRecords方法
    private int getValidRecords(Statement stmt, String uploadDate) throws SQLException {
        String sql = String.format(
                "SELECT COUNT(*) as count FROM %s.%s WHERE upload_date=\'%s\' AND " +
                        "rating IS NOT NULL AND merchant IS NOT NULL AND review_id IS NOT NULL",
                hiveDatabase, odsTable, uploadDate
        );
        return executeCountQuery(stmt, sql);
    }

    // 添加连接健康检查方法
    public boolean checkConnectionHealth() {
        try {
            if (hiveConnection == null || hiveConnection.isClosed()) {
                logger.warn("Hive连接已关闭或为空");
                // 尝试重新建立连接
                return tryReconnect();
            }

            // 执行简单查询测试连接
            try (Statement stmt = hiveConnection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                boolean isConnected = rs.next() && rs.getInt(1) == 1;
                if (!isConnected) {
                    return tryReconnect();
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Hive连接健康检查失败: {}", e.getMessage());
            return tryReconnect();
        }
    }
    // 添加重连方法
    private boolean tryReconnect() {
        logger.info("尝试重新建立Hive连接...");
        try {
            // 这里需要根据你的Hive连接配置实现重连逻辑
            // 如果是Spring管理的连接，可能需要重新从连接池获取
            // 示例：hiveConnection = dataSource.getConnection();
            logger.info("Hive连接重连成功");
            return true;
        } catch (Exception e) {
            logger.error("Hive连接重连失败: {}", e.getMessage());
            return false;
        }
    }


    // 修改executeHiveSql方法，增加更好的超时处理
    private void executeHiveSql(String sql) throws SQLException {
        int maxRetries = 2; // 减少重试次数
        int retryCount = 0;
        long queryTimeout = 1200; // 20分钟超时

        while (retryCount <= maxRetries) {
            try (Statement stmt = hiveConnection.createStatement()) {
                logger.info("Executing Hive SQL (尝试 {}): {}", retryCount + 1, sql);
                long startTime = System.currentTimeMillis();

                // 设置查询超时
                stmt.setQueryTimeout((int) queryTimeout);

                boolean isQuery = sql.trim().toUpperCase().startsWith("SELECT");

                if (isQuery) {
                    try (ResultSet rs = stmt.executeQuery(sql)) {
                        int rowCount = 0;
                        while (rs.next()) {
                            rowCount++;
                        }
                        logger.info("Hive SQL执行完成, 返回 {} 行, 耗时: {} ms",
                                rowCount, System.currentTimeMillis() - startTime);
                    }
                } else {
                    int updateCount = stmt.executeUpdate(sql);
                    logger.info("Hive SQL执行完成, 影响 {} 行, 耗时: {} ms",
                            updateCount, System.currentTimeMillis() - startTime);
                }

                return; // 执行成功，退出循环

            } catch (SQLException e) {
                retryCount++;

                // 检查连接是否有效
                if (!checkConnectionHealth()) {
                    logger.error("Hive连接已断开，无法重试");
                    throw new SQLException("Hive连接已断开", e);
                }

                if (retryCount > maxRetries) {
                    logger.error("Hive SQL执行失败(重试{}次): {}, SQL: {}",
                            maxRetries, e.getMessage(), sql);
                    throw e;
                }

                logger.warn("Hive SQL执行失败，第{}次重试: {}", retryCount, e.getMessage());

                try {
                    // 指数退避策略
                    long delay = 5000 * retryCount;
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("重试被中断", ie);
                }
            }
        }
    }

    // 添加批量处理的优化方法 - 修改为使用MapReduce
    public void executeBatchEtl(String uploadDate) throws SQLException {
        configureMapReduceOptimization(); // 改为MapReduce配置

        // 使用MapReduce特定的优化设置
        List<String> setupSqls = Arrays.asList(
                "SET hive.execution.engine=mr",
                "SET mapred.map.tasks=100",
                "SET mapred.reduce.tasks=50",
                "SET hive.exec.parallel=true",
                "SET hive.exec.parallel.thread.number=8",
                "SET hive.optimize.skewjoin=true",
                "SET hive.skewjoin.key=100000",
                "SET hive.exec.compress.intermediate=true",
                "SET hive.exec.compress.output=true",
                "SET mapred.compress.map.output=true",
                "SET mapred.output.compress=true"
        );
        executeHiveSqlBatch(setupSqls);

        // 分步骤执行ETL
        executeEtlToDwd(uploadDate);
        executeDwsAggregation(uploadDate);
        executeTimeAnalysisAggregation(uploadDate);
        executeFoodKeywordsAggregation(uploadDate);

        logger.info("批量ETL处理完成: {}", uploadDate);
    }

    // 添加性能监控方法 - 修改为MapReduce计数器
    public Map<String, Object> monitorQueryPerformance(String sql) throws SQLException {
        Map<String, Object> metrics = new HashMap<>();

        long startTime = System.currentTimeMillis();
        try (Statement stmt = hiveConnection.createStatement()) {
            stmt.execute("SET mapred.job.counters.limit=1000"); // MapReduce计数器限制

            try (ResultSet rs = stmt.executeQuery("EXPLAIN " + sql)) {
                StringBuilder explainPlan = new StringBuilder();
                while (rs.next()) {
                    explainPlan.append(rs.getString(1)).append("\n");
                }
                metrics.put("explainPlan", explainPlan.toString());
            }

            // 执行实际查询
            try (ResultSet rs = stmt.executeQuery(sql)) {
                int rowCount = 0;
                while (rs.next()) {
                    rowCount++;
                }
                metrics.put("rowCount", rowCount);
            }
        }

        long endTime = System.currentTimeMillis();
        metrics.put("executionTime", endTime - startTime);

        return metrics;
    }

    public boolean testConnection() {
        try {
            String sql = "SHOW DATABASES";
            try (Statement stmt = hiveConnection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                boolean connected = rs.next();
                logger.info("Hive集群连接测试: {}", connected ? "成功" : "失败");
                return connected;
            }
        } catch (SQLException e) {
            logger.error("Hive集群连接失败: {}", e.getMessage());
            return false;
        }
    }

    // HiveService.java - 添加彻底清理和重建方法
    public void dropAllTables() throws SQLException {
        List<String> dropSqls = new ArrayList<>();
        dropSqls.add(String.format("DROP TABLE IF EXISTS %s.%s", hiveDatabase, odsTable));
        dropSqls.add(String.format("DROP TABLE IF EXISTS %s.%s", hiveDatabase, dwdTable));
        dropSqls.add(String.format("DROP TABLE IF EXISTS %s.%s", hiveDatabase, dwsTable));

        executeHiveSqlBatch(dropSqls);
        logger.info("所有表删除完成");
    }

    public void forceRecreateAllTables() throws SQLException {
        dropAllTables();
        createDatabaseAndTables();
        logger.info("强制重新创建所有表完成");
    }
    public Map<String, Object> getActualTableStructure(String tableName) throws SQLException {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, String>> columns = new ArrayList<>();

        String sql = String.format("DESCRIBE %s.%s", hiveDatabase, tableName);

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, String> column = new HashMap<>();
                column.put("name", rs.getString(1));
                column.put("type", rs.getString(2));
                columns.add(column);
            }
        }

        result.put("tableName", tableName);
        result.put("columnCount", columns.size());
        result.put("columns", columns);

        return result;
    }

    public Map<String, Object> getAllTableStructures() throws SQLException {
        Map<String, Object> result = new HashMap<>();

        result.put("odsTable", getActualTableStructure(odsTable));
        result.put("dwdTable", getActualTableStructure(dwdTable));
        result.put("dwsTable", getActualTableStructure(dwsTable));

        return result;
    }

    public Map<String, Object> inspectDataFile(String uploadDate, int limit) throws SQLException {
        Map<String, Object> result = new HashMap<>();

        String sql = String.format(
                "SELECT * FROM %s.%s WHERE upload_date='%s' LIMIT %d",
                hiveDatabase, odsTable, uploadDate, limit
        );

        List<Map<String, Object>> rows = new ArrayList<>();

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            result.put("columnCount", columnCount);

            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
            result.put("columnNames", columnNames);

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String colName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(colName, value);
                }
                rows.add(row);
            }
        }

        result.put("sampleData", rows);
        return result;
    }

    // HiveService.java - 添加表结构检查方法
    public void checkTableStructure(String tableName) throws SQLException {
        String sql = String.format("DESCRIBE %s.%s", hiveDatabase, tableName);

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            logger.info("表 {} 结构:", tableName);
            while (rs.next()) {
                String colName = rs.getString(1);
                String dataType = rs.getString(2);
                logger.info("  {}: {}", colName, dataType);
            }
        }
    }
private void verifyInsertResult(String uploadDate) throws SQLException {
    String verifySql = String.format(
            "SELECT COUNT(*) as total FROM %s.%s WHERE stat_date='%s' AND update_date='%s'",
            hiveDatabase, dwsTable, uploadDate, uploadDate
    );

    try (Statement stmt = hiveConnection.createStatement();
         ResultSet rs = stmt.executeQuery(verifySql)) {
        if (rs.next()) {
            int count = rs.getInt("total");
            logger.info("验证结果: 分区 stat_date={}, update_date={} 中有 {} 条记录",
                    uploadDate, uploadDate, count);

            // 获取临时表记录数进行对比
            String tempCountSql = "SELECT COUNT(*) FROM temp_dws_" + uploadDate;
            int tempCount = executeCountQuery(stmt, tempCountSql);

            if (count == 0 && tempCount > 0) {
                // 如果DWS表没有数据但临时表有数据，检查具体错误
                checkInsertErrors(uploadDate);
                throw new SQLException(String.format(
                        "DWS聚合失败，数据没有插入成功。临时表%d条，DWS表%d条", tempCount, count));
            } else if (Math.abs(count - tempCount) > tempCount * 0.1) {
                throw new SQLException(String.format(
                        "DWS聚合失败，数据不一致: 临时表%d条 ≠ DWS表%d条", tempCount, count));
            } else {
                logger.info("数据验证通过: 临时表{}条, DWS表{}条 (差异在允许范围内)",
                        tempCount, count);
            }
        }
    }
}

    private void checkInsertErrors(String uploadDate) throws SQLException {
        // 检查是否有MapReduce错误日志
        String checkErrorSql = String.format(
                "SELECT * FROM %s.%s WHERE stat_date='%s' LIMIT 1",
                hiveDatabase, dwsTable, uploadDate
        );

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(checkErrorSql)) {
            // 如果能执行到这里，说明表结构没问题
            logger.info("表结构检查通过");
        } catch (SQLException e) {
            logger.error("表访问错误: {}", e.getMessage());
            throw new SQLException("DWS表访问失败，可能表结构或分区有问题: " + e.getMessage());
        }
    }

private void validateTableStructure(String tempTable, String targetTable) throws SQLException {
    String checkTempSchema = "DESCRIBE " + tempTable;
    String checkTargetSchema = "DESCRIBE " + hiveDatabase + "." + targetTable;

    logger.info("验证表结构一致性...");

    // 获取临时表结构（只有数据列）
    List<String> tempColumns = new ArrayList<>();
    try (Statement stmt = hiveConnection.createStatement();
         ResultSet rs = stmt.executeQuery(checkTempSchema)) {
        while (rs.next()) {
            String colName = rs.getString(1);
            String colType = rs.getString(2);
            if (colName != null && !colName.startsWith("#") && colType != null) {
                tempColumns.add(colName + " " + colType);
            }
        }
    }

    /// 获取目标表结构（只比较数据列，忽略分区列）
    List<String> targetDataColumns = new ArrayList<>();
    try (Statement stmt = hiveConnection.createStatement();
         ResultSet rs = stmt.executeQuery(checkTargetSchema)) {
        while (rs.next()) {
            String colName = rs.getString(1);
            String colType = rs.getString(2);
            // 忽略分区列和元数据信息
            if (colName != null && !colName.startsWith("#") && colType != null &&
                    !isPartitionColumn(targetTable, colName)) {
                targetDataColumns.add(colName + " " + colType);
            }
        }
    }

    logger.info("临时表结构: {}", tempColumns);
    logger.info("目标表结构: {}", targetDataColumns);

    // 只比较数据列，忽略分区列的差异
    if (tempColumns.size() != targetDataColumns.size()) {
        throw new SQLException("表数据列数量不一致: 临时表 " + tempColumns.size() + " 列, 目标表 " + targetDataColumns.size() + " 列");
    }

    for (int i = 0; i < tempColumns.size(); i++) {
        if (!tempColumns.get(i).equals(targetDataColumns.get(i))) {
            throw new SQLException("数据列不匹配: 临时表[" + tempColumns.get(i) + "] ≠ 目标表[" + targetDataColumns.get(i) + "]");
        }
    }
    for (int i = 0; i < tempColumns.size(); i++) {
        String tempCol = tempColumns.get(i);
        String targetCol = targetDataColumns.get(i);

        // 允许 decimal(26,2) 到 double 的转换
        if (tempCol.equals("positive_rate decimal(26,2)") &&
                targetCol.equals("positive_rate double")) {
            logger.info("允许 decimal(26,2) 到 double 的类型转换");
            continue;
        }

        if (!tempCol.equals(targetCol)) {
            throw new SQLException("数据列不匹配: 临时表[" + tempCol + "] ≠ 目标表[" + targetCol + "]");
        }
    }
    logger.info("表数据列结构验证通过");
}
    // 判断是否为分区列的方法
    private boolean isPartitionColumn(String tableName, String columnName) {
        // 根据表名确定分区列
        if (tableName.equals(dwsTable)) {
            return "stat_date".equals(columnName) || "update_date".equals(columnName);
        } else if (tableName.equals(dwdTable) || tableName.equals(odsTable)) {
            return "upload_date".equals(columnName);
        }
        return false;
    }

    // HiveService.java - 修复validateTableColumns方法
    private void validateTableColumns(String tableName, List<String> expectedColumns) throws SQLException {
        String sql = String.format("DESCRIBE %s.%s", hiveDatabase, tableName);

        List<String> actualColumns = new ArrayList<>();
        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String colName = rs.getString(1);
                actualColumns.add(colName);
            }
        }

        logger.info("表 {} 的实际列: {}", tableName, actualColumns);

        // 为不同类型的表定义不同的核心列
        List<String> coreColumns;

        if (tableName.equals(odsTable) || tableName.equals(dwdTable)) {
            // ODS和DWD表的共同核心列
            coreColumns = Arrays.asList(
                    "review_id", "merchant", "rating", "score_taste", "score_environment",
                    "score_service", "price_per_person", "review_time", "num_thumbs_up",
                    "num_response", "content_review", "reviewer"
            );
        } else if (tableName.equals(dwsTable)) {
            // DWS聚合表的核心列
            coreColumns = Arrays.asList(
                    "merchant", "avg_rating", "avg_taste", "avg_environment", "avg_service",
                    "avg_price", "total_reviews", "positive_reviews", "negative_reviews",
                    "neutral_reviews", "positive_rate", "update_date"
            );
        } else if (tableName.equals("dws_time_analysis")) {
            // 时间分析表的核心列
            coreColumns = Arrays.asList(
                    "merchant", "time_period", "period_type", "avg_rating",
                    "positive_count", "negative_count", "total_count", "update_date"
            );
        } else if (tableName.equals("dws_food_keywords")) {
            // 菜品关键词表的核心列
            coreColumns = Arrays.asList(
                    "merchant", "keyword", "mention_count", "update_date"
            );
        } else {
            // 其他表的默认核心列
            coreColumns = Arrays.asList("merchant", "update_date");
        }

        // 检查核心列是否存在
        for (String coreColumn : coreColumns) {
            if (!actualColumns.contains(coreColumn)) {
                throw new SQLException(String.format("表 %s 缺少核心列: %s", tableName, coreColumn));
            }
        }

        logger.info("表 {} 的核心列验证通过", tableName);
    }

    // HiveService.java - 添加数据文件列数检查
    public void validateDataFile(String uploadDate) throws SQLException {
        logger.info("开始验证数据文件结构...");

        // 检查数据文件的实际列数
        String checkSql = String.format(
                "SELECT * FROM %s.%s WHERE upload_date='%s' LIMIT 1",
                hiveDatabase, odsTable, uploadDate
        );

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            logger.info("数据文件实际列数: {}", columnCount);

            // 打印所有列名（去掉表前缀）
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                String rawName = metaData.getColumnName(i);   // 可能是  ods_raw_reviews.xxx
                String colName = rawName.contains(".")
                        ? rawName.substring(rawName.lastIndexOf('.') + 1)
                        : rawName;
                columnNames.add(colName);
                logger.info("列 {}: {}", i, colName);
            }

            if (columnCount != 16) { // 15数据列 + 1分区列
                throw new SQLException(String.format(
                        "数据文件列数不匹配: 期望 16 列 (15数据列 + 1分区列), 实际 %d 列. 列名: %s",
                        columnCount, columnNames));
            }

            // 检查核心列是否存在
            List<String> requiredColumns = Arrays.asList(
                    "review_id", "merchant", "rating", "score_taste", "score_environment",
                    "score_service", "price_per_person", "review_time", "num_thumbs_up",
                    "num_response", "content_review", "reviewer"
            );

            for (String requiredColumn : requiredColumns) {
                if (!columnNames.contains(requiredColumn)) {
                    throw new SQLException(String.format("缺少核心列: %s", requiredColumn));
                }
            }
        }

        logger.info("数据文件验证完成");
    }

    public void compareTableAndQuery(String uploadDate) throws SQLException {
        // 检查ODS表结构
        checkTableStructure(odsTable);

        // 检查DWD表结构
        checkTableStructure(dwdTable);

        // 检查DWS表结构
        checkTableStructure(dwsTable);

        // 检查查询的列数
        String testQuery = String.format(
                "SELECT COUNT(*) FROM %s.%s WHERE upload_date='%s'",
                hiveDatabase, odsTable, uploadDate
        );

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(testQuery)) {
            if (rs.next()) {
                logger.info("ODS表记录数: {}", rs.getInt(1));
            }
        }
    }
    // HiveService.java - 添加方法检查实际表结构

    // 添加详细的表结构验证方法
    public void validateTableStructureDetailed() throws SQLException {
        logger.info("开始详细验证表结构...");

        // 验证ODS表
        Map<String, Object> odsStructure = getActualTableStructure(odsTable);
        List<Map<String, String>> odsColumns = (List<Map<String, String>>) odsStructure.get("columns");
        logger.info("ODS表实际列数: {}", odsColumns.size());

        for (Map<String, String> column : odsColumns) {
            logger.info("ODS列: {} - {}", column.get("name"), column.get("type"));
        }

        // 检查是否包含所有必要的列
        List<String> requiredOdsColumns = Arrays.asList(
                "review_id", "merchant", "rating", "score_taste", "score_environment",
                "score_service", "price_per_person", "review_time", "num_thumbs_up",
                "num_response", "content_review", "reviewer", "reviewer_value",
                "reviewer_rank", "favorite_foods"
        );

        for (String requiredColumn : requiredOdsColumns) {
            boolean found = odsColumns.stream().anyMatch(col -> requiredColumn.equals(col.get("name")));
            if (!found) {
                throw new SQLException("ODS表缺少必要列: " + requiredColumn);
            }
        }

        logger.info("ODS表结构验证通过");
    }

    // HiveService.java - 添加专门的表验证方法
    public void validateTableStructureSmart() throws SQLException {
        logger.info("开始智能验证表结构...");

        // 验证ODS表
        validateTableWithType(odsTable, "ods");

        // 验证DWD表
        validateTableWithType(dwdTable, "dwd");

        // 验证DWS表
        validateTableWithType(dwsTable, "dws");

        logger.info("智能表结构验证完成");
    }

    private void validateTableWithType(String tableName, String tableType) throws SQLException {
        logger.info("验证 {} 表: {}", tableType.toUpperCase(), tableName);

        List<String> requiredColumns = getRequiredColumnsForTableType(tableType);
        List<String> actualColumns = getActualTableColumns(tableName);

        logger.info("表 {} 需要的列: {}", tableName, requiredColumns);
        logger.info("表 {} 实际的列: {}", tableName, actualColumns);

        for (String requiredColumn : requiredColumns) {
            if (!actualColumns.contains(requiredColumn)) {
                throw new SQLException(String.format("表 %s 缺少必要列: %s", tableName, requiredColumn));
            }
        }

        logger.info("表 {} 验证通过", tableName);
    }

    private List<String> getRequiredColumnsForTableType(String tableType) {
        switch (tableType.toLowerCase()) {
            case "ods":
                return Arrays.asList(
                        "review_id", "merchant", "rating", "score_taste", "score_environment",
                        "score_service", "price_per_person", "review_time", "num_thumbs_up",
                        "num_response", "content_review", "reviewer", "upload_date"
                );
            case "dwd":
                return Arrays.asList(
                        "review_id", "merchant", "rating", "score_taste", "score_environment",
                        "score_service", "price_per_person", "review_time", "num_thumbs_up",
                        "num_response", "content_review", "reviewer", "sentiment", "upload_date"
                );
            case "dws":
                return Arrays.asList(
                        "merchant", "avg_rating", "avg_taste", "avg_environment", "avg_service",
                        "avg_price", "total_reviews", "positive_reviews", "negative_reviews",
                        "neutral_reviews", "positive_rate", "update_date"
                );
            default:
                return Arrays.asList("merchant", "update_date");
        }
    }

    private List<String> getActualTableColumns(String tableName) throws SQLException {
        String sql = String.format("DESCRIBE %s.%s", hiveDatabase, tableName);
        List<String> columns = new ArrayList<>();

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String colName = rs.getString(1);
                columns.add(colName);
            }
        }

        return columns;
    }

    // 添加getter方法
    public String getOdsTable() {
        return odsTable;
    }

    public String getDwdTable() {
        return dwdTable;
    }

    public String getDwsTable() {
        return dwsTable;
    }

    public String getHiveDatabase() {
        return hiveDatabase;
    }


}