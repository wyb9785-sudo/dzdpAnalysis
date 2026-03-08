package org.example.dzdp_analysis.controller.analysis;

import org.example.dzdp_analysis.repository.dto.merchant.MerchantBubbleDTO;
import org.example.dzdp_analysis.repository.dto.merchant.MerchantDashboardDTO;
import org.example.dzdp_analysis.repository.dto.merchant.RoseChartDTO;
import org.example.dzdp_analysis.repository.dto.merchant.SentimentTrendDTO;
import org.example.dzdp_analysis.repository.entity.analysis.FoodKeywords;
import org.example.dzdp_analysis.service.analysis.MerchantAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant-analysis")
public class MerchantAnalysisController {

    @Autowired
    private MerchantAnalysisService merchantAnalysisService;

    // 获取当前商户的仪表板数据
    @GetMapping("/dashboard-data")
    public ResponseEntity<?> getDashboardData() {
        try {
            String merchantName = merchantAnalysisService.getCurrentMerchant();
            MerchantDashboardDTO data = merchantAnalysisService.getMerchantDashboardData(merchantName);
            return ResponseEntity.ok(data != null ? data : Collections.emptyMap());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取仪表板数据失败: " + e.getMessage()));
        }
    }

    // 获取热门菜品
    @GetMapping("/foods")
    public ResponseEntity<?> getFoods(@RequestParam(defaultValue = "10") int limit) {
        try {
            String merchantName = merchantAnalysisService.getCurrentMerchant();
            List<FoodKeywords> foods = merchantAnalysisService.getPopularFoods(merchantName, limit);
            return ResponseEntity.ok(foods != null ? foods : Collections.emptyList());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取菜品数据失败: " + e.getMessage()));
        }
    }

    // 获取气泡图数据
    @GetMapping("/bubble-data")
    public ResponseEntity<?> getBubbleData() {
        try {
            List<MerchantBubbleDTO> data = merchantAnalysisService.getAllMerchantsBubbleData();
            return ResponseEntity.ok(data != null ? data : Collections.emptyList());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取气泡图数据失败: " + e.getMessage()));
        }
    }

    // 获取情感趋势数据
    @GetMapping("/sentiment-trend")
    public ResponseEntity<?> getSentimentTrend() {
        try {
            String merchantName = merchantAnalysisService.getCurrentMerchant();
            List<SentimentTrendDTO> data = merchantAnalysisService.getSentimentTrendData(merchantName);

            return ResponseEntity.ok(data != null ? data : Collections.emptyList());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取情感趋势数据失败: " + e.getMessage()));
        }
    }

    // 获取玫瑰图数据
    @GetMapping("/rose-chart")
    public ResponseEntity<?> getRoseChart() {
        try {
            List<RoseChartDTO> data = merchantAnalysisService.getRoseChartData();
            return ResponseEntity.ok(data != null ? data : Collections.emptyList());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取玫瑰图数据失败: " + e.getMessage()));
        }
    }

    // 商户口碑分析 - 雷达图
    @GetMapping("/overview")
    public ResponseEntity<?> getMerchantOverview() {
        try {
            String merchantName = merchantAnalysisService.getCurrentMerchant();
            return ResponseEntity.ok(merchantAnalysisService.getMerchantDashboardData(merchantName));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

}