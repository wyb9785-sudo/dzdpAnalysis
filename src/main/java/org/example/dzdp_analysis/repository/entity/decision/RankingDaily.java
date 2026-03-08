// RankingDaily.java
package org.example.dzdp_analysis.repository.entity.decision;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "ranking_daily")
public class RankingDaily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rank_date")
    private String rankDate;

    @Column(name = "rank_type")
    private String rankType;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "composite_score")
    private Double compositeScore;

    @Column(name = "avg_price")
    private Double avgPrice;

    @Column(name = "positive_rate")
    private Double positiveRate;

    @Column(name = "update_time")
    private Date updateTime;

    // Getter和Setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRankDate() { return rankDate; }
    public void setRankDate(String rankDate) { this.rankDate = rankDate; }

    public String getRankType() { return rankType; }
    public void setRankType(String rankType) { this.rankType = rankType; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }

    public Double getCompositeScore() { return compositeScore; }
    public void setCompositeScore(Double compositeScore) { this.compositeScore = compositeScore; }

    public Double getAvgPrice() { return avgPrice; }
    public void setAvgPrice(Double avgPrice) { this.avgPrice = avgPrice; }

    public Double getPositiveRate() { return positiveRate; }
    public void setPositiveRate(Double positiveRate) { this.positiveRate = positiveRate; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}