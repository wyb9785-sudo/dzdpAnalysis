package org.example.dzdp_analysis.service.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.Random;

@Service
public class UserSyncService {

    private static final Logger logger = LoggerFactory.getLogger(UserSyncService.class);

    @Autowired
    private Connection hiveConnection;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${hive.database:dianping}")
    private String hiveDatabase;

    private static final Random random = new Random();

    // 名称验证正则表达式
    private static final Pattern VALID_NAME_PATTERN =
            Pattern.compile("^[\\u4e00-\\u9fa5a-zA-Z0-9\\-\\s（）()&]+$");

    @Transactional
    public void syncAllUsers(String uploadDate) {
        logger.info("开始同步所有用户数据...");

        try {
            // 同步商户用户
            syncMerchantUsers(uploadDate);

            // 同步评论者用户
            syncReviewerUsers(uploadDate);

            logger.info("所有用户数据同步完成");
        } catch (Exception e) {
            logger.error("同步用户数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("同步用户数据失败: " + e.getMessage());
        }
    }

    @Transactional
    public void syncMerchantUsers(String uploadDate) {
        logger.info("开始同步商户用户数据...");

        try {
            // 从MySQL获取有效的商户名称
            Set<String> validMerchants = extractValidMerchants(uploadDate);
            logger.info("提取到商户列表: {}", validMerchants);

            // 获取现有的商户用户
            Set<String> existingMerchants = getExistingUsers("MERCHANT", "merchant_name");
            logger.info("现有商户用户: {}", existingMerchants);

            // 找出需要新增的商户
            Set<String> newMerchants = new HashSet<>(validMerchants);
            newMerchants.removeAll(existingMerchants);
            logger.info("需要创建的新商户: {}", newMerchants);

            // 创建新商户用户
            if (!newMerchants.isEmpty()) {
                createMerchantUsers(newMerchants);
                logger.info("成功创建 {} 个新商户用户", newMerchants.size());

                // 验证数据是否真正插入
                verifyUserCreation(newMerchants);
            } else {
                logger.info("没有需要创建的新商户用户");
            }

        } catch (Exception e) {
            logger.error("同步商户用户数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("同步商户用户数据失败: " + e.getMessage());
        }
    }
    // 添加验证方法
    private void verifyUserCreation(Set<String> merchantNames) {
        for (String merchantName : merchantNames) {
            String checkSql = "SELECT COUNT(*) FROM user WHERE merchant_name = ? AND role = 'MERCHANT'";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, merchantName);
            if (count == 0) {
                logger.error("商户用户创建验证失败: {}", merchantName);
            } else {
                logger.info("商户用户创建验证成功: {}", merchantName);
            }
        }
    }
    // 添加专门的商户用户创建方法
    // 修改createMerchantUsers方法，使用商户名作为用户名
    private void createMerchantUsers(Set<String> merchantNames) {
        LocalDateTime now = LocalDateTime.now();

        String sql = "INSERT INTO user (" +
                "username, password, merchant_name, role, status, create_time, update_time, " +
                "email, phone, last_login_time" +
                ") VALUES (?, ?, ?, 'MERCHANT', 'ACTIVE', ?, ?, ?, ?, ?)";

        for (String merchantName : merchantNames) {
            try {
                // 使用商户名作为用户名（进行适当清理）
                String username = cleanUsername(merchantName);
                String email = generateRandomAlphanumeric(10) + "@example.com";
                String phone = generateRandomPhone();
                String encodedPassword = passwordEncoder.encode("Merchant@123");

                jdbcTemplate.update(sql,
                        username,
                        encodedPassword,
                        merchantName,  // 保存原始商户名称
                        now,
                        now,
                        email,
                        phone,
                        now
                );

                logger.info("创建商户用户: 商户名={}, 用户名={}, 邮箱={}", merchantName, username, email);

            } catch (Exception e) {
                logger.error("创建商户用户失败: {}, 错误: {}", merchantName, e.getMessage());
            }
        }
    }
    // 添加用户名清理方法
    private String cleanUsername(String merchantName) {
        // 移除特殊字符，只保留字母、数字、下划线
        String cleaned = merchantName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_]", "_");
        // 限制长度
        if (cleaned.length() > 20) {
            cleaned = cleaned.substring(0, 20);
        }
        return cleaned;
    }

    // 生成商户用户名
    private String generateMerchantUsername(String merchantName) {
        // 简单处理：使用随机字符串+商户名hash
        String randomPart = generateRandomAlphanumeric(6);
        int hash = Math.abs(merchantName.hashCode()) % 10000;
        return "mcht_" + randomPart + "_" + hash;
    }
    @Transactional
    public void syncReviewerUsers(String uploadDate) {
        logger.info("开始同步评论者用户数据...");

        try {
            // 1. 从Hive获取所有唯一的有效评论者名称
            Set<String> validReviewers = extractValidReviewers(uploadDate);

            // 2. 获取现有的评论者用户
            Set<String> existingReviewers = getExistingUsers("CUSTOMER", "username");

            // 3. 找出需要新增的评论者
            Set<String> newReviewers = new HashSet<>(validReviewers);
            newReviewers.removeAll(existingReviewers);

            // 4. 创建新评论者用户
            if (!newReviewers.isEmpty()) {
                createReviewerUsers(newReviewers);
                logger.info("成功创建 {} 个新评论者用户", newReviewers.size());
            } else {
                logger.info("没有需要创建的新评论者用户");
            }

        } catch (Exception e) {
            logger.error("同步评论者用户数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("同步评论者用户数据失败: " + e.getMessage());
        }
    }
    // 修改商户名称验证逻辑
    private boolean isValidMerchantNameForUser(String merchantName) {
        if (merchantName == null || merchantName.trim().isEmpty()) {
            return false;
        }
        String trimmedName = merchantName.trim();

        // 真正的乱码检测：包含�字符
        if (trimmedName.contains("�")) {
            logger.warn("商户名称包含乱码字符�: {}", trimmedName);
            return false;
        }

        // 排除常见无效名称
        String[] invalidPatterns = {
                "未知商户", "test", "null", "示例", "空", "测试"
        };

        for (String pattern : invalidPatterns) {
            if (trimmedName.toLowerCase().contains(pattern.toLowerCase())) {
                logger.warn("商户名称包含无效模式: {}", trimmedName);
                return false;
            }
        }

        return true;
    }
    private Set<String> extractValidMerchants(String uploadDate) throws Exception {
        Set<String> validMerchants = new HashSet<>();
    // 从merchant_analysis表查询（不是temp表）
        String sql = "SELECT DISTINCT merchant_name FROM merchant_analysis WHERE update_date = ?";

        jdbcTemplate.query(sql, new Object[]{uploadDate}, rs -> {
            String merchantName = rs.getString("merchant_name");
            if (merchantName != null && isValidMerchantNameForUser(merchantName)) {
                validMerchants.add(merchantName.trim());
            }
        });

        logger.info("从MySQL merchant_analysis表提取到 {} 个有效商户", validMerchants.size());
        return validMerchants;
    }
    // 添加评论者名称验证
    private boolean isValidReviewerName(String reviewerName) {
        if (reviewerName == null || reviewerName.trim().isEmpty()) {
            return false;
        }

        String trimmedName = reviewerName.trim();

        // 宽松的验证：只要不是明显无效的就允许
        if (trimmedName.length() < 2 || trimmedName.length() > 50) {
            return false;
        }

        // 排除明显的测试数据
        if (trimmedName.equalsIgnoreCase("匿名用户") ||
                trimmedName.equalsIgnoreCase("未知用户") ||
                trimmedName.equalsIgnoreCase("test") ||
                trimmedName.equalsIgnoreCase("null")) {
            return false;
        }

        return true;
    }
    private Set<String> extractValidReviewers(String uploadDate) throws Exception {
        Set<String> validReviewers = new HashSet<>();

        String sql = String.format(
                "SELECT DISTINCT reviewer FROM %s.%s WHERE upload_date='%s' AND reviewer IS NOT NULL",
                hiveDatabase, "ods_raw_reviews", uploadDate
        );

        try (Statement stmt = hiveConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String reviewerName = rs.getString("reviewer");
                if (isValidReviewerName(reviewerName)) {
                    validReviewers.add(reviewerName.trim());
                }
            }
        }

        logger.info("从Hive提取到 {} 个有效评论者", validReviewers.size());
        return validReviewers;
    }

    private Set<String> getExistingUsers(String role, String identifierField) {
        Set<String> existingUsers = new HashSet<>();

        String sql = String.format("SELECT %s FROM user WHERE role = '%s'", identifierField, role);

        jdbcTemplate.query(sql, rs -> {
            existingUsers.add(rs.getString(identifierField));
        });

        return existingUsers;
    }


    private void createReviewerUsers(Set<String> reviewerNames) {
        LocalDateTime now = LocalDateTime.now();
        String encodedPassword = passwordEncoder.encode("Customer@123");

        String sql =
                "INSERT INTO user (" +
                        "username, password, merchant_name, role, status, create_time, update_time, " +
                        "email, phone, last_login_time" +
                        ") VALUES (?, ?, NULL, 'CUSTOMER', 'ACTIVE', ?, ?, ?, ?, ?)";

        for (String reviewerName : reviewerNames) {
            try {
                // 评论者用户直接使用评论者名称作为用户名
                String username = generateUniqueUsername(reviewerName);
                String email = generateRandomAlphanumeric(10) + "@example.com";
                String phone = generateRandomPhone();

                jdbcTemplate.update(sql,
                        username,
                        encodedPassword,
                        now,
                        now,
                        email,
                        phone,
                        now
                );

                logger.info("创建评论者用户: {} (邮箱: {}, 电话: {})",
                        username, email, phone);

            } catch (Exception e) {
                logger.error("创建评论者用户失败: {}, 错误: {}", reviewerName, e.getMessage());
            }
        }
    }

    private String generateUniqueUsername(String name) {
        String baseUsername = generateUsername(name);
        String username = baseUsername;
        int suffix = 1;

        // 确保用户名唯一
        while (isUsernameExists(username)) {
            username = baseUsername + suffix;
            suffix++;
        }

        return username;
    }

    private String generateUsername(String name) {
        // 简单处理：去除特殊字符，用下划线替换空格
        return name.trim()
                .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_")
                .replaceAll("_+", "_")
                .toLowerCase();
    }
    // 添加生成随机字母数字的方法
    private String generateRandomAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    private String generateRandomEmail(String username) {
        // 生成随机邮箱
        // 生成随机字母数字前缀，避免使用中文
        String randomPrefix = generateRandomAlphanumeric(8);
        String[] domains = {"gmail.com", "outlook.com", "qq.com", "163.com", "sina.com"};
        String domain = domains[random.nextInt(domains.length)];
        return randomPrefix + "@" + domain;
    }

    private String generateRandomPhone() {
        // 生成随机手机号
        String[] prefixes = {"130", "131", "132", "133", "134", "135", "136", "137", "138", "139",
                "150", "151", "152", "153", "155", "156", "157", "158", "159",
                "180", "181", "182", "183", "184", "185", "186", "187", "188", "189"};
        String prefix = prefixes[random.nextInt(prefixes.length)];
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            suffix.append(random.nextInt(10));
        }
        return prefix + suffix.toString();
    }

    private boolean isUsernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM user WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    // 批量同步方法
    @Transactional
    public void batchSyncAllUsers(List<String> uploadDates) {
        for (String uploadDate : uploadDates) {
            try {
                syncAllUsers(uploadDate);
            } catch (Exception e) {
                logger.error("同步日期 {} 的用户数据失败: {}", uploadDate, e.getMessage());
            }
        }
    }

    @Transactional
    public void batchSyncMerchantUsers(List<String> uploadDates) {
        for (String uploadDate : uploadDates) {
            try {
                syncMerchantUsers(uploadDate);
            } catch (Exception e) {
                logger.error("同步日期 {} 的商户用户失败: {}", uploadDate, e.getMessage());
            }
        }
    }

    @Transactional
    public void batchSyncReviewerUsers(List<String> uploadDates) {
        for (String uploadDate : uploadDates) {
            try {
                syncReviewerUsers(uploadDate);
            } catch (Exception e) {
                logger.error("同步日期 {} 的评论者用户失败: {}", uploadDate, e.getMessage());
            }
        }
    }
}