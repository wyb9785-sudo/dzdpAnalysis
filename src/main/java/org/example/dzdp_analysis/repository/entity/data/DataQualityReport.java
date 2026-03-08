package org.example.dzdp_analysis.repository.entity.data;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "data_quality_report")
@Data
public class DataQualityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "task_name")
    private String taskName;

    @Column(name = "upload_date")
    private String uploadDate;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "valid_records")
    private Integer validRecords;

    @Column(name = "null_comment_count")
    private Integer nullCommentCount;

    @Column(name = "price_error_count")
    private Integer priceErrorCount;

    @Column(name = "core_field_missing_count")
    private Integer coreFieldMissingCount;

    @Column(name = "rating_anomaly_count")
    private Integer ratingAnomalyCount;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "core_field_score")
    private Double coreFieldScore;

    @Column(name = "content_score")
    private Double contentScore;

    @Column(name = "price_score")
    private Double priceScore;

    @Column(name = "rating_score")
    private Double ratingScore;

    @Column(name = "merchant_count")
    private Integer merchantCount;

    @Column(name = "avg_rating")
    private Double avgRating;

    @Column(name = "positive_rate")
    private Double positiveRate;

    @Column(name = "create_time")
    private LocalDateTime createTime;
    // 计算其他异常数量（空评论数 + 价格格式错误数）
    @Transient
    public Integer getOtherErrorCount() {
        return (nullCommentCount != null ? nullCommentCount : 0) +
                (priceErrorCount != null ? priceErrorCount : 0);
    }

    // 计算有效数据百分比
    @Transient
    public Double getValidRecordsPercentage() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }
        return (validRecords != null ? validRecords : 0) * 100.0 / totalRecords;
    }

    // 构造函数
    public DataQualityReport() {
        this.createTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public Integer getValidRecords() {
        return validRecords;
    }

    public Integer getNullCommentCount() {
        return nullCommentCount;
    }

    public Integer getPriceErrorCount() {
        return priceErrorCount;
    }

    public Integer getCoreFieldMissingCount() {
        return coreFieldMissingCount;
    }

    public Integer getRatingAnomalyCount() {
        return ratingAnomalyCount;
    }

    public Double getQualityScore() {
        return qualityScore;
    }

    public Double getCoreFieldScore() {
        return coreFieldScore;
    }

    public Double getContentScore() {
        return contentScore;
    }

    public Double getPriceScore() {
        return priceScore;
    }

    public Double getRatingScore() {
        return ratingScore;
    }

    public Integer getMerchantCount() {
        return merchantCount;
    }

    public Double getAvgRating() {
        return avgRating;
    }

    public Double getPositiveRate() {
        return positiveRate;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public void setValidRecords(Integer validRecords) {
        this.validRecords = validRecords;
    }

    public void setNullCommentCount(Integer nullCommentCount) {
        this.nullCommentCount = nullCommentCount;
    }

    public void setPriceErrorCount(Integer priceErrorCount) {
        this.priceErrorCount = priceErrorCount;
    }

    public void setCoreFieldMissingCount(Integer coreFieldMissingCount) {
        this.coreFieldMissingCount = coreFieldMissingCount;
    }

    public void setRatingAnomalyCount(Integer ratingAnomalyCount) {
        this.ratingAnomalyCount = ratingAnomalyCount;
    }

    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public void setCoreFieldScore(Double coreFieldScore) {
        this.coreFieldScore = coreFieldScore;
    }

    public void setContentScore(Double contentScore) {
        this.contentScore = contentScore;
    }

    public void setPriceScore(Double priceScore) {
        this.priceScore = priceScore;
    }

    public void setRatingScore(Double ratingScore) {
        this.ratingScore = ratingScore;
    }

    public void setMerchantCount(Integer merchantCount) {
        this.merchantCount = merchantCount;
    }

    public void setAvgRating(Double avgRating) {
        this.avgRating = avgRating;
    }

    public void setPositiveRate(Double positiveRate) {
        this.positiveRate = positiveRate;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "DataQualityReport{" +
                "id=" + id +
                ", uploadDate='" + uploadDate + '\'' +
                ", taskId=" + taskId +
                ", totalRecords=" + totalRecords +
                ", validRecords=" + validRecords +
                ", nullCommentCount=" + nullCommentCount +
                ", priceErrorCount=" + priceErrorCount +
                ", qualityScore=" + qualityScore +
                ", createTime=" + createTime +
                '}';
        }
}

