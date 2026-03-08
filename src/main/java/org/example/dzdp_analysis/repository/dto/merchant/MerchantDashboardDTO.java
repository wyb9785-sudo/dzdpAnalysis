package org.example.dzdp_analysis.repository.dto.merchant;

import lombok.Data;
import org.example.dzdp_analysis.repository.entity.analysis.MerchantDashboard;

@Data
public class MerchantDashboardDTO {
    private String merchantName;
    private Double avgRating7d;
    private Double avgTaste7d;
    private Double avgService7d;
    private Double avgEnvironment7d;
    private Double positiveRate7d;
    private Integer totalReviews7d; // 添加这个字段

    // 添加构造方法
    public MerchantDashboardDTO() {}

    public MerchantDashboardDTO(MerchantDashboard dashboard) {
        this.merchantName = dashboard.getMerchantName();
        this.avgRating7d = dashboard.getAvgRating7d();
        this.avgTaste7d = dashboard.getAvgTaste7d();
        this.avgService7d = dashboard.getAvgService7d();
        this.avgEnvironment7d = dashboard.getAvgEnvironment7d();
        this.positiveRate7d = dashboard.getPositiveRate7d();
        // 如果需要totalReviews7d，需要在MerchantDashboard实体中添加这个字段
    }
    // 归一化方法
    public Double getNormalizedRating() {
        return normalizeScore(avgRating7d);
    }

    public Double getNormalizedTaste() {
        return normalizeScore(avgTaste7d);
    }

    public Double getNormalizedService() {
        return normalizeScore(avgService7d);
    }

    public Double getNormalizedEnvironment() {
        return normalizeScore(avgEnvironment7d);
    }

    private Double normalizeScore(Double score) {
        if (score == null) {
            return 0.0;
        }
        return (score - 1) / 4 * 100;
    }
}