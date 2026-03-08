package org.example.dzdp_analysis.repository.entity.data;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "upload_log")

public class UploadLog {
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "record_count")
    private Integer recordCount;

    @Column(name = "upload_status", length = 20)
    private String uploadStatus;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "hdfs_path", length = 500)
    private String hdfsPath;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

}