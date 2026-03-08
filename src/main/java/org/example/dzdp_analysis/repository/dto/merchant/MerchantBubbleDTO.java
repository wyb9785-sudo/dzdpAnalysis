package org.example.dzdp_analysis.repository.dto.merchant;

import lombok.Data;

@Data
public class MerchantBubbleDTO {
    private String merchantName;
    private Double avgRating;
    private Double positiveRate;
    private Integer totalReviews;
   // private String category;
}