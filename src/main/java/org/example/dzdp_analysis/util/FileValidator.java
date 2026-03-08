package org.example.dzdp_analysis.util;

import org.springframework.web.multipart.MultipartFile;

public class FileValidator {

    public static void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 检查文件扩展名
        if (!fileName.matches(".*\\.(csv|CSV|xlsx|XLSX)$")) {
            throw new IllegalArgumentException("只支持CSV和Excel文件格式");
        }

        // 检查文件大小 (这里不限制大小，因为配置中已经设置为不限制)
    }
}