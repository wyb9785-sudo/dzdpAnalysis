package org.example.dzdp_analysis.service.analysis;
import jakarta.transaction.Transactional;
import org.example.dzdp_analysis.repository.dao.analysis.*;
import org.example.dzdp_analysis.repository.dao.decision.TimeAnalysisRepository;
import org.example.dzdp_analysis.repository.dto.merchant.*;
import org.example.dzdp_analysis.repository.entity.analysis.*;
import org.example.dzdp_analysis.repository.entity.decision.TimeAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
@Service
@Transactional
public class MerchantAnalysisService {

    @Autowired
    private MerchantAnalysisRepository merchantAnalysisRepository;

    @Autowired
    private MerchantDashboardRepository merchantDashboardRepository;

    @Autowired
    private TimeAnalysisRepository timeAnalysisRepository;

    @Autowired
    private FoodKeywordsRepository foodKeywordsRepository;

    // 获取当前登录商户名称
    public String getCurrentMerchant() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                String username;

                if (principal instanceof UserDetails) {
                    username = ((UserDetails) principal).getUsername();
                } else if (principal instanceof String) {
                    username = (String) principal;
                } else {
                    username = "default_merchant";
                }

                // 记录日志以便调试
                System.out.println("获取到的商户名: " + username);
                return username;
            }
        } catch (Exception e) {
            System.err.println("获取商户名异常: " + e.getMessage());
        }

        // 返回默认商户名，确保不会空指针
        return "default_merchant";
    }

    // 获取当前商户的仪表板数据（雷达图用）
    public MerchantDashboardDTO getMerchantDashboardData(String merchantName) {
        try {
            System.out.println("查询商户仪表板数据: " + merchantName);

            // 首先尝试精确查询最新数据
            Optional<MerchantDashboard> dashboardOpt = merchantDashboardRepository
                    .findTopByMerchantNameOrderByUpdateTimeDesc(merchantName);

            if (dashboardOpt.isPresent()) {
                MerchantDashboard dashboard = dashboardOpt.get();
                System.out.println("找到精确匹配的仪表板数据: " + dashboard.getMerchantName());

                MerchantDashboardDTO dto = new MerchantDashboardDTO();
                dto.setMerchantName(dashboard.getMerchantName());
                dto.setAvgRating7d(dashboard.getAvgRating7d());
                dto.setAvgTaste7d(dashboard.getAvgTaste7d());
                dto.setAvgService7d(dashboard.getAvgService7d());
                dto.setAvgEnvironment7d(dashboard.getAvgEnvironment7d());
                dto.setPositiveRate7d(dashboard.getPositiveRate7d());

                return dto;
            }

            // 如果精确查询没有结果，尝试模糊查询
            List<MerchantDashboard> dashboards = merchantDashboardRepository
                    .findByMerchantNameContaining(merchantName);

            System.out.println("模糊查询结果数量: " + dashboards.size());

            if (!dashboards.isEmpty()) {
                // 取最新的一个
                MerchantDashboard dashboard = dashboards.get(0);
                System.out.println("使用模糊匹配的仪表板数据: " + dashboard.getMerchantName());

                MerchantDashboardDTO dto = new MerchantDashboardDTO();
                dto.setMerchantName(dashboard.getMerchantName());
                dto.setAvgRating7d(dashboard.getAvgRating7d());
                dto.setAvgTaste7d(dashboard.getAvgTaste7d());
                dto.setAvgService7d(dashboard.getAvgService7d());
                dto.setAvgEnvironment7d(dashboard.getAvgEnvironment7d());
                dto.setPositiveRate7d(dashboard.getPositiveRate7d());

                return dto;
            }

            System.out.println("未找到仪表板数据，返回null");
            return null;

        } catch (Exception e) {
            System.err.println("获取仪表板数据异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<FoodKeywords> getPopularFoods(String merchantName, int limit) {
        try {
            System.out.println("查询热门菜品，商户: " + merchantName + ", 限制: " + limit);

            // 首先尝试精确匹配
            List<FoodKeywords> foods = foodKeywordsRepository.findByMerchantName(merchantName);

            if (foods.isEmpty()) {
                // 如果精确匹配没有结果，尝试模糊匹配
                System.out.println("精确匹配无结果，尝试模糊匹配");
                Pageable pageable = PageRequest.of(0, limit);
                foods = foodKeywordsRepository.findByMerchantNameContainingAndKeywordNot(merchantName, pageable);
            }

            // 过滤掉无效关键词并限制数量
            List<FoodKeywords> filteredFoods = foods.stream()
                    .filter(item -> item.getKeyword() != null &&
                            !item.getKeyword().trim().isEmpty() &&
                            !"无".equals(item.getKeyword()) &&
                            !"无关键词".equals(item.getKeyword()))
                    .limit(limit)
                    .collect(Collectors.toList());

            if (filteredFoods.isEmpty()) {
                System.out.println("无有效菜品数据，使用模拟数据");
                return createMockFoodsData(merchantName);
            }

            System.out.println("找到 " + filteredFoods.size() + " 个有效菜品");
            return filteredFoods;

        } catch (Exception e) {
            System.err.println("获取热门菜品异常: " + e.getMessage());
            e.printStackTrace();
            return createMockFoodsData(merchantName);
        }
    }
    // 创建模拟菜品数据的方法
    private List<FoodKeywords> createMockFoodsData(String merchantName) {
        List<FoodKeywords> mockFoods = new ArrayList<>();
        String[] foods = {"猪肚鸡", "椰子鸡", "煲仔饭", "烤鱼", "奶茶", "烧鹅", "叉烧", "肠粉", "虾饺", "炒牛河"};
        int[] counts = {120, 98, 76, 65, 54, 43, 32, 28, 25, 22};

        for (int i = 0; i < foods.length; i++) {
            FoodKeywords food = new FoodKeywords();
            food.setKeyword(foods[i]);
            food.setMentionCount(counts[i]);
            food.setMerchantName(merchantName);
            mockFoods.add(food);
        }
        return mockFoods;
    }

    // 获取所有商户的气泡图数据
    public List<MerchantBubbleDTO> getAllMerchantsBubbleData() {

        List<MerchantAnalysis> dashboards = merchantAnalysisRepository.findAll();
        return dashboards.stream().map(dashboard -> {
            MerchantBubbleDTO dto = new MerchantBubbleDTO();
            dto.setMerchantName(dashboard.getMerchantName());
            dto.setAvgRating(dashboard.getAvgRating());
            dto.setPositiveRate(dashboard.getPositiveRate());
            dto.setTotalReviews(dashboard.getTotalReviews());
            return dto;
        }).collect(Collectors.toList());
    }

    // 获取情感趋势数据 - 修改后的方法
    public List<SentimentTrendDTO> getSentimentTrendData(String merchantName) {
        try {
            System.out.println("查询情感趋势数据，商户: " + merchantName);

            // 直接从商户分析表获取最新数据
            Optional<MerchantAnalysis> latestAnalysis = merchantAnalysisRepository
                    .findTopByMerchantNameOrderByUpdateDateDesc(merchantName);

            if (latestAnalysis.isPresent()) {
                MerchantAnalysis analysis = latestAnalysis.get();
                System.out.println("找到商户分析数据: " + analysis.getMerchantName());

                SentimentTrendDTO dto = new SentimentTrendDTO();
                dto.setMerchantName(analysis.getMerchantName());
                dto.setPositiveCount(analysis.getPositiveReviews() != null ? analysis.getPositiveReviews() : 0);
                dto.setNegativeCount(analysis.getNegativeReviews() != null ? analysis.getNegativeReviews() : 0);
                dto.setNeutralCount(analysis.getNeutralReviews() != null ? analysis.getNeutralReviews() : 0);
                dto.setTotalCount(analysis.getTotalReviews() != null ? analysis.getTotalReviews() : 0);

                return Arrays.asList(dto);
            } else {
                System.out.println("未找到商户分析数据，使用模拟数据");
                // 返回模拟数据
                SentimentTrendDTO mockDto = new SentimentTrendDTO();
                mockDto.setMerchantName(merchantName);
                mockDto.setPositiveCount(120);
                mockDto.setNegativeCount(15);
                mockDto.setNeutralCount(35);
                mockDto.setTotalCount(170);
                return Arrays.asList(mockDto);
            }

        } catch (Exception e) {
            System.err.println("获取情感趋势数据异常: " + e.getMessage());
            e.printStackTrace();

            // 异常时返回模拟数据
            SentimentTrendDTO mockDto = new SentimentTrendDTO();
            mockDto.setMerchantName(merchantName);
            mockDto.setPositiveCount(120);
            mockDto.setNegativeCount(15);
            mockDto.setNeutralCount(35);
            mockDto.setTotalCount(170);
            return Arrays.asList(mockDto);
        }
    }

    // 获取玫瑰图数据 - 修改后的方法
    public List<RoseChartDTO> getRoseChartData() {
        try {
            // 获取所有商户的最新分析数据
            List<String> merchantNames = merchantAnalysisRepository.findDistinctMerchantNames();
            List<RoseChartDTO> result = new ArrayList<>();

            for (String merchant : merchantNames) {
                Optional<MerchantAnalysis> latestAnalysis = merchantAnalysisRepository
                        .findTopByMerchantNameOrderByUpdateDateDesc(merchant);

                if (latestAnalysis.isPresent()) {
                    MerchantAnalysis analysis = latestAnalysis.get();
                    RoseChartDTO dto = new RoseChartDTO();
                    dto.setMerchantName(analysis.getMerchantName());
                    dto.setPositiveCount(analysis.getPositiveReviews() != null ? analysis.getPositiveReviews() : 0);
                    dto.setNegativeCount(analysis.getNegativeReviews() != null ? analysis.getNegativeReviews() : 0);

                    // 添加中性评论
                    dto.setNeutralCount(analysis.getNeutralReviews() != null ? analysis.getNeutralReviews() : 0);

                    double total = analysis.getTotalReviews() != null ? analysis.getTotalReviews() : 1;
                    double positive = dto.getPositiveCount();
                    dto.setPositiveRate(total > 0 ? (positive / total) * 100 : 0);

                    result.add(dto);
                }
            }

            return result;
        } catch (Exception e) {
            System.err.println("获取玫瑰图数据异常: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}