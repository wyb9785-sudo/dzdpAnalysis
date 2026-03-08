# 大数据处理一体化平台

### 📌 简介

大数据处理一体化平台 是一个面向餐饮点评数据的端到端大数据处理与分析系统。
它集成了数据上传、ETL 清洗、Hive 数仓、质量评估、商户分析、可视化对比、排行榜及决策辅助等模块，
支持从原始 CSV/Excel 到交互式图表的全流程处理，解决传统数据分析中“上传难、清洗慢、展示乱、决策难”的痛点。

### 🎯 目的

为餐饮企业提供 一站式口碑洞察：评分、评论、菜品、趋势、对标一目了然。
为平台运营方提供 数据治理工具：上传审计、质量报告、任务监控、权限管理。
为技术团队提供 可扩展架构：插件式 ETL、MapReduce引擎支持、自动同步 MySQL、RESTful API。
最终愿景：让数据“上传即可用，查询即决策”。

### 🌟 项目特色
* 全流程数据处理：从数据上传到ETL处理再到质量报告的一站式解决方案

* 多角色支持：管理员、商户、顾客三种角色各具专属功能界面

* 丰富可视化：雷达图、词云、玫瑰图、折线图等多种数据展示方式

* 智能推荐：基于多维度的商户推荐和对比功能

* 响应式设计：适配桌面、平板和移动端多种设备

### ✨ 功能
| 模块 | 主要能力 |
|:--:|:---|
| 🚀 数据接入 | CSV/Excel 拖拽上传 → HDFS → 自动分区 |
| 🔁 智能 ETL | 字段清洗、缺失/异常检测、情感标注、商户归一化 |
| 🏗️ 分层数仓 | ODS → DWD → DWS → ADS（Parquet+Snappy） |
| 📊 质量报告 | 完整性、合法性、合理性 3 维度评分，可下钻 |
| 📈 商户分析 | 雷达图、气泡图、玫瑰图、趋势、热门菜品 |
| 🔍 决策辅助 | 多条件筛选、排行榜、商户横向对比、关键词抽取 |
| 👥 权限管理 | 管理员/商户/顾客三角色，JWT 鉴权，按钮级控制 |
| 🔍 运维监控 | ETL 任务实时状态、失败重试、日志追踪 |
| 🔄 双写同步 | Hive 聚合结果自动同步 MySQL，供前端秒查 |



### 🛠️ 技术栈

* 后端：Spring Boot 3.x、Spring Security、JWT、MySQL 8、MyBatis-JPA

* 大数据：Hadoop 3.3.x、Hive 3.1.x（MapReduce）、HDFS、Parquet、Snappy

* 前端：Vue 3 + Element-Plus + ECharts 5

* 中间件：Axios、Vue-Router、Lombok、Hutool、Logback

* 环境：JDK 17、Maven 3.9、Node 18+、CentOS 7+ / Ubuntu 20+

### 🏗️ 系统架构

前端(Vue3 + Element Plus) → 后端API → Hive数据仓库 → HDFS分布式存储
↑            ↑              ↑
用户交互     数据处理逻辑     数据分析计算

### 📦 安装指南

1. 前置条件

| 组件 | 最低版本 |  备注 |
|:--:|:---|:---|
| Maven | 3.8+ |  必须 |
| MySQL | 8.0 |  必须 |
| Hadoop | 3.3.1 |  伪分布式即可 |
| Hive | 3.1.2 |  需集成 HDFS |
| Node | 18.x |  仅前端构建需要 |
| Vue | 3.x |  必须 |
           

2. 数据库

   mysql -uroot -p < docs/sql/init.sql   # 创建库表+基础数据
   脚本包含：user、upload_log、etl_task、data_quality_report、merchant_* 等 12 张表。

3. 配置 Hadoop & Hive

   将 docs/hive/ 下的 hive-site.xml、core-site.xml、hdfs-site.xml 放到
   $HADOOP_HOME/etc/hadoop/ 与 $HIVE_HOME/conf/ 目录，并保证：
   hdfs dfs -mkdir -p /dianping/warehouse/{ods,dwd,dws,ads}
   hive --service metastore &
   hive --service hiveserver2 &

4. 后端配置
   
   backend/src/main/resources/application.yml
   spring:
   datasource:
   url: jdbc:mysql://127.0.0.1:3306/dzdp_analysis?useSSL=false&serverTimezone=Asia/Shanghai
   username: root
   password: ****
   hadoop:
   hdfs:
   uri: hdfs://127.0.0.1:9000
   user: hdfs
   hive:
   jdbc:
   url: jdbc:hive2://127.0.0.1:10000/default
   username: hive
   password: ""

5. 编译 & 启动

   cd backend
   mvn clean package -DskipTests
   java -jar target/bigdata-platform-1.0.0.jar
   默认端口：8080
   Swagger-API：http://localhost:8080/swagger-ui/index.html

7. 前端构建
   cd frontend
   npm install
   npm run dev
   默认访问：http://localhost:8081

### 📖 使用说明

1. 注册/登录

   角色	测试账号	默认密码	权限
   管理员	admin	Admin@123	上传、ETL、用户管理
   商户	merchant1	Merchant@123	查看自家分析、对比
   顾客	customer1	Customer@123	浏览排行榜、筛选

2. 数据上传

   顶部导航 → 数据上传 → 选择 .csv/.xlsx（≤10 GB）
   输入操作员 → 开始上传 → 上传完毕自动弹出 “立即执行 ETL”
   点击 立即执行 → 跳转到 ETL 监控 页，实时查看进度。
   期望格式（15 列，UTF-8，逗号分隔）
   Review_ID,Marchant,Rating,Score_taste,...Favorite_foods

3. 查看质量报告

   ETL 完成后 → 数据质量报告
   指标：总记录、有效记录、核心字段缺失、评分异常、质量评分（百分制）。
   支持 下钻详情 与 重新生成。
4. 商户分析（商户视角）

   登录商户账号 → 商户分析中心
   雷达图：口味、服务、环境、综合 4 维度评分
   玫瑰图：积极/中性/消极评论占比
   热门菜品：基于评论关键词抽取 TOP10
   趋势：最近 7 天评分走势

5. 决策辅助（管理员/顾客）

   智能筛选：输入商户名称、最低评分、最高人均、最低好评率 → 实时返回候选列表
   排行榜：综合 / 口味 / 性价比 3 类，每日自动更新
   商户对比：多选 2-3 家 → 折线+柱状+表格 三视图对比评分与评论

6. 运维管理
   
   ETL 监控：任务 ID、状态、起止时间、处理记录数、失败原因
   用户管理：启用/禁用、角色切换、密码重置
   集群状态：HDFS 健康、Hive 连接、队列信息一键检测
   
   场景	                                命令
   后端单元测试	                     mvn test
   前端                            Lint	npm run lint
   清空 Hive 表重新建	       POST /api/sync/recreate-tables
   手动触发 ETL	  POST /api/etl/run?hdfsPath=/dianping/xxx.csv&operator=admin
   查询质量报告	        GET /api/quality-reports/task/{taskId}
   查看 Swagger	    http://localhost:8080/swagger-ui/index.html
