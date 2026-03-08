package org.example.dzdp_analysis.repository.entity.data;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "etl_task")
public class EtlTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_name", length = 200)
    private String taskName;

    @Column(name = "hdfs_path", length = 500)
    private String hdfsPath;

    @Column(name = "operator", length = 100)
    private String operator;

    // 将Joda Time改为Java Time
    @Column(name = "start_time")
    private java.time.LocalDateTime startTime;

    @Column(name = "end_time")
    private java.time.LocalDateTime endTime;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "execution_duration")
    private Integer executionDuration;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public void setHdfsPath(String hdfsPath) {
        this.hdfsPath = hdfsPath;
    }

    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public void setExecutionDuration(Integer executionDuration) {
        this.executionDuration = executionDuration;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getHdfsPath() {
        return hdfsPath;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    public String getOperator() {
        return operator;
    }

    public String getStatus() {
        return status;
    }

    public Integer getExecutionDuration() {
        return executionDuration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}