package org.example.dzdp_analysis.repository.dao.analysis;

import org.example.dzdp_analysis.repository.entity.analysis.MerchantDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantDashboardRepository extends JpaRepository<MerchantDashboard, Long> {
    @Query("SELECT m FROM MerchantDashboard m WHERE m.merchantName LIKE %:merchantName%")
    List<MerchantDashboard> findByMerchantNameContaining(@Param("merchantName") String merchantName);

    @Query("SELECT MAX(m.updateTime) FROM MerchantDashboard m")
    Date findLatestUpdateTime();
    Optional<MerchantDashboard> findByMerchantName(String merchantName);
    long countByStatDate(String statDate);
    Optional<MerchantDashboard> findByMerchantNameAndStatDate(String merchantName, String statDate);
    // 根据商户名获取最新的仪表板数据
    Optional<MerchantDashboard> findTopByMerchantNameOrderByUpdateTimeDesc(String merchantName);
    void deleteByStatDate(String statDate);

    List<MerchantDashboard> findByMerchantNameOrderByStatDateDesc(String merchantName);

    @Query("SELECT m FROM MerchantDashboard m WHERE m.merchantName = :merchantName ORDER BY m.statDate DESC")
    List<MerchantDashboard> findLatestByMerchantName(@Param("merchantName") String merchantName);
}