// DecisionController.java
package org.example.dzdp_analysis.controller.decision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.dzdp_analysis.repository.dto.custom.MerchantComparisonDTO;
import org.example.dzdp_analysis.repository.dto.custom.MerchantSummaryDTO;
import org.example.dzdp_analysis.repository.entity.analysis.FoodKeywords;
import org.example.dzdp_analysis.repository.entity.decision.RankingDaily;
import org.example.dzdp_analysis.service.decision.DecisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/decision")
@CrossOrigin(origins = "http://localhost:8081")
public class DecisionController {

    private static final Logger logger = LoggerFactory.getLogger(DecisionController.class);

    @Autowired
    private DecisionService decisionService;

    // 获取所有商户名称（用于筛选）
    @GetMapping("/merchants")
    public ResponseEntity<?> getMerchantNames() {
        logger.info("获取商户名称列表");
        try {
            List<String> merchants = decisionService.getAllMerchantNames();
            return ResponseEntity.ok(merchants); // 直接返回列表
        } catch (Exception e) {
            logger.error("获取商户名称失败: ", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/rank-types")
    public ResponseEntity<?> getRankTypes() {
        logger.info("获取排行榜类型");
        try {
            List<String> types = decisionService.getRankTypes();
            return ResponseEntity.ok(types); // 直接返回列表
        } catch (Exception e) {
            logger.error("获取排行榜类型失败: ", e);
            return ResponseEntity.ok(Arrays.asList("综合排名", "口味排名", "性价比排名"));
        }
    }
    //排行榜
    // DecisionController.java - 修改getRankings方法
    @GetMapping("/rankings")
    public ResponseEntity<?> getRankings(
            @RequestParam String rankType,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {

        logger.info("获取排行榜: rankType={}, limit={}, sortField={}, sortOrder={}",
                rankType, limit, sortField, sortOrder);

        try {
            List<RankingDaily> rankings = decisionService.getRankings(rankType, limit, sortField, sortOrder);
            return ResponseEntity.ok(rankings);
        } catch (Exception e) {
            logger.error("获取排行榜失败: ", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    // 智能餐厅筛选

    // DecisionController.java - 修复搜索逻辑
    @GetMapping("/search")
    public ResponseEntity<?> searchRestaurants(
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minPositiveRate) {

        logger.info("智能餐厅筛选: merchantName={}, minRating={}, maxPrice={}, minPositiveRate={}",
                merchantName, minRating, maxPrice, minPositiveRate);

        try {
            List<MerchantSummaryDTO> results = decisionService.searchRestaurants(
                    merchantName, minRating, maxPrice, minPositiveRate);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("餐厅筛选失败: ", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }


    // 商户对比
    // DecisionController.java - 修复compareMerchants方法
    @PostMapping("/compare")
    public ResponseEntity<MerchantComparisonDTO> compareMerchants(@RequestBody List<String> merchantNames) {
        logger.info("商户对比请求: {}", merchantNames);

        if (merchantNames == null || merchantNames.size() < 2 || merchantNames.size() > 3) {
            logger.warn("商户数量不符合要求: {}", merchantNames != null ? merchantNames.size() : "null");
            return ResponseEntity.badRequest().body(new MerchantComparisonDTO());
        }

        try {
            MerchantComparisonDTO comparisonData = decisionService.compareMerchants(merchantNames);

            // 确保返回的数据不为null
            if (comparisonData == null) {
                comparisonData = new MerchantComparisonDTO();
            }

            // 确保各个map不为null
            if (comparisonData.getBasicInfo() == null) {
                comparisonData.setBasicInfo(new HashMap<>());
            }
            if (comparisonData.getRatings() == null) {
                comparisonData.setRatings(new HashMap<>());
            }
            if (comparisonData.getPrices() == null) {
                comparisonData.setPrices(new HashMap<>());
            }
            if (comparisonData.getReviewStats() == null) {
                comparisonData.setReviewStats(new HashMap<>());
            }

            logger.info("商户对比成功: {}", merchantNames);
            return ResponseEntity.ok(comparisonData);
        } catch (Exception e) {
            logger.error("商户对比失败: ", e);
            return ResponseEntity.ok(new MerchantComparisonDTO());
        }
    }

    //获取热门关键词
    // DecisionController.java - 添加菜品推荐方法
    @GetMapping("/keywords/{merchantName}")
    public ResponseEntity<List<FoodKeywords>> getKeywords(
            @PathVariable String merchantName,
            @RequestParam(defaultValue = "10") int limit) {

        logger.info("获取商户热门菜品: merchantName={}, limit={}", merchantName, limit);

        try {
            List<FoodKeywords> keywords = decisionService.getTopKeywords(merchantName, limit);
            return ResponseEntity.ok(keywords);
        } catch (Exception e) {
            logger.error("获取菜品关键词失败: ", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
}