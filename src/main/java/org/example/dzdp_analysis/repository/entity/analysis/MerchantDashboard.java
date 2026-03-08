// MerchantDashboard.java
package org.example.dzdp_analysis.repository.entity.analysis;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "merchant_dashboard")
public class MerchantDashboard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "stat_date")
    private String statDate;

    @Column(name = "avg_rating_7d")
    private Double avgRating7d;

    @Column(name = "avg_taste_7d")
    private Double avgTaste7d;

    @Column(name = "avg_service_7d")
    private Double avgService7d;

    @Column(name = "avg_environment_7d")
    private Double avgEnvironment7d;

    @Column(name = "positive_rate_7d")
    private Double positiveRate7d;

    @Column(name = "trend_direction")
    private String trendDirection;

    @Column(name = "top_foods", columnDefinition = "TEXT")
    private String topFoods;

    @Column(name = "update_time")
    private Date updateTime;

    // Getter和Setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getStatDate() { return statDate; }
    public void setStatDate(String statDate) { this.statDate = statDate; }

    public Double getAvgRating7d() { return avgRating7d; }
    public void setAvgRating7d(Double avgRating7d) { this.avgRating7d = avgRating7d; }

    public Double getAvgTaste7d() { return avgTaste7d; }
    public void setAvgTaste7d(Double avgTaste7d) { this.avgTaste7d = avgTaste7d; }

    public Double getAvgService7d() { return avgService7d; }
    public void setAvgService7d(Double avgService7d) { this.avgService7d = avgService7d; }

    public Double getAvgEnvironment7d() { return avgEnvironment7d; }
    public void setAvgEnvironment7d(Double avgEnvironment7d) { this.avgEnvironment7d = avgEnvironment7d; }

    public Double getPositiveRate7d() { return positiveRate7d; }
    public void setPositiveRate7d(Double positiveRate7d) { this.positiveRate7d = positiveRate7d; }

    public String getTrendDirection() { return trendDirection; }
    public void setTrendDirection(String trendDirection) { this.trendDirection = trendDirection; }

    public String getTopFoods() { return topFoods; }
    public void setTopFoods(String topFoods) { this.topFoods = topFoods; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}