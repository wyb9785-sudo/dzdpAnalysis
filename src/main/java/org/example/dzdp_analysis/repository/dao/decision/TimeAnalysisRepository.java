package org.example.dzdp_analysis.repository.dao.decision;

import org.example.dzdp_analysis.repository.entity.decision.TimeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeAnalysisRepository extends JpaRepository<TimeAnalysis, Long> {
    long countByUpdateDate(String updateDate);

    List<TimeAnalysis> findByMerchantNameOrderByTimePeriodDesc(String merchantName);

    List<TimeAnalysis> findByMerchantNameAndTimePeriod(String merchantName, String timePeriod);
    // 根据商户名获取指定数量的最新时间分析数据
    @Query("SELECT t FROM TimeAnalysis t WHERE t.merchantName = :merchantName ORDER BY t.timePeriod DESC")
    List<TimeAnalysis> findTopByMerchantNameOrderByTimePeriodDesc(@Param("merchantName") String merchantName,
                                                                  @Param("limit") int limit);
    @Query("SELECT MAX(t.updateDate) FROM TimeAnalysis t")
    String findLatestDate();

    @Query("SELECT t FROM TimeAnalysis t WHERE t.updateDate = :date")
    List<TimeAnalysis> findByUpdateDate(@Param("date") String date);
    // 获取最近2条数据用于异常检测
    @Query("SELECT t FROM TimeAnalysis t WHERE t.merchantName = :merchantName ORDER BY t.timePeriod DESC LIMIT 2")
    List<TimeAnalysis> findTop2ByMerchantNameOrderByTimePeriodDesc(@Param("merchantName") String merchantName);
    @Modifying
    @Query("DELETE FROM TimeAnalysis t WHERE t.updateDate = :updateDate")
    void deleteByUpdateDate(@Param("updateDate") String updateDate);

    @Query("SELECT t FROM TimeAnalysis t WHERE t.merchantName = :merchantName ORDER BY t.timePeriod DESC")
    List<TimeAnalysis> findByMerchantName(@Param("merchantName") String merchantName);
}