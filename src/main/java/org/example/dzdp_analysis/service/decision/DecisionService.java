// DecisionService.java
package org.example.dzdp_analysis.service.decision;

import org.example.dzdp_analysis.repository.dto.custom.MerchantComparisonDTO;
import org.example.dzdp_analysis.repository.dto.custom.MerchantSummaryDTO;
import org.example.dzdp_analysis.repository.dao.analysis.FoodKeywordsRepository;
import org.example.dzdp_analysis.repository.dao.analysis.MerchantAnalysisRepository;
import org.example.dzdp_analysis.repository.dao.decision.RankingDailyRepository;
import org.example.dzdp_analysis.repository.entity.analysis.FoodKeywords;
import org.example.dzdp_analysis.repository.entity.analysis.MerchantAnalysis;
import org.example.dzdp_analysis.repository.entity.decision.RankingDaily;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DecisionService {
    private static final Logger logger = LoggerFactory.getLogger(DecisionService.class);
    @Autowired
    private MerchantAnalysisRepository merchantAnalysisRepository;

    @Autowired
    private RankingDailyRepository rankingDailyRepository;

    @Autowired
    private FoodKeywordsRepository foodKeywordsRepository;

    // 智能餐厅筛选
    public List<MerchantSummaryDTO> searchRestaurants(String merchantName, Double minRating,
                                                      Double maxPrice, Double minPositiveRate) {
        try {
            // 获取所有商户的最新分析数据
            List<MerchantAnalysis> allAnalyses = merchantAnalysisRepository.findAll();

            // 按商户名分组，获取每个商户的最新数据
            Map<String, MerchantAnalysis> latestAnalyses = allAnalyses.stream()
                    .collect(Collectors.toMap(
                            MerchantAnalysis::getMerchantName,
                            analysis -> analysis,
                            (existing, replacement) -> {
                                // 选择更新日期最新的数据
                                if (existing.getUpdateDate().compareTo(replacement.getUpdateDate()) > 0) {
                                    return existing;
                                } else {
                                    return replacement;
                                }
                            }
                    ));

            return latestAnalyses.values().stream()
                    .map(this::convertToSummaryDTO)
                    .filter(dto -> matchesFilters(dto, merchantName, minRating, maxPrice, minPositiveRate))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("搜索餐厅失败", e);
            return Collections.emptyList();
        }
    }

    // 获取所有商户名称（用于品类筛选）
    public List<String> getAllMerchantNames() {
        return merchantAnalysisRepository.findDistinctMerchantNames();
    }

    // 获取排行榜类型
    public List<String> getRankTypes() {
        return rankingDailyRepository.findDistinctRankTypes();
    }

    // 获取排行榜
    // DecisionService.java - 添加排序方法
    public List<RankingDaily> getRankings(String rankType, int limit, String sortField, String sortOrder) {
        String latestDate = rankingDailyRepository.findLatestRankDate();
        if (latestDate == null) {
            return Collections.emptyList();
        }

        List<RankingDaily> rankings = rankingDailyRepository.findByRankTypeAndRankDateOrderByRankPositionAsc(rankType, latestDate);

        // 应用排序
        if (sortField != null && !"default".equals(sortField)) {
            Comparator<RankingDaily> comparator = null;

            switch (sortField) {
                case "avgPrice":
                    comparator = Comparator.comparing(RankingDaily::getAvgPrice,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "positiveRate":
                    comparator = Comparator.comparing(RankingDaily::getPositiveRate,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "compositeScore":
                default:
                    comparator = Comparator.comparing(RankingDaily::getCompositeScore,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
            }

            if ("desc".equals(sortOrder)) {
                comparator = comparator.reversed();
            }

            rankings = rankings.stream()
                    .sorted(comparator)
                    .collect(Collectors.toList());
        }

        return rankings.stream().limit(limit).collect(Collectors.toList());
    }

    // 商户对比
    // DecisionService.java - 完整修复compareMerchants方法
    public MerchantComparisonDTO compareMerchants(List<String> merchantNames) {
        MerchantComparisonDTO comparisonDTO = new MerchantComparisonDTO();
        Map<String, Object> basicInfo = new HashMap<>();
        Map<String, Object> ratings = new HashMap<>();
        Map<String, Object> prices = new HashMap<>();
        Map<String, Object> reviewStats = new HashMap<>();

        for (String merchantName : merchantNames) {
            // 获取商户的最新分析数据
            List<MerchantAnalysis> analyses = merchantAnalysisRepository.findByMerchantNameOrderByUpdateDateDesc(merchantName);
            if (!analyses.isEmpty()) {
                MerchantAnalysis ma = analyses.get(0);

                // 基本信息
                basicInfo.put(merchantName, Map.of(
                        "name", merchantName,
                        "category", "餐饮",
                        "updateDate", ma.getUpdateDate() != null ? ma.getUpdateDate() : "未知"
                ));

                // 评分信息 - 确保所有评分字段都有值
                ratings.put(merchantName, Map.of(
                        "avgRating", ma.getAvgRating() != null ? ma.getAvgRating() : 0,
                        "tasteScore", ma.getAvgTaste() != null ? ma.getAvgTaste() : 0,
                        "serviceScore", ma.getAvgService() != null ? ma.getAvgService() : 0,
                        "environmentScore", ma.getAvgEnvironment() != null ? ma.getAvgEnvironment() : 0
                ));

                // 价格信息
                prices.put(merchantName, ma.getAvgPrice() != null ? ma.getAvgPrice() : 0);

                // 完整的评论统计
                reviewStats.put(merchantName, Map.of(
                        "totalReviews", ma.getTotalReviews() != null ? ma.getTotalReviews() : 0,
                        "positiveRate", ma.getPositiveRate() != null ? ma.getPositiveRate() : 0,
                        "positiveReviews", ma.getPositiveReviews() != null ? ma.getPositiveReviews() : 0,
                        "negativeReviews", ma.getNegativeReviews() != null ? ma.getNegativeReviews() : 0,
                        "neutralReviews", ma.getNeutralReviews() != null ? ma.getNeutralReviews() : 0
                ));
            } else {
                // 如果没有找到数据，提供默认值
                basicInfo.put(merchantName, Map.of("name", merchantName, "category", "餐饮", "updateDate", "无数据"));
                ratings.put(merchantName, Map.of("avgRating", 0, "tasteScore", 0, "serviceScore", 0, "environmentScore", 0));
                prices.put(merchantName, 0);
                reviewStats.put(merchantName, Map.of(
                        "totalReviews", 0, "positiveRate", 0,
                        "positiveReviews", 0, "negativeReviews", 0, "neutralReviews", 0
                ));
            }
        }

        comparisonDTO.setBasicInfo(basicInfo);
        comparisonDTO.setRatings(ratings);
        comparisonDTO.setPrices(prices);
        comparisonDTO.setReviewStats(reviewStats);

        return comparisonDTO;
    }

    // 获取热门关键词
    // DecisionService.java - 修改getTopKeywords方法
    public List<FoodKeywords> getTopKeywords(String merchantName, int limit) {
        try {
            List<FoodKeywords> allKeywords = foodKeywordsRepository.findByMerchantName(merchantName);

            // 过滤掉'无'关键词并按提及次数排序
            return allKeywords.stream()
                    .filter(keyword -> !"无".equals(keyword.getKeyword()))
                    .sorted((a, b) -> b.getMentionCount().compareTo(a.getMentionCount()))
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("获取热门关键词失败", e);
            return Collections.emptyList();
        }
    }

    // DecisionService.java - 修改convertToSummaryDTO方法
    private MerchantSummaryDTO convertToSummaryDTO(MerchantAnalysis analysis) {
        MerchantSummaryDTO dto = new MerchantSummaryDTO();
        dto.setMerchantName(analysis.getMerchantName());
        dto.setCategory("餐饮");
        dto.setAvgRating(analysis.getAvgRating());
        dto.setAvgPrice(analysis.getAvgPrice());
        dto.setPositiveRate(analysis.getPositiveRate());
        dto.setTasteScore(analysis.getAvgTaste());
        dto.setEnvironmentScore(analysis.getAvgEnvironment());
        dto.setServiceScore(analysis.getAvgService());
        dto.setTotalReviews(analysis.getTotalReviews());
        dto.setUpdateDate(analysis.getUpdateDate());

        // 添加评论统计数据
        dto.setPositiveReviews(analysis.getPositiveReviews());
        dto.setNegativeReviews(analysis.getNegativeReviews());
        dto.setNeutralReviews(analysis.getNeutralReviews());

        return dto;
    }

    // 修改匹配过滤器方法
    private boolean matchesFilters(MerchantSummaryDTO dto, String merchantName, Double minRating,
                                   Double maxPrice, Double minPositiveRate) {
        // 商户名称筛选
        if (merchantName != null && !merchantName.isEmpty() &&
                !dto.getMerchantName().toLowerCase().contains(merchantName.toLowerCase())) {
            return false;
        }

        // 其他筛选条件保持不变
        if (minRating != null && (dto.getAvgRating() == null || dto.getAvgRating() < minRating)) {
            return false;
        }
        if (maxPrice != null && (dto.getAvgPrice() == null || dto.getAvgPrice() > maxPrice)) {
            return false;
        }
        if (minPositiveRate != null && (dto.getPositiveRate() == null || dto.getPositiveRate() < minPositiveRate)) {
            return false;
        }
        return true;
    }
}