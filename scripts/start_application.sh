#!/bin/bash
# start_application.sh - 启动Spring Boot应用

echo "=== 启动DZDP分析应用 ==="

# 编译项目
echo "编译项目..."
mvn clean package -DskipTests

# 启动应用
echo "启动应用..."
java -jar target/dzdp_analysis-0.0.1-SNAPSHOT.jar \
    --spring.profiles.active=prod \
    --server.port=8080

echo "=== 应用启动完成 ==="