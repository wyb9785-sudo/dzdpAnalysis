package org.example.dzdp_analysis.repository.dao.decision;

import org.example.dzdp_analysis.repository.entity.decision.RankingDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


// RankingDailyRepository.java
public interface RankingDailyRepository extends JpaRepository<RankingDaily, Long> {

    @Query("SELECT r FROM RankingDaily r WHERE r.rankType = :rankType AND r.rankDate = :rankDate ORDER BY r.rankPosition ASC")
    List<RankingDaily> findByRankTypeAndRankDateOrderByRankPositionAsc(
            @Param("rankType") String rankType,
            @Param("rankDate") String rankDate);

    @Modifying
    @Query("DELETE FROM RankingDaily r WHERE r.rankDate = :rankDate")
    void deleteByRankDate(@Param("rankDate") String rankDate);
    List<RankingDaily> findByMerchantNameAndRankDate(String merchantName, String rankDate);
    // 添加查询方法用于验证
    List<RankingDaily> findByRankDate(String rankDate);
    long countByRankDate(String rankDate);
    @Query("SELECT DISTINCT r.rankType FROM RankingDaily r WHERE r.rankType IS NOT NULL")
    List<String> findDistinctRankTypes();

    @Query("SELECT MAX(r.rankDate) FROM RankingDaily r")
    String findLatestRankDate();
}
