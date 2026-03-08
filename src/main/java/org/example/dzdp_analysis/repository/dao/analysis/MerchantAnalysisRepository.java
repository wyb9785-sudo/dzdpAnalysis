package org.example.dzdp_analysis.repository.dao.analysis;

import org.example.dzdp_analysis.repository.entity.analysis.MerchantAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantAnalysisRepository extends JpaRepository<MerchantAnalysis, Long> {

    // 添加缺失的方法
    Optional<MerchantAnalysis> findTopByMerchantNameOrderByUpdateDateDesc(String merchantName);
    long countByUpdateDate(String updateDate);
   // 确保继承了JpaSpecificationExecutor
    List<MerchantAnalysis> findByMerchantNameOrderByUpdateDateDesc(String merchantName);
    // 添加按stat_date查询的方法
    Optional<MerchantAnalysis> findTopByMerchantNameAndStatDateOrderByCreateTimeDesc(String merchantName, String statDate);
    // 根据商户名获取最新的一条分析数据
    Optional<MerchantAnalysis> findTopByMerchantNameOrderByCreateTimeDesc(String merchantName);

    // 根据商户名获取所有分析数据
    List<MerchantAnalysis> findByMerchantNameOrderByCreateTimeDesc(String merchantName);

    // 自定义查询方法：按商户名称查找并按日期降序排列
    @Query("SELECT ma FROM MerchantAnalysis ma WHERE ma.merchantName = :merchantName ORDER BY ma.createTime DESC")
    List<MerchantAnalysis> findLatestByMerchant(@Param("merchantName") String merchantName);
    // 其他现有方法...
    @Modifying
    @Query("DELETE FROM MerchantAnalysis m WHERE m.updateDate = :updateDate")
    void deleteByUpdateDate(@Param("updateDate") String updateDate);

    @Query("SELECT m FROM MerchantAnalysis m WHERE m.updateDate = :updateDate")
    List<MerchantAnalysis> findByUpdateDate(@Param("updateDate") String updateDate);
    boolean existsByUpdateDate(String updateDate);

    @Query("SELECT DISTINCT ma.merchantName FROM MerchantAnalysis ma")
    List<String> findDistinctMerchantNames();
    @Query("SELECT ma FROM MerchantAnalysis ma WHERE ma.merchantName = :merchantName ORDER BY ma.updateDate DESC")
    List<MerchantAnalysis> findLatestByMerchantName(@Param("merchantName") String merchantName);

    @Query("SELECT DISTINCT ma.merchantName FROM MerchantAnalysis ma WHERE ma.merchantName LIKE %:keyword%")
    List<String> findMerchantNamesByKeyword(@Param("keyword") String keyword);

}