package org.example.dzdp_analysis.repository.dao.data;  // 添加包声明

import org.example.dzdp_analysis.repository.entity.data.DataQualityReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DataQualityReportRepository extends JpaRepository<DataQualityReport, Long> {

    Optional<DataQualityReport> findByTaskId(Long taskId);
    List<DataQualityReport> findByUploadDateOrderByCreateTimeDesc(String uploadDate);
    List<DataQualityReport> findAllByOrderByCreateTimeDesc();
    @Override
    Page<DataQualityReport> findAll(Pageable pageable);
    List<DataQualityReport> findByUploadDateOrderByIdDesc(String uploadDate);

    // 添加缺失的分页查询方法
    // 修复分页查询方法 - 添加缺失的注解
    Page<DataQualityReport> findByUploadDate(@Param("uploadDate") String uploadDate, Pageable pageable);

    // 在DataQualityReportRepository接口中添加
    @Query("SELECT AVG(q.qualityScore) FROM DataQualityReport q WHERE q.qualityScore IS NOT NULL")
    Double findAvgQualityScore();

    @Query("SELECT COUNT(q) FROM DataQualityReport q WHERE q.qualityScore >= 90")
    Long countHighQualityReports();

    @Query("SELECT COUNT(q) FROM DataQualityReport q WHERE q.qualityScore < 60")
    Long countLowQualityReports();

    @Query("SELECT MAX(q.createTime) FROM DataQualityReport q")
    Optional<LocalDateTime> findLatestReportTime();
}