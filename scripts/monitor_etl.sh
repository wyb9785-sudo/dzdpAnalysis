#!/bin/bash
# monitor_etl.sh - 监控ETL任务

LOG_FILE="/var/log/dianping/etl_monitor.log"

echo "$(date) - 开始ETL任务监控" >> $LOG_FILE

# 检查运行中的任务
RUNNING_TASKS=$(curl -s "http://localhost:8080/api/etl/status" | grep -c "RUNNING")

if [ "$RUNNING_TASKS" -gt 0 ]; then
    echo "$(date) - 当前有 $RUNNING_TASKS 个ETL任务运行中" >> $LOG_FILE
fi

# 检查HDFS空间
HDFS_SPACE=$(hdfs dfsadmin -report | grep "Used" | awk '{print $3}')
echo "$(date) - HDFS已使用空间: $HDFS_SPACE" >> $LOG_FILE

echo "$(date) - ETL监控完成" >> $LOG_FILE