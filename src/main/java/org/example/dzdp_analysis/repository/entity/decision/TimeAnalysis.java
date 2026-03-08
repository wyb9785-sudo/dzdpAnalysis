package org.example.dzdp_analysis.repository.entity.decision;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "time_analysis")
public class TimeAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "time_period")
    private String timePeriod;

    @Column(name = "period_type")
    private String periodType;

    @Column(name = "avg_rating")
    private Double avgRating;

    @Column(name = "positive_count")
    private Integer positiveCount;

    @Column(name = "negative_count")
    private Integer negativeCount;

    @Column(name = "total_count")
    private Integer totalCount;

    @Column(name = "update_date")
    private String updateDate;

    @Column(name = "create_time")
    private Date createTime;

    // Getter和Setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getTimePeriod() { return timePeriod; }
    public void setTimePeriod(String timePeriod) { this.timePeriod = timePeriod; }

    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }

    public Double getAvgRating() { return avgRating; }
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }

    public Integer getPositiveCount() { return positiveCount; }
    public void setPositiveCount(Integer positiveCount) { this.positiveCount = positiveCount; }

    public Integer getNegativeCount() { return negativeCount; }
    public void setNegativeCount(Integer negativeCount) { this.negativeCount = negativeCount; }

    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }

    public String getUpdateDate() { return updateDate; }
    public void setUpdateDate(String updateDate) { this.updateDate = updateDate; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}