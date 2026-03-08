package org.example.dzdp_analysis.repository.dto.custom;

import lombok.Data;

@Data
public class MerchantSummaryDTO {
    private String merchantId;
    private String merchantName;
    private String category;
    private Double avgRating;
    private Double avgPrice;
    private Double positiveRate; // 这里应该是百分比格式，如85.63
    private Double tasteScore;
    private Double environmentScore;
    private Double serviceScore;
    private Integer totalReviews;
    private String updateDate;

    // 添加评论统计字段（如果需要）
    private Integer positiveReviews;
    private Integer negativeReviews;
    private Integer neutralReviews;

    public void setPositiveReviews(Integer positiveReviews) {
        this.positiveReviews = positiveReviews;
    }

    public void setNegativeReviews(Integer negativeReviews) {
        this.negativeReviews = negativeReviews;
    }

    public void setNeutralReviews(Integer neutralReviews) {
        this.neutralReviews = neutralReviews;
    }

    public Integer getPositiveReviews() {
        return positiveReviews;
    }

    public Integer getNegativeReviews() {
        return negativeReviews;
    }

    public Integer getNeutralReviews() {
        return neutralReviews;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAvgRating(Double avgRating) {
        this.avgRating = avgRating;
    }

    public void setAvgPrice(Double avgPrice) {
        this.avgPrice = avgPrice;
    }

    public void setPositiveRate(Double positiveRate) {
        this.positiveRate = positiveRate;
    }

    public void setTasteScore(Double tasteScore) {
        this.tasteScore = tasteScore;
    }

    public void setEnvironmentScore(Double environmentScore) {
        this.environmentScore = environmentScore;
    }

    public void setServiceScore(Double serviceScore) {
        this.serviceScore = serviceScore;
    }

    public void setTotalReviews(Integer totalReviews) {
        this.totalReviews = totalReviews;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getCategory() {
        return category;
    }

    public Double getAvgRating() {
        return avgRating;
    }

    public Double getAvgPrice() {
        return avgPrice;
    }

    public Double getPositiveRate() {
        return positiveRate;
    }

    public Double getTasteScore() {
        return tasteScore;
    }

    public Double getEnvironmentScore() {
        return environmentScore;
    }

    public Double getServiceScore() {
        return serviceScore;
    }

    public Integer getTotalReviews() {
        return totalReviews;
    }

    public String getUpdateDate() {
        return updateDate;
    }
}