#!/bin/bash
# setup_cluster.sh - 集群环境准备

echo "=== 开始集群环境准备 ==="

# 1. 启动Hadoop集群
echo "启动HDFS和YARN..."
start-dfs.sh
start-yarn.sh

# 2. 启动Hive服务
echo "启动Hive服务..."
hive --service metastore &
hive --service hiveserver2 &

# 3. 创建HDFS目录
echo "创建HDFS目录结构..."
hdfs dfs -mkdir -p /dianping/data/raw
hdfs dfs -mkdir -p /user/hive/warehouse/dianping/ods
hdfs dfs -mkdir -p /user/hive/warehouse/dianping/dwd
hdfs dfs -mkdir -p /user/hive/warehouse/dianping/dws
hdfs dfs -mkdir -p /user/hive/warehouse/dianping/ads

# 设置权限
hdfs dfs -chmod -R 755 /dianping
hdfs dfs -chmod -R 755 /user/hive/warehouse/dianping
hdfs dfs -chown -R hadoop:hadoop /dianping
hdfs dfs -chown -R hadoop:hadoop /user/hive/warehouse/dianping

echo "=== 集群环境准备完成 ==="