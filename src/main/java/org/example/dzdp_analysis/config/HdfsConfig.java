package org.example.dzdp_analysis.config;

import org.apache.hadoop.fs.FileSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;

@Configuration
public class HdfsConfig {

    @Value("${hadoop.hdfs.uri}")
    private String hdfsUri;

    @Value("${hadoop.hdfs.user:root}")  // 修复这里！使用${}从配置读取
    private String hdfsUser;
    @Bean
    public FileSystem fileSystem() throws IOException {
        try {
//            // 强制设置用户身份
            System.setProperty("HADOOP_USER_NAME", hdfsUser);  // 关键行！
            org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
            conf.set("fs.defaultFS", hdfsUri);
            conf.set("dfs.replication", "3");
            conf.set("dfs.client.use.datanode.hostname", "true");
            conf.setBoolean("fs.hdfs.impl.disable.cache", true);
            return FileSystem.get(new URI(hdfsUri), conf, hdfsUser);
        } catch (Exception e) {
            throw new IOException("Failed to initialize HDFS FileSystem", e);
        }
    }
}