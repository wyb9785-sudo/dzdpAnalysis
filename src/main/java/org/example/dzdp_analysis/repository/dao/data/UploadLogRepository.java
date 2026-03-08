package org.example.dzdp_analysis.repository.dao.data;

import org.example.dzdp_analysis.repository.entity.data.UploadLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadLogRepository extends JpaRepository<UploadLog, Long> {

    // 修改为正确的查询方法命名
    List<UploadLog> findAllByOrderByUploadTimeDesc();

    // 保留其他方法
    List<UploadLog> findByUploadStatusOrderByUploadTimeDesc(String status);
    List<UploadLog> findByOperator(String operator);
    // 添加分页查询
    @Query("SELECT u FROM UploadLog u ORDER BY u.uploadTime DESC")
    List<UploadLog> findRecentUploads();
    Optional<UploadLog> findFirstByHdfsPathOrderByUploadTimeDesc(String hdfsPath);
    // 添加统计方法
    @Override
    long count();
}