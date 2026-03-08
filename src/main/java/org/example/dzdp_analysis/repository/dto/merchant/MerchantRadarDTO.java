package org.example.dzdp_analysis.repository.dto.merchant;

import lombok.Data;
import java.util.Map;

@Data
public class MerchantRadarDTO {
    private String merchantName;
    private Double normalizedRating;      // 归一化后的综合评分 (0-100)
    private Double normalizedTaste;       // 归一化后的口味评分 (0-100)
    private Double normalizedService;     // 归一化后的服务评分 (0-100)
    private Double normalizedEnvironment; // 归一化后的环境评分 (0-100)
    private Double positiveRate;          // 好评率 (0-100)

    // 归一化方法：将1-5分转换为0-100分
    private Double normalizeScore(Double score) {
        if (score == null) {
            return 0.0;
        }
        return (score - 1) / 4 * 100;
    }

    public MerchantRadarDTO(String merchantName, Double rating, Double taste,
                            Double service, Double environment, Double positiveRate) {
        this.merchantName = merchantName;
        this.normalizedRating = normalizeScore(rating);
        this.normalizedTaste = normalizeScore(taste);
        this.normalizedService = normalizeScore(service);
        this.normalizedEnvironment = normalizeScore(environment);
        this.positiveRate = positiveRate != null ? positiveRate : 0.0;
    }

    public Map<String, Object> toRadarData() {
        return Map.of(
                "merchant", merchantName,
                "value", new double[]{
                        normalizedTaste,
                        normalizedService,
                        normalizedEnvironment,
                        normalizedRating,
                        positiveRate
                },
                "name", "当前评分"
        );
    }
}