package org.example.dzdp_analysis.repository.entity.analysis;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "merchant_analysis")
public class MerchantAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_name")
    private String merchantName;
    @Column(name = "stat_date") // 添加stat_date字段
    private String statDate;
    @Column(name = "avg_rating")
    private Double avgRating;

    @Column(name = "avg_taste")
    private Double avgTaste;

    @Column(name = "avg_environment")
    private Double avgEnvironment;

    @Column(name = "avg_service")
    private Double avgService;

    @Column(name = "avg_price")
    private Double avgPrice;

    @Column(name = "total_reviews")
    private Integer totalReviews;

    @Column(name = "positive_reviews")
    private Integer positiveReviews;

    @Column(name = "negative_reviews")
    private Integer negativeReviews;

    @Column(name = "neutral_reviews")
    private Integer neutralReviews;

    @Column(name = "positive_rate")
    private Double positiveRate;

    @Column(name = "update_date")
    private String updateDate;

    public void setStatDate(String statDate) {
        this.statDate = statDate;
    }

    public String getStatDate() {
        return statDate;
    }

    @Column(name = "create_time")
    private Date createTime;

    // Getter和Setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public Double getAvgRating() { return avgRating; }
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }

    public Double getAvgTaste() { return avgTaste; }
    public void setAvgTaste(Double avgTaste) { this.avgTaste = avgTaste; }

    public Double getAvgEnvironment() { return avgEnvironment; }
    public void setAvgEnvironment(Double avgEnvironment) { this.avgEnvironment = avgEnvironment; }

    public Double getAvgService() { return avgService; }
    public void setAvgService(Double avgService) { this.avgService = avgService; }

    public Double getAvgPrice() { return avgPrice; }
    public void setAvgPrice(Double avgPrice) { this.avgPrice = avgPrice; }

    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public Integer getPositiveReviews() { return positiveReviews; }
    public void setPositiveReviews(Integer positiveReviews) { this.positiveReviews = positiveReviews; }

    public Integer getNegativeReviews() { return negativeReviews; }
    public void setNegativeReviews(Integer negativeReviews) { this.negativeReviews = negativeReviews; }

    public Integer getNeutralReviews() { return neutralReviews; }
    public void setNeutralReviews(Integer neutralReviews) { this.neutralReviews = neutralReviews; }

    public Double getPositiveRate() { return positiveRate; }
    public void setPositiveRate(Double positiveRate) { this.positiveRate = positiveRate; }

    public String getUpdateDate() { return updateDate; }
    public void setUpdateDate(String updateDate) { this.updateDate = updateDate; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}