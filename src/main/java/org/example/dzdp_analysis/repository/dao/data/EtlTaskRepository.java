package org.example.dzdp_analysis.repository.dao.data;

import org.example.dzdp_analysis.repository.entity.data.EtlTask;
import org.joda.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EtlTaskRepository extends JpaRepository<EtlTask, Long> {

    List<EtlTask> findByStatusOrderByStartTimeDesc(String status);
    List<EtlTask> findByStatusAndStartTimeBefore(String status, java.time.LocalDateTime time);
    List<EtlTask> findByOperator(String operator);


    @Query("SELECT e FROM EtlTask e WHERE e.hdfsPath = :hdfsPath ORDER BY e.startTime DESC")
    List<EtlTask> findByHdfsPath(@Param("hdfsPath") String hdfsPath);

    // 修复LIMIT查询 - 使用TOP 1代替LIMIT 1
    @Query("SELECT e FROM EtlTask e WHERE e.hdfsPath = :hdfsPath ORDER BY e.startTime DESC")
    List<EtlTask> findTop1ByHdfsPathOrderByStartTimeDesc(@Param("hdfsPath") String hdfsPath);

    // 修复countByStatus方法 - 使用正确的JPA查询语法
    @Query("SELECT COUNT(e) FROM EtlTask e WHERE e.status = :status")
    long countByStatus(@Param("status") String status);


    @Query("SELECT e FROM EtlTask e WHERE e.startTime >= :startTime AND e.startTime <= :endTime")
    List<EtlTask> findByTimeRange(@Param("startTime") java.time.LocalDateTime startTime,
                                  @Param("endTime") java.time.LocalDateTime endTime);
    // 新增方法：按开始时间倒序排列
    List<EtlTask> findAllByOrderByStartTimeDesc();

    @Query("SELECT SUM(t.recordsProcessed) FROM EtlTask t WHERE t.status = :status")
    Long sumRecordsProcessedByStatus(@Param("status") String status);

    // 添加分页查询方法
    @Override
    Page<EtlTask> findAll(Pageable pageable);
    Page<EtlTask> findByTaskNameContainingOrOperatorContaining(
            String taskName, String operator, Pageable pageable);
    //按状态分页查询
    Page<EtlTask> findByStatus(String status, Pageable pageable);
    @Override
    // 添加统计方法
    long count();
}