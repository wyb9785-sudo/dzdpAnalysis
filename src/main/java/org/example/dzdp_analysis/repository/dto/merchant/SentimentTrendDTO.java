package org.example.dzdp_analysis.repository.dto.merchant;

import lombok.Data;

@Data
public class SentimentTrendDTO {
    private String merchantName;
   // private String timePeriod;
    private Integer positiveCount;
    private Integer negativeCount;
    private Integer neutralCount;
    private Integer totalCount;
}