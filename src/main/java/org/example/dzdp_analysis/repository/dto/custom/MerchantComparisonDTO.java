package org.example.dzdp_analysis.repository.dto.custom;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MerchantComparisonDTO {
    private Map<String, Object> basicInfo;
    private Map<String, Object> ratings;
    private Map<String, Object> prices;
    private Map<String, Object> reviewStats;
    public MerchantComparisonDTO() {
        this.basicInfo = new HashMap<>();
        this.ratings = new HashMap<>();
        this.prices = new HashMap<>();
        this.reviewStats = new HashMap<>();
    }

    public void setBasicInfo(Map<String, Object> basicInfo) {
        this.basicInfo = basicInfo;
    }

    public void setRatings(Map<String, Object> ratings) {
        this.ratings = ratings;
    }

    public void setPrices(Map<String, Object> prices) {
        this.prices = prices;
    }

    public void setReviewStats(Map<String, Object> reviewStats) {
        this.reviewStats = reviewStats;
    }

    public Map<String, Object> getBasicInfo() {
        return basicInfo;
    }

    public Map<String, Object> getRatings() {
        return ratings;
    }

    public Map<String, Object> getPrices() {
        return prices;
    }

    public Map<String, Object> getReviewStats() {
        return reviewStats;
    }
}