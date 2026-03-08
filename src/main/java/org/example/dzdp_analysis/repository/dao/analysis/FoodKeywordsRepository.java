package org.example.dzdp_analysis.repository.dao.analysis;

import org.example.dzdp_analysis.repository.entity.analysis.FoodKeywords;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodKeywordsRepository extends JpaRepository<FoodKeywords, Long> {

    List<FoodKeywords> findByMerchantNameOrderByMentionCountDesc(String merchantName);
    long countByUpdateDate(String updateDate);
    @Query("SELECT f FROM FoodKeywords f WHERE f.merchantName LIKE %:merchantName% AND f.keyword != '无' ORDER BY f.mentionCount DESC")
    List<FoodKeywords> findByMerchantNameContainingAndKeywordNot(@Param("merchantName") String merchantName, Pageable pageable);
    // 修改这个方法，添加商户名参数
    @Query("SELECT f FROM FoodKeywords f WHERE f.merchantName = :merchantName AND f.keyword != '无' ORDER BY f.mentionCount DESC")
    List<FoodKeywords> findByMerchantNameAndKeywordNot(@Param("merchantName") String merchantName, Pageable pageable);

    // 其他方法保持不变...
    List<FoodKeywords> findTop10ByMerchantNameOrderByMentionCountDesc(String merchantName);
    // 根据商户名获取指定数量的热门菜品，按提及次数降序
    @Query("SELECT f FROM FoodKeywords f WHERE f.merchantName = :merchantName ORDER BY f.mentionCount DESC")
    List<FoodKeywords> findTopByMerchantNameOrderByMentionCountDesc(@Param("merchantName") String merchantName,
                                                                    @Param("limit") int limit);
    @Query("SELECT f FROM FoodKeywords f WHERE f.merchantName = :merchantName ORDER BY f.mentionCount DESC")
    List<FoodKeywords> findByMerchantName(@Param("merchantName") String merchantName);

    @Query("SELECT f FROM FoodKeywords f WHERE f.merchantName = :merchantName ORDER BY f.mentionCount DESC")
    List<FoodKeywords> findTopKeywordsByMerchant(@Param("merchantName") String merchantName);

}