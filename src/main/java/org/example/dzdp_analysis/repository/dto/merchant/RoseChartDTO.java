package org.example.dzdp_analysis.repository.dto.merchant;
import lombok.Data;

@Data
public class RoseChartDTO {
    private String merchantName;
    private Integer positiveCount;
    private Integer negativeCount;
    private Integer neutralCount; // 添加中性评论字段
    private Double positiveRate;
}