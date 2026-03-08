package org.example.dzdp_analysis.service.data;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class HdfsService {

    private static final Logger logger = LoggerFactory.getLogger(HdfsService.class);

    @Value("${hadoop.hdfs.uri}")
    private String hdfsUri;

    @Value("${hadoop.hdfs.user}")
    private String hdfsUser;

    @Value("${hadoop.hdfs.raw-data-path:/dianping/data/raw}")
    private String rawDataPath;

    // 自定义MultipartFile实现
    private static class CustomMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;
        private final String contentType;
        private final String originalFilename;

        public CustomMultipartFile(byte[] content, String name, String contentType, String originalFilename) {
            this.content = content;
            this.name = name;
            this.contentType = contentType;
            this.originalFilename = originalFilename;
        }

        @Override
        public String getName() { return name; }
        @Override
        public String getOriginalFilename() { return originalFilename; }
        @Override
        public String getContentType() { return contentType; }
        @Override
        public boolean isEmpty() { return content.length == 0; }
        @Override
        public long getSize() { return content.length; }
        @Override
        public byte[] getBytes() { return content; }
        @Override
        public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override
        public void transferTo(File dest) throws IOException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
    public String getFileMd5(String hdfsFilePath) throws IOException {
        try (FileSystem fileSystem = getFileSystem();
             FSDataInputStream inputStream = fileSystem.open(new Path(hdfsFilePath))) {

            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] md5Bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : md5Bytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 algorithm not found", e);
        }
    }
    private FileSystem getFileSystem() throws IOException {
        try {
            Configuration configuration = new Configuration();
            configuration.set("fs.defaultFS", hdfsUri);
            configuration.set("dfs.replication", "3");
            configuration.set("io.file.buffer.size", "131072");
            configuration.setBoolean("fs.hdfs.impl.disable.cache", true);

            System.setProperty("HADOOP_USER_NAME", hdfsUser);

            return FileSystem.get(new URI(hdfsUri), configuration, hdfsUser);
        } catch (Exception e) {
            throw new IOException("Failed to get HDFS filesystem: " + e.getMessage(), e);
        }
    }

    // 检查是否包含乱码字符
    private boolean hasGarbledCharacters(String content) {
        return content.contains("��") || content.contains("�") ||
                content.contains("ï¿½") || content.contains("â€");
    }

    // 编码检测和修复方法
    private MultipartFile fixFileEncoding(MultipartFile originalFile, String fileName) throws IOException {
        byte[] fileContent = originalFile.getBytes();

        // 先尝试UTF-8
        String content = new String(fileContent, StandardCharsets.UTF_8);

        // 如果没有乱码，直接返回原文件
        if (!hasGarbledCharacters(content)) {
            logger.info("文件编码正常(UTF-8)，无需修复");
            return originalFile;
        }

        logger.warn("检测到乱码字符，尝试其他编码修复...");

        // 尝试常见的中文编码
        String[] encodings = {"GBK", "GB2312", "BIG5", "ISO-8859-1"};

        for (String encoding : encodings) {
            try {
                String testContent = new String(fileContent, encoding);
                if (!hasGarbledCharacters(testContent)) {
                    logger.info("成功使用 {} 编码修复文件", encoding);
                    content = testContent;
                    break;
                }
            } catch (Exception e) {
                // 继续尝试下一个编码
            }
        }

        // 创建正确编码的UTF-8文件
        return new CustomMultipartFile(
                content.getBytes(StandardCharsets.UTF_8),
                fileName,
                "text/csv",
                fileName
        );
    }

    public String uploadFile(MultipartFile file, String fileName) throws IOException {
        // 先检查文件编码并修复
        MultipartFile correctedFile = fixFileEncoding(file, fileName);
        String hdfsFilePath = rawDataPath + "/" + fileName;

        try (FileSystem fileSystem = getFileSystem();
             InputStream inputStream = correctedFile.getInputStream()) {

            // 确保HDFS目录存在
            Path dataDir = new Path(rawDataPath);
            if (!fileSystem.exists(dataDir)) {
                boolean created = fileSystem.mkdirs(dataDir);
                logger.info("HDFS目录创建: {} - {}", rawDataPath, created ? "成功" : "失败");
            }

            Path path = new Path(hdfsFilePath);

            // 如果文件已存在则删除
            if (fileSystem.exists(path)) {
                boolean deleted = fileSystem.delete(path, true);
                logger.info("删除已存在文件: {} - {}", hdfsFilePath, deleted ? "成功" : "失败");
            }

            // 使用缓冲区上传大文件到HDFS集群
            try (FSDataOutputStream outputStream = fileSystem.create(path)) {
                byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
                int bytesRead;
                long totalBytes = 0;
                long startTime = System.currentTimeMillis();

                while ((bytesRead = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;

                    // 记录上传进度
                    if (totalBytes % (10 * 1024 * 1024) == 0) {
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        double speed = (totalBytes / 1024.0 / 1024.0) / (elapsedTime / 1000.0);
                        logger.info("已上传: {} MB, 速度: {:.2f} MB/s",
                                totalBytes / (1024 * 1024), speed);
                    }
                }

                long elapsedTime = System.currentTimeMillis() - startTime;
                logger.info("文件上传完成: {}, 大小: {} MB, 耗时: {} ms",
                        fileName, totalBytes / (1024 * 1024), elapsedTime);
            }

            // 验证文件是否成功上传
            if (fileSystem.exists(path)) {
                long fileSize = fileSystem.getFileStatus(path).getLen();
                logger.info("HDFS文件验证成功: {}, 大小: {} bytes", hdfsFilePath, fileSize);

                // 验证HDFS上的文件内容
                verifyHdfsFileContent(fileSystem, path);

                return hdfsFilePath;
            } else {
                throw new IOException("文件上传到HDFS失败: " + hdfsFilePath);
            }
        }
    }

    // 验证HDFS文件内容
    private void verifyHdfsFileContent(FileSystem fileSystem, Path path) throws IOException {
        try (InputStream is = fileSystem.open(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String firstLine = reader.readLine();
            if (firstLine != null) {
                logger.info("HDFS文件首行内容: {}", firstLine);
                if (hasGarbledCharacters(firstLine)) {
                    logger.error("HDFS文件仍然包含乱码字符！");
                } else {
                    logger.info("HDFS文件编码验证通过");
                }
            }
        }
    }

    public boolean deleteFile(String hdfsPath) {
        try (FileSystem fileSystem = getFileSystem()) {  // 使用try-with-resources确保FileSystem正确关闭
            Path path = new Path(hdfsPath);
            if (fileSystem.exists(path)) {
                boolean deleted = fileSystem.delete(path, false);
                logger.info("删除HDFS文件: {}, 结果: {}", hdfsPath, deleted);
                return deleted;
            } else {
                logger.warn("HDFS文件不存在: {}", hdfsPath);
                return true; // 文件不存在也算删除成功
            }
        } catch (Exception e) {
            logger.error("删除HDFS文件失败: {}", hdfsPath, e);
            throw new RuntimeException("删除HDFS文件失败: " + e.getMessage());
        }
    }

    public boolean testConnection() {
        try (FileSystem fileSystem = getFileSystem()) {
            boolean connected = fileSystem.exists(new Path("/"));
            logger.info("HDFS集群连接测试: {}", connected ? "成功" : "失败");
            return connected;
        } catch (IOException e) {
            logger.error("HDFS集群连接失败: {}", e.getMessage());
            return false;
        }
    }

    public long getFileSize(String hdfsPath) throws IOException {
        try (FileSystem fileSystem = getFileSystem()) {
            return fileSystem.getFileStatus(new Path(hdfsPath)).getLen();
        }
    }

    public boolean fileExists(String hdfsPath) throws IOException {
        try (FileSystem fileSystem = getFileSystem()) {
            return fileSystem.exists(new Path(hdfsPath));
        }
    }
}