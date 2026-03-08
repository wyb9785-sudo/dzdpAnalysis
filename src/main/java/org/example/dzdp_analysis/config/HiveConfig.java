package org.example.dzdp_analysis.config;

import jakarta.annotation.PostConstruct;
import org.apache.hive.jdbc.HiveDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.apache.hadoop.hbase.Version.url;

@Configuration
public class HiveConfig {

    @Value("${hive.jdbc.url:jdbc:hive2://192.168.137.129:10000/default}")
    private String hiveJdbcUrl;

    @Value("${hive.jdbc.username:hive}")
    private String hiveUser;

    @Value("${hive.jdbc.password:}")
    private String hivePassword;

    @Value("${hive.connection.pool.size:5}")
    private int poolSize;

    @Value("${hive.execution.engine:tez}")
    private String executionEngine;

    // 主数据源配置（MySQL）
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return dataSourceProperties().initializeDataSourceBuilder().build();
    }

    // Hive数据源配置（不作为JPA管理）
    @Bean(name = "hiveDataSource")
    public DataSource hiveDataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(HiveDriver.class);
        dataSource.setUrl(hiveJdbcUrl);
        dataSource.setUsername(hiveUser);
        dataSource.setPassword(hivePassword);

        // 设置连接属性
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("tez.queue.name", "default");
        connectionProperties.setProperty("hive.execution.engine", executionEngine);
        connectionProperties.setProperty("hive.server2.session.check.interval", "300000");
        connectionProperties.setProperty("hive.server2.idle.session.timeout", "1800000");
        connectionProperties.setProperty("ssl", "false");
        connectionProperties.setProperty("transportMode", "http");
        connectionProperties.setProperty("httpPath", "cliservice");

        // 添加Hive特定的配置，避免元数据读取
        connectionProperties.setProperty("hive.support.concurrency", "false");
        connectionProperties.setProperty("hive.txn.manager", "org.apache.hadoop.hive.ql.lockmgr.DummyTxnManager");

        dataSource.setConnectionProperties(connectionProperties);

        return dataSource;
    }

    @Bean
    public Connection hiveConnection() throws SQLException {
        System.setProperty("org.apache.hive.jdbc.log", "console");
        try {
            Class.forName("org.apache.hive.jdbc.HiveDriver");

            // 添加连接参数，明确禁用SSL和设置其他必要参数
            String connectionUrl = hiveJdbcUrl + "?" +
                    "ssl=false&" +
                    "transportMode=http&" +
                    "httpPath=cliservice&" +
                    "socketTimeout=60&" +  // 增加超时设置
                    "retries=3&" +         // 增加重试机制
                    "connectTimeout=30";

            return DriverManager.getConnection(connectionUrl, hiveUser, hivePassword);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Hive JDBC Driver not found", e);
        }
    }
    @PostConstruct
    public void printProperties() {
        System.out.println("Hive URL: " + hiveJdbcUrl);
        System.out.println("Hive Username: " + hiveUser);
        System.out.println("Hive Password: " + hivePassword);
        System.out.println("Hive Execution Engine: " + executionEngine);
    }


}