-- 创建数据库
CREATE DATABASE IF NOT EXISTS dianping;

USE dianping;

-- ODS层表 - 原始评论数据
CREATE EXTERNAL TABLE IF NOT EXISTS ods_raw_reviews (
    review_id INT,
    merchant STRING,
    rating INT,
    score_taste INT,
    score_environment INT,
    score_service INT,
    price_per_person STRING,
    review_time STRING,
    num_thumbs_up INT,
    num_response INT,
    content_review STRING,
    reviewer STRING,
    reviewer_value INT,
    reviewer_rank INT,
    favorite_foods STRING
)
PARTITIONED BY (upload_date STRING)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/dianping/ods'
TBLPROPERTIES ('skip.header.line.count'='1');

-- DWD层表 - 清洗后的明细数据
CREATE EXTERNAL TABLE IF NOT EXISTS dwd_cleaned_reviews (
    review_id INT,
    merchant STRING,
    rating INT,
    score_taste INT,
    score_environment INT,
    score_service INT,
    price_per_person DOUBLE,
    review_time TIMESTAMP,
    num_thumbs_up INT,
    num_response INT,
    content_review STRING,
    reviewer STRING,
    reviewer_value INT,
    reviewer_rank INT,
    favorite_foods STRING,
    sentiment STRING
)
PARTITIONED BY (upload_date STRING)
STORED AS PARQUET
LOCATION '/user/hive/warehouse/dianping/dwd'
TBLPROPERTIES ('parquet.compression'='SNAPPY');

-- DWS层表 - 商户分析汇总
CREATE EXTERNAL TABLE IF NOT EXISTS dws_merchant_analysis (
    merchant STRING,
    avg_rating DOUBLE,
    avg_taste DOUBLE,
    avg_environment DOUBLE,
    avg_service DOUBLE,
    avg_price DOUBLE,
    total_reviews INT,
    positive_reviews INT,
    negative_reviews INT,
    neutral_reviews INT,
    positive_rate DOUBLE,
    update_date STRING
)
STORED AS PARQUET
LOCATION '/user/hive/warehouse/dianping/dws'
TBLPROPERTIES ('parquet.compression'='SNAPPY');

-- 创建数据质量监控表
CREATE TABLE IF NOT EXISTS data_quality_monitor (
    process_date STRING,
    total_records INT,
    valid_records INT,
    null_count INT,
    price_error_count INT,
    quality_score DOUBLE
)
STORED AS ORC;